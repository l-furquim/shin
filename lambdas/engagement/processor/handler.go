package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"strconv"
	"time"

	"engagement/internal/model"
	"engagement/internal/validation"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/dynamodb"
	"github.com/aws/aws-sdk-go-v2/service/dynamodb/types"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
)

const (
	maxEventAge           = 5 * time.Minute
	shortVideoThreshold   = 30
	shortVideoMinRatio    = 0.75
	longVideoMinWatchTime = 30
)

type sessionState struct {
	exists              bool
	watchTimeSeconds    int
	lastPositionSeconds int
	lastUpdatedAt       time.Time
	viewCounted         bool
}

type ProcessorHandler struct {
	dynamoClient *dynamodb.Client
	sqsClient    *sqs.Client
	tableName    string
	viewQueueURL string
	sessionTTL   time.Duration
}

func (h *ProcessorHandler) Handle(ctx context.Context, event events.SQSEvent) error {
	for _, record := range event.Records {
		if err := h.processRecord(ctx, record); err != nil {
			return err
		}
	}
	return nil
}

func (h *ProcessorHandler) processRecord(ctx context.Context, record events.SQSMessage) error {
	var progress model.ProgressRecord
	if err := json.Unmarshal([]byte(record.Body), &progress); err != nil {
		log.Printf("skipping malformed record %s: %v", record.MessageId, err)
		return nil
	}

	if err := validation.ValidateProgressRecord(&progress); err != nil {
		log.Printf("skipping invalid record %s: %v", record.MessageId, err)
		return nil
	}

	if err := validation.ValidateEventFreshness(progress.EventTimestamp, maxEventAge); err != nil {
		log.Printf("skipping stale record %s: %v", record.MessageId, err)
		return nil
	}

	session, err := h.getSessionState(ctx, progress.SessionID)
	if err != nil {
		return fmt.Errorf("failed to read session %s: %w", progress.SessionID, err)
	}

	if session.exists {
		if err := validation.ValidateWatchTimeConsistency(progress.WatchTimeSeconds, progress.EventTimestamp, session.lastUpdatedAt); err != nil {
			log.Printf("skipping inconsistent record %s: %v", record.MessageId, err)
			return nil
		}
	}

	newTotalWatchTime, err := h.updateSession(ctx, &progress)
	if err != nil {
		return fmt.Errorf("failed to update session %s: %w", progress.SessionID, err)
	}

	if !session.viewCounted && isViewQualified(newTotalWatchTime, progress.VideoDurationSeconds) {
		counted, err := h.markViewCounted(ctx, progress.SessionID)
		if err != nil {
			return fmt.Errorf("failed to mark view for session %s: %w", progress.SessionID, err)
		}
		if counted {
			return h.publishViewEvent(ctx, &progress, newTotalWatchTime)
		}
	}

	return nil
}

func (h *ProcessorHandler) getSessionState(ctx context.Context, sessionID string) (*sessionState, error) {
	result, err := h.dynamoClient.GetItem(ctx, &dynamodb.GetItemInput{
		TableName: aws.String(h.tableName),
		Key: map[string]types.AttributeValue{
			"sessionId": &types.AttributeValueMemberS{Value: sessionID},
		},
	})
	if err != nil {
		return nil, err
	}

	if len(result.Item) == 0 {
		return &sessionState{exists: false}, nil
	}

	state := &sessionState{exists: true}

	if v, ok := result.Item["watchTimeSeconds"].(*types.AttributeValueMemberN); ok {
		state.watchTimeSeconds, _ = strconv.Atoi(v.Value)
	}
	if v, ok := result.Item["lastPositionSeconds"].(*types.AttributeValueMemberN); ok {
		state.lastPositionSeconds, _ = strconv.Atoi(v.Value)
	}
	if v, ok := result.Item["lastUpdatedAt"].(*types.AttributeValueMemberS); ok {
		state.lastUpdatedAt, _ = time.Parse(time.RFC3339, v.Value)
	}
	if v, ok := result.Item["viewCounted"].(*types.AttributeValueMemberBOOL); ok {
		state.viewCounted = v.Value
	}

	return state, nil
}

func (h *ProcessorHandler) updateSession(ctx context.Context, progress *model.ProgressRecord) (int, error) {
	expiresAt := time.Now().Add(h.sessionTTL).Unix()

	result, err := h.dynamoClient.UpdateItem(ctx, &dynamodb.UpdateItemInput{
		TableName: aws.String(h.tableName),
		Key: map[string]types.AttributeValue{
			"sessionId": &types.AttributeValueMemberS{Value: progress.SessionID},
		},
		UpdateExpression: aws.String("ADD watchTimeSeconds :delta SET videoId = :vid, userId = :uid, lastPositionSeconds = :pos, lastUpdatedAt = :ts, expiresAt = :exp"),
		ExpressionAttributeValues: map[string]types.AttributeValue{
			":delta": &types.AttributeValueMemberN{Value: fmt.Sprintf("%d", progress.WatchTimeSeconds)},
			":vid":   &types.AttributeValueMemberS{Value: progress.VideoID},
			":uid":   &types.AttributeValueMemberS{Value: progress.UserID},
			":pos":   &types.AttributeValueMemberN{Value: fmt.Sprintf("%d", progress.CurrentPositionSeconds)},
			":ts":    &types.AttributeValueMemberS{Value: time.Now().UTC().Format(time.RFC3339)},
			":exp":   &types.AttributeValueMemberN{Value: fmt.Sprintf("%d", expiresAt)},
		},
		ReturnValues: types.ReturnValueAllNew,
	})
	if err != nil {
		return 0, err
	}

	var total int
	if v, ok := result.Attributes["watchTimeSeconds"].(*types.AttributeValueMemberN); ok {
		total, _ = strconv.Atoi(v.Value)
	}

	return total, nil
}

func (h *ProcessorHandler) markViewCounted(ctx context.Context, sessionID string) (bool, error) {
	_, err := h.dynamoClient.UpdateItem(ctx, &dynamodb.UpdateItemInput{
		TableName: aws.String(h.tableName),
		Key: map[string]types.AttributeValue{
			"sessionId": &types.AttributeValueMemberS{Value: sessionID},
		},
		UpdateExpression:    aws.String("SET viewCounted = :true"),
		ConditionExpression: aws.String("attribute_not_exists(viewCounted)"),
		ExpressionAttributeValues: map[string]types.AttributeValue{
			":true": &types.AttributeValueMemberBOOL{Value: true},
		},
	})
	if err != nil {
		var condErr *types.ConditionalCheckFailedException
		if errors.As(err, &condErr) {
			return false, nil
		}
		return false, err
	}
	return true, nil
}

func isViewQualified(totalWatchTime, videoDurationSeconds int) bool {
	if videoDurationSeconds >= shortVideoThreshold {
		return totalWatchTime >= longVideoMinWatchTime
	}
	threshold := int(float64(videoDurationSeconds) * shortVideoMinRatio)
	return totalWatchTime >= threshold
}

func (h *ProcessorHandler) publishViewEvent(ctx context.Context, progress *model.ProgressRecord, totalWatchTime int) error {
	viewEvent := model.ViewEvent{
		SessionID:              progress.SessionID,
		VideoID:                progress.VideoID,
		UserID:                 progress.UserID,
		WatchTimeSeconds:       totalWatchTime,
		CurrentPositionSeconds: progress.CurrentPositionSeconds,
		VideoDurationSeconds:   progress.VideoDurationSeconds,
		EventTimestamp:         time.Now().UTC(),
	}

	body, err := json.Marshal(viewEvent)
	if err != nil {
		return fmt.Errorf("failed to marshal view event for session %s: %w", progress.SessionID, err)
	}

	_, err = h.sqsClient.SendMessage(ctx, &sqs.SendMessageInput{
		QueueUrl:    aws.String(h.viewQueueURL),
		MessageBody: aws.String(string(body)),
	})
	if err != nil {
		return fmt.Errorf("failed to publish view event for session %s: %w", progress.SessionID, err)
	}

	return nil
}

package sqs

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net/url"
	"path"
	"strings"
	"thumbnail-processor/internal/config"
	"thumbnail-processor/internal/model"
	"thumbnail-processor/internal/service"
)

type ThumbnailWorker struct {
	service *service.ThumbnailService
}

var errSkipMessage = errors.New("skip thumbnail message")

type s3NotificationEnvelope struct {
	Message string `json:"Message"`
}

type s3NotificationEvent struct {
	Records []s3Record `json:"Records"`
}

type s3Record struct {
	EventSource string `json:"eventSource"`
	EventName   string `json:"eventName"`
	S3          struct {
		Bucket struct {
			Name string `json:"name"`
		} `json:"bucket"`
		Object struct {
			Key string `json:"key"`
		} `json:"object"`
	} `json:"s3"`
}

func NewThumbnailWorker(service *service.ThumbnailService) *ThumbnailWorker {
	return &ThumbnailWorker{
		service: service,
	}
}

func (w *ThumbnailWorker) Handle(ctx context.Context, msg []byte, cfg *config.Config) error {
	job, err := w.resolveJob(ctx, msg, cfg)
	if err != nil {
		if errors.Is(err, errSkipMessage) {
			log.Printf("Skipping thumbnail message: %v", err)
			return nil
		}
		return err
	}

	if err := w.service.ProcessJob(ctx, job, cfg); err != nil {
		return err
	}

	log.Printf("Thumbnail job completed successfully for video: %s", job.VideoID)
	return nil
}

func (w *ThumbnailWorker) resolveJob(ctx context.Context, msg []byte, cfg *config.Config) (*model.ThumbnailJob, error) {
	record, err := parseS3Record(msg)
	if err != nil {
		return nil, err
	}

	decodedKey, decodeErr := url.QueryUnescape(strings.ReplaceAll(record.S3.Object.Key, "+", "%20"))
	if decodeErr != nil {
		decodedKey = record.S3.Object.Key
	}

	if strings.TrimSpace(decodedKey) == "" {
		return nil, fmt.Errorf("s3 object key is required")
	}

	bucketName := cfg.RawBucketName
	if strings.TrimSpace(record.S3.Bucket.Name) != "" {
		bucketName = record.S3.Bucket.Name
	}

	isCustom := isCustomThumbnailUpload(decodedKey)
	metadata, err := w.service.Storage.GetObjectMetadata(ctx, decodedKey, bucketName)
	if err != nil {
		log.Printf("Could not fetch object metadata for s3://%s/%s: %v", bucketName, decodedKey, err)
		metadata = map[string]string{}
	}

	videoID := strings.TrimSpace(metadata["videoid"])
	if videoID == "" {
		videoID = extractVideoIDFromKey(decodedKey, isCustom)
	}
	if videoID == "" {
		return nil, fmt.Errorf("missing required video id for key: %s", decodedKey)
	}

	if strings.EqualFold(strings.TrimSpace(metadata["thumbnailkind"]), "generated") {
		return nil, fmt.Errorf("%w: generated thumbnail artifact key=%s", errSkipMessage, decodedKey)
	}

	return &model.ThumbnailJob{
		VideoID:      videoID,
		SourceKey:    decodedKey,
		SourceBucket: bucketName,
		IsCustom:     isCustom,
	}, nil
}

func parseS3Record(msg []byte) (*s3Record, error) {
	var envelope s3NotificationEnvelope
	if err := json.Unmarshal(msg, &envelope); err == nil && strings.TrimSpace(envelope.Message) != "" {
		return parseS3Record([]byte(envelope.Message))
	}

	var eventPayload s3NotificationEvent
	if err := json.Unmarshal(msg, &eventPayload); err != nil {
		return nil, err
	}

	if len(eventPayload.Records) == 0 {
		return nil, fmt.Errorf("s3 notification has no records")
	}

	record := eventPayload.Records[0]
	if !strings.HasPrefix(record.EventSource, "aws:s3") || !strings.HasPrefix(record.EventName, "ObjectCreated:") {
		return nil, fmt.Errorf("message is not an s3 object created event")
	}

	return &record, nil
}

func isCustomThumbnailUpload(objectKey string) bool {
	normalized := strings.ToLower(strings.TrimSpace(objectKey))
	return strings.HasPrefix(normalized, "thumbnails/") && strings.Contains(normalized, "/custom/custom.")
}

func extractVideoIDFromKey(objectKey string, isCustom bool) string {
	cleanKey := strings.TrimPrefix(path.Clean("/"+objectKey), "/")
	parts := strings.Split(cleanKey, "/")

	if isCustom {
		if len(parts) >= 2 && parts[0] == "thumbnails" {
			return strings.TrimSpace(parts[1])
		}
		return ""
	}

	if len(parts) >= 1 {
		return strings.TrimSpace(parts[0])
	}

	return ""
}

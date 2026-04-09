package sqs

import (
	"context"
	"encoding/json"
	"errors"
	"log"
	"net/url"
	"strings"
	"thumbnail-processor/internal/config"
	"thumbnail-processor/internal/event"
	"thumbnail-processor/internal/model"
	"thumbnail-processor/internal/service"
)

type ThumbnailWorker struct {
	service *service.ThumbnailService
}

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
		return err
	}

	if err := w.service.ProcessJob(ctx, job, cfg); err != nil {
		return err
	}

	log.Printf("Thumbnail job completed successfully for video: %s", job.VideoId)
	return nil
}

func (w *ThumbnailWorker) resolveJob(ctx context.Context, msg []byte, cfg *config.Config) (*model.ThumbnailJob, error) {
	legacyJob, legacyErr := parseLegacyJob(msg)
	if legacyErr == nil {
		return legacyJob, nil
	}

	record, err := parseS3Record(msg)
	if err != nil {
		return nil, errors.Join(legacyErr, err)
	}

	decodedKey, decodeErr := url.QueryUnescape(strings.ReplaceAll(record.S3.Object.Key, "+", "%20"))
	if decodeErr != nil {
		decodedKey = record.S3.Object.Key
	}

	if strings.TrimSpace(decodedKey) == "" {
		return nil, errors.New("s3 object key is required")
	}

	bucketName := cfg.RawBucketName
	if strings.TrimSpace(record.S3.Bucket.Name) != "" {
		bucketName = record.S3.Bucket.Name
	}

	metadata, err := w.service.Storage.GetObjectMetadata(ctx, decodedKey, bucketName)
	if err != nil {
		return nil, err
	}

	videoID := strings.TrimSpace(metadata["videoid"])
	if videoID == "" {
		return nil, errors.New("missing required object metadata: videoid")
	}

	return &model.ThumbnailJob{
		VideoId: videoID,
		S3Key:   decodedKey,
	}, nil
}

func parseLegacyJob(msg []byte) (*model.ThumbnailJob, error) {
	var jobEvent event.ThumbnailJobEvent
	if err := json.Unmarshal(msg, &jobEvent); err != nil {
		return nil, err
	}

	job := jobEvent.ToModel()
	if strings.TrimSpace(job.VideoId) == "" || strings.TrimSpace(job.S3Key) == "" {
		return nil, errors.New("legacy payload missing videoId or s3Key")
	}

	return job, nil
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
		return nil, errors.New("s3 notification has no records")
	}

	record := eventPayload.Records[0]
	if !strings.HasPrefix(record.EventSource, "aws:s3") || !strings.HasPrefix(record.EventName, "ObjectCreated:") {
		return nil, errors.New("message is not an s3 object created event")
	}

	return &record, nil
}

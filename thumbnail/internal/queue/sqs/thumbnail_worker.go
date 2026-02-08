package sqs

import (
	"context"
	"encoding/json"
	"log"
	"thumbnail-service/internal/config"
	"thumbnail-service/internal/event"
	"thumbnail-service/internal/service"
)

type ThumbnailWorker struct {
	service *service.ThumbnailService
}

func NewThumbnailWorker(service *service.ThumbnailService) *ThumbnailWorker {
	return &ThumbnailWorker{
		service: service,
	}
}

func (w *ThumbnailWorker) Handle(ctx context.Context, msg []byte, cfg *config.Config) error {
	var jobEvent event.ThumbnailJobEvent
	if err := json.Unmarshal(msg, &jobEvent); err != nil {
		return err
	}

	model := jobEvent.ToModel()

	if err := w.service.ProcessJob(ctx, model, cfg); err != nil {
		return err
	}

	log.Printf("Thumbnail job completed successfully for video: %s", model.VideoId)
	return nil
}


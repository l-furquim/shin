package sqs

import (
	"context"
	"encoding/json"
	"log"
	"os"
	"transcoding-service/internal/config"
	"transcoding-service/internal/event"
	"transcoding-service/internal/service"
)

type TranscodingWorker struct {
	service *service.TranscodingService
}

func NewTranscodingWorker(service *service.TranscodingService) *TranscodingWorker {
	return &TranscodingWorker{
		service: service,
	}
}

func (w *TranscodingWorker) Handle(ctx context.Context, msg []byte, cfg *config.Config) error {
	var event event.TranscodingEvent

	if err := json.Unmarshal(msg, &event); err != nil {
		return err
	}

	model := event.ToModel()

	processedPath, err := w.service.ProcessJob(ctx, model, cfg)
	if err != nil {
		return err
	}

	defer func() {
		log.Printf("Cleaning up job directory: %s", processedPath)
		if err := os.RemoveAll(processedPath); err != nil {
			log.Printf("Failed to clean up job directory %s: %v", processedPath, err)
		}
	}()

	err = w.service.SendChunksToStorage(ctx, processedPath, model)
	if err != nil {
		return err
	}

	log.Printf("Transcoding job completed successfully for video: %s", model.VideoId)
	return nil
}


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
	var transcodingEvent event.TranscodingEvent

	if err := json.Unmarshal(msg, &transcodingEvent); err != nil {
		return err
	}

	job := transcodingEvent.ToModel()
	var processingErr error

	defer func() {
		if processingErr != nil {
			w.service.SendFailureNotification(ctx, job.VideoId, processingErr.Error())
		}
	}()

	processedPath, duration, fileInfo, err := w.service.ProcessJob(ctx, job, cfg)
	if err != nil {
		processingErr = err
		return err
	}

	defer func() {
		log.Printf("Cleaning up job directory: %s", processedPath)
		if err := os.RemoveAll(processedPath); err != nil {
			log.Printf("Failed to clean up job directory %s: %v", processedPath, err)
		}
	}()

	err = w.service.SendChunksToStorage(ctx, processedPath, job, duration, fileInfo, cfg)
	if err != nil {
		processingErr = err
		return err
	}

	log.Printf("Transcoding job completed successfully for video: %s", job.VideoId)
	return nil
}

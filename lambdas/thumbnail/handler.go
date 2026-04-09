package main

import (
	"context"
	"log"

	"github.com/aws/aws-lambda-go/events"

	internalconfig "thumbnail-processor/internal/config"
	sqsqueue "thumbnail-processor/internal/queue/sqs"
)

type Handler struct {
	cfg    *internalconfig.Config
	worker *sqsqueue.ThumbnailWorker
}

func (h *Handler) Handle(ctx context.Context, event events.SQSEvent) error {
	for _, record := range event.Records {
		if err := h.processRecord(ctx, record); err != nil {
			return err
		}
	}
	return nil
}

func (h *Handler) processRecord(ctx context.Context, record events.SQSMessage) error {
	log.Printf("Processing SQS message: %s", record.MessageId)
	return h.worker.Handle(ctx, []byte(record.Body), h.cfg)
}

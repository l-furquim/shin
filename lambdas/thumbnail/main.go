package main

import (
	"context"
	"log"

	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/aws/aws-sdk-go-v2/service/sqs"

	internalconfig "thumbnail-processor/internal/config"
	sqsqueue "thumbnail-processor/internal/queue/sqs"
	"thumbnail-processor/internal/service"
	s3storage "thumbnail-processor/internal/storage/s3"
)

func main() {
	ctx := context.Background()

	awsCfg, err := config.LoadDefaultConfig(ctx)
	if err != nil {
		log.Fatalf("failed to load AWS config: %v", err)
	}

	cfg := internalconfig.LoadConfig()

	s3Client := s3.NewFromConfig(awsCfg)
	storage := s3storage.NewStorageService(s3Client)

	sqsClient := sqs.NewFromConfig(awsCfg)
	completionProducer := sqsqueue.NewCompletionProducer(sqsClient, cfg.ThumbnailFinishedQueueURL)

	ts := &service.ThumbnailService{
		Storage:            storage,
		CompletionProducer: completionProducer,
	}

	worker := sqsqueue.NewThumbnailWorker(ts)

	handler := &Handler{cfg: cfg, worker: worker}

	lambda.Start(handler.Handle)
}

package main

import (
	"context"
	"log"
	"transcoding-service/internal/aws"
	"transcoding-service/internal/config"
	"transcoding-service/internal/queue/sqs"
	"transcoding-service/internal/service"
	s3storage "transcoding-service/internal/storage/s3"
)

func main() {
	ctx := context.Background()

	cfg := config.LoadConfig(config.DEV)
	log.Println(cfg.JobRequestQueueURL)

	awsEndpoint := "http://localhost:4566"

	awsCfg, err := aws.LoadConfig(ctx)
	if err != nil {
		log.Fatalf("failed to load AWS config: %v", err)
	}

	// S3
	s3Client := s3storage.New(awsCfg, awsEndpoint)
	storage := s3storage.NewStorageService(s3Client)

	// Service
	ts := &service.TranscodingService{
		Storage: storage,
	}

	// SQS
	sqsClient := sqs.New(awsCfg, awsEndpoint)
	consumer := sqs.NewConsumer(sqsClient, cfg.JobRequestQueueURL)
	worker := sqs.NewTranscodingWorker(ts)

	consumer.Start(ctx, func(msg []byte) error {
		return worker.Handle(ctx, msg, cfg)
	})
}

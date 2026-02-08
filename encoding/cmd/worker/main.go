package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"
	"transcoding-service/internal/aws"
	"transcoding-service/internal/config"
	"transcoding-service/internal/queue/sqs"
	"transcoding-service/internal/service"
	s3storage "transcoding-service/internal/storage/s3"
)

func main() {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	cfg := config.LoadConfig(config.DEV)
	log.Printf("Starting transcoding worker with queue: %s", cfg.JobRequestQueueURL)
	log.Printf("AWS Region: %s", cfg.Region)

	awsCfg, err := aws.LoadConfig(ctx, cfg.Region)
	if err != nil {
		log.Fatalf("failed to load AWS config: %v", err)
	}

	s3Client := s3storage.New(awsCfg)
	storage := s3storage.NewStorageService(s3Client)

	ts := &service.TranscodingService{
		Storage: storage,
	}

	sqsClient := sqs.New(awsCfg)
	consumer := sqs.NewConsumer(sqsClient, cfg.JobRequestQueueURL)
	worker := sqs.NewTranscodingWorker(ts)

	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM)

	go func() {
		<-sigChan
		log.Println("Received shutdown signal")
		cancel()
	}()

	log.Println("Worker started, waiting for messages...")
	if err := consumer.Start(ctx, func(msg []byte) error {
		return worker.Handle(ctx, msg, cfg)
	}); err != nil && err != context.Canceled {
		log.Fatalf("Consumer error: %v", err)
	}

	log.Println("Worker stopped")
}

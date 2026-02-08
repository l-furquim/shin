package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"
	"thumbnail-service/internal/aws"
	"thumbnail-service/internal/config"
	"thumbnail-service/internal/queue/sns"
	"thumbnail-service/internal/queue/sqs"
	"thumbnail-service/internal/service"
	s3storage "thumbnail-service/internal/storage/s3"
)

func main() {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	cfg := config.LoadConfig(config.DEV)
	log.Printf("Starting thumbnail worker with queue: %s", cfg.JobRequestQueueURL)
	log.Printf("AWS Region: %s", cfg.Region)

	awsCfg, err := aws.LoadConfig(ctx, cfg.Region)
	if err != nil {
		log.Fatalf("failed to load AWS config: %v", err)
	}

	s3Client := s3storage.New(awsCfg)
	storage := s3storage.NewStorageService(s3Client)

	var publisher *sns.Publisher
	if cfg.ThumbnailGeneratedTopicARN != "" {
		snsClient := sns.New(awsCfg)
		publisher = sns.NewPublisher(snsClient, cfg.ThumbnailGeneratedTopicARN)
		log.Printf("SNS publisher initialized for topic: %s", cfg.ThumbnailGeneratedTopicARN)
	}

	ts := &service.ThumbnailService{
		Storage:   storage,
		Publisher: publisher,
	}

	sqsClient := sqs.New(awsCfg)
	consumer := sqs.NewConsumer(sqsClient, cfg.JobRequestQueueURL)
	worker := sqs.NewThumbnailWorker(ts)

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

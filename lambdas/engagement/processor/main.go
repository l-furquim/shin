package main

import (
	"context"
	"log"
	"os"
	"time"

	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/dynamodb"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
)

func main() {
	cfg, err := config.LoadDefaultConfig(context.Background())
	if err != nil {
		log.Fatalf("failed to load AWS config: %v", err)
	}

	tableName := os.Getenv("TABLE_NAME")
	if tableName == "" {
		log.Fatal("TABLE_NAME environment variable is required")
	}

	viewQueueURL := os.Getenv("VIEW_EVENTS_QUEUE_URL")
	if viewQueueURL == "" {
		log.Fatal("VIEW_EVENTS_QUEUE_URL environment variable is required")
	}

	handler := &ProcessorHandler{
		dynamoClient: dynamodb.NewFromConfig(cfg),
		sqsClient:    sqs.NewFromConfig(cfg),
		tableName:    tableName,
		viewQueueURL: viewQueueURL,
		sessionTTL:   24 * time.Hour,
	}

	lambda.Start(handler.Handle)
}

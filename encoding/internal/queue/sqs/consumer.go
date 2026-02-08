package sqs

import (
	"context"
	"log"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
)

type Consumer struct {
	client   *sqs.Client
	queueURL string
}

func NewConsumer(client *sqs.Client, queueURL string) *Consumer {
	return &Consumer{
		client:   client,
		queueURL: queueURL,
	}
}

func (c *Consumer) Start(ctx context.Context, handler func([]byte) error) error {
	for {
		select {
		case <-ctx.Done():
			log.Println("Consumer shutting down...")
			return ctx.Err()
		default:
			if err := c.processMessages(ctx, handler); err != nil {
				if ctx.Err() != nil {
					return ctx.Err()
				}
				log.Printf("Error processing messages: %v", err)
			}
		}
	}
}

func (c *Consumer) processMessages(ctx context.Context, handler func([]byte) error) error {
	resp, err := c.client.ReceiveMessage(ctx, &sqs.ReceiveMessageInput{
		QueueUrl:            aws.String(c.queueURL),
		MaxNumberOfMessages: 10,
		WaitTimeSeconds:     20,
	})
	if err != nil {
		return err
	}

	for _, msg := range resp.Messages {
		if msg.Body == nil {
			continue
		}

		if err := handler([]byte(*msg.Body)); err != nil {
			log.Printf("Error handling message: %v", err)
			continue
		}

		if _, err := c.client.DeleteMessage(ctx, &sqs.DeleteMessageInput{
			QueueUrl:      aws.String(c.queueURL),
			ReceiptHandle: msg.ReceiptHandle,
		}); err != nil {
			log.Printf("Error deleting message: %v", err)
		}
	}

	return nil
}

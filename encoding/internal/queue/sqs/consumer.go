package sqs

import (
	"context"
	"log"

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

func (c *Consumer) Start(ctx context.Context, handler func([]byte) error) {
	for {
		resp, err := c.client.ReceiveMessage(ctx, &sqs.ReceiveMessageInput{
			QueueUrl:            &c.queueURL,
			MaxNumberOfMessages: 10,
			WaitTimeSeconds:     20, // Long polling
		})

		if err != nil {
			log.Println("Error while receiving the message: ", err)
			continue
		}

		for _, msg := range resp.Messages {
			err := handler([]byte(*msg.Body))

			if err == nil {
				c.client.DeleteMessage(ctx, &sqs.DeleteMessageInput{
					QueueUrl:      &c.queueURL,
					ReceiptHandle: msg.ReceiptHandle,
				})
			}
		}

	}
}

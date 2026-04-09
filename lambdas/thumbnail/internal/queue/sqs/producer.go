package sqs

import (
	"context"
	"encoding/json"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
)

type CompletionProducer struct {
	client   *sqs.Client
	queueURL string
}

func NewCompletionProducer(client *sqs.Client, queueURL string) *CompletionProducer {
	return &CompletionProducer{
		client:   client,
		queueURL: queueURL,
	}
}

func (p *CompletionProducer) Send(ctx context.Context, payload any) error {
	body, err := json.Marshal(payload)
	if err != nil {
		return err
	}

	message := string(body)
	_, err = p.client.SendMessage(ctx, &sqs.SendMessageInput{
		QueueUrl:    aws.String(p.queueURL),
		MessageBody: aws.String(message),
	})

	return err
}

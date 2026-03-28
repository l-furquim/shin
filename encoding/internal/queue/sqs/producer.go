package sqs

import (
	"context"
	"encoding/json"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
)

type TranscodingCompletedProducer struct {
	c        *sqs.Client
	queueURL string
}

func NewTranscodingCompletedProducer(c *sqs.Client, queueURL string) *TranscodingCompletedProducer {
	return &TranscodingCompletedProducer{
		c:        c,
		queueURL: queueURL,
	}
}

func (p *TranscodingCompletedProducer) Send(ctx context.Context, payload any) error {
	body, err := json.Marshal(payload)
	if err != nil {
		return err
	}

	message := string(body)
	_, err = p.c.SendMessage(ctx, &sqs.SendMessageInput{
		QueueUrl:    aws.String(p.queueURL),
		MessageBody: aws.String(message),
	})

	return err
}

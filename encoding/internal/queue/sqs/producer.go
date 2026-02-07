package sqs

import "github.com/aws/aws-sdk-go-v2/service/sqs"

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

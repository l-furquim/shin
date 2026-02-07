package sqs

import (
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
)

func New(cfg *aws.Config, endpoint string) *sqs.Client {
	return sqs.NewFromConfig(*cfg, func(o *sqs.Options) {
		o.BaseEndpoint = aws.String(endpoint)
	})
}

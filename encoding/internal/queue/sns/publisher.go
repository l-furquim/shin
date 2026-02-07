package sns

import (
	"context"
	"encoding/json"

	"github.com/aws/aws-sdk-go-v2/service/sns"
)

type Publisher struct {
	client *sns.Client
	topic  string
}

func NewPublisher(client *sns.Client, topicArn string) *Publisher {
	return &Publisher{
		client: client,
		topic:  topicArn,
	}
}

func (p *Publisher) Publish(ctx context.Context, event any) error {
	body, err := json.Marshal(event)
	if err != nil {
		return err
	}

	s := string(body)

	_, err = p.client.Publish(ctx, &sns.PublishInput{
		TopicArn: &p.topic,
		Message:  &s,
	})

	return err
}

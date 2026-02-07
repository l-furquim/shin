package sns

import (
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/sns"
)

func New(cfg *aws.Config, endpoint string) *sns.Client {
	return sns.NewFromConfig(*cfg, func(o *sns.Options) {
		o.BaseEndpoint = aws.String(endpoint)
	})
}

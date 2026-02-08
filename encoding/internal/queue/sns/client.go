package sns

import (
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/sns"
)

func New(cfg *aws.Config) *sns.Client {
	return sns.NewFromConfig(*cfg)
}

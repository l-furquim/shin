package config

import (
	"log"
	"os"
)

type Env int

const (
	PROD Env = iota
	DEV
)

type Config struct {
	Port                string
	JobRequestQueueURL  string
	JobFinishedTopicARN string
	RawBucketName       string
	ProcessedBucketName string
	Env                 Env
}

func LoadConfig(env Env) *Config {
	cfg := &Config{
		Port:                getEnv("PORT", "8080"),
		JobRequestQueueURL:  getEnv("JOB_REQUEST_QUEUE", "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/transcoding-job"),
		JobFinishedTopicARN: getEnv("JOB_FINISHED_ARN", ""),
		RawBucketName:       getEnv("RAW_BUCKET_NAME", "baboo-raw-bucket"),
		ProcessedBucketName: getEnv("PROCESSED_BUCKET_NAME", "baboo-processed-bucket"),
		Env:                 env,
	}

	return cfg
}

func getEnv(key, defaultValue string) string {
	env := os.Getenv(key)

	log.Printf("Loaded env: %s\n", env)

	if value := env; value != "" {
		return value
	}
	return defaultValue
}

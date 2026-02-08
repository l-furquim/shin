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
	Port                       string
	JobRequestQueueURL         string
	ThumbnailGeneratedTopicARN string
	RawBucketName              string
	ThumbnailBucketName        string
	Env                        Env
	Region                     string
}

func LoadConfig(env Env) *Config {
	cfg := &Config{
		Port:                       getEnv("PORT", "8080"),
		JobRequestQueueURL:         mustGetEnv("THUMBNAIL_JOB_QUEUE_URL"),
		ThumbnailGeneratedTopicARN: getEnv("THUMBNAIL_GENERATED_TOPIC_ARN", ""),
		RawBucketName:              mustGetEnv("RAW_BUCKET_NAME"),
		ThumbnailBucketName:        mustGetEnv("THUMBNAIL_BUCKET_NAME"),
		Region:                     getEnv("AWS_REGION", "us-east-1"),
		Env:                        env,
	}

	return cfg
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		log.Printf("Loaded env %s: %s", key, value)
		return value
	}
	log.Printf("Using default for %s: %s", key, defaultValue)
	return defaultValue
}

func mustGetEnv(key string) string {
	value := os.Getenv(key)
	if value == "" {
		log.Fatalf("Required environment variable %s is not set", key)
	}
	log.Printf("Loaded env %s: %s", key, value)
	return value
}

package config

import (
	"log"
	"os"
)

type Config struct {
	ThumbnailFinishedQueueURL string
	RawBucketName             string
	ThumbnailBucketName       string
	FFmpegPath                string
}

func LoadConfig() *Config {
	return &Config{
		ThumbnailFinishedQueueURL: mustGetEnv("THUMBNAIL_FINISHED_EVENTS_QUEUE_URL"),
		RawBucketName:             mustGetEnv("RAW_BUCKET_NAME"),
		ThumbnailBucketName:       mustGetEnv("THUMBNAIL_BUCKET_NAME"),
		FFmpegPath:                getEnv("FFMPEG_PATH", "/opt/bin/ffmpeg"),
	}
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

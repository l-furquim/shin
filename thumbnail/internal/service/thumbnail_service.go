package service

import (
	"context"
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"time"
	"thumbnail-service/internal/config"
	"thumbnail-service/internal/model"
	"thumbnail-service/internal/queue/sns"
)

type ThumbnailService struct {
	Storage   StorageService
	Publisher *sns.Publisher
}

type ThumbnailGeneratedEvent struct {
	VideoId   string    `json:"videoId"`
	S3Key     string    `json:"s3Key"`
	Timestamp time.Time `json:"timestamp"`
}

func (s *ThumbnailService) ProcessJob(ctx context.Context, job *model.ThumbnailJob, cfg *config.Config) error {
	log.Printf("Received thumbnail job, videoId: %s, s3Key: %s", job.VideoId, job.S3Key)

	videoPath, err := s.Storage.GetRawVideo(ctx, job.S3Key, cfg.RawBucketName)
	if err != nil {
		return fmt.Errorf("failed to get raw video: %w", err)
	}
	defer os.Remove(videoPath)

	thumbnailPath, err := generateThumbnail(videoPath, job.VideoId)
	if err != nil {
		return fmt.Errorf("failed to generate thumbnail: %w", err)
	}
	defer os.Remove(thumbnailPath)

	thumbnailKey := fmt.Sprintf("%s/thumbnail.jpg", job.VideoId)

	data, err := os.ReadFile(thumbnailPath)
	if err != nil {
		return fmt.Errorf("failed to read thumbnail: %w", err)
	}

	if err := s.Storage.UploadThumbnail(ctx, &data, thumbnailKey, cfg.ThumbnailBucketName); err != nil {
		return fmt.Errorf("failed to upload thumbnail: %w", err)
	}

	log.Printf("Thumbnail uploaded successfully: %s", thumbnailKey)

	if s.Publisher != nil {
		notification := ThumbnailGeneratedEvent{
			VideoId:   job.VideoId,
			S3Key:     thumbnailKey,
			Timestamp: time.Now(),
		}

		if err := s.Publisher.Publish(ctx, notification); err != nil {
			log.Printf("Failed to publish thumbnail-generated notification: %v", err)
		} else {
			log.Printf("Published thumbnail-generated notification for video %s", job.VideoId)
		}
	}

	return nil
}

func generateThumbnail(videoPath, videoId string) (string, error) {
	outputPath := filepath.Join(os.TempDir(), fmt.Sprintf("%s-thumbnail.jpg", videoId))

	args := []string{
		"-i", videoPath,
		"-ss", "00:00:01",
		"-vframes", "1",
		"-vf", "scale=1280:720",
		"-q:v", "2",
		"-y",
		outputPath,
	}

	cmd := exec.Command("ffmpeg", args...)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	log.Printf("Generating thumbnail: %s", outputPath)

	if err := cmd.Run(); err != nil {
		return "", fmt.Errorf("ffmpeg thumbnail generation failed: %w", err)
	}

	log.Printf("Thumbnail generated successfully: %s", outputPath)
	return outputPath, nil
}

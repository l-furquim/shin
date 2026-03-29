package service

import (
	"context"
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"thumbnail-service/internal/config"
	"thumbnail-service/internal/model"
	"time"
)

type QualityConfig struct {
	Profile string
	Width   int
	Height  int
}

var thumbnailResolutions [5]QualityConfig = [5]QualityConfig{
	{Profile: "default", Width: 120, Height: 90},
	{Profile: "medium", Width: 320, Height: 180},
	{Profile: "high", Width: 480, Height: 360},
	{Profile: "standard", Width: 640, Height: 480},
	{Profile: "maxres", Width: 1280, Height: 720},
}

type ThumbnailService struct {
	Storage            StorageService
	CompletionProducer CompletionSender
}

type CompletionSender interface {
	Send(ctx context.Context, payload any) error
}

type ThumbnailGeneratedEvent struct {
	EventType string    `json:"eventType"`
	VideoId   string    `json:"videoId"`
	S3Key     string    `json:"s3Key"`
	Timestamp time.Time `json:"timestamp"`
}

type GeneratedThumbnail struct {
	Profile   string
	LocalPath string
}

func (s *ThumbnailService) ProcessJob(ctx context.Context, job *model.ThumbnailJob, cfg *config.Config) error {
	log.Printf("Received thumbnail job, videoId: %s, s3Key: %s", job.VideoId, job.S3Key)

	videoPath, err := s.Storage.GetRawVideo(ctx, job.S3Key, cfg.RawBucketName)
	if err != nil {
		return fmt.Errorf("failed to get raw video: %w", err)
	}
	defer os.Remove(videoPath)

	thumbnails, tempDir, err := generateThumbnails(videoPath, job.VideoId)
	if err != nil {
		if tempDir != "" {
			_ = os.RemoveAll(tempDir)
		}
		return fmt.Errorf("failed to generate thumbnails: %w", err)
	}
	defer os.RemoveAll(tempDir)

	thumbnailBaseKey := fmt.Sprintf("thumbnails/%s", job.VideoId)

	for _, thumbnail := range thumbnails {
		data, readErr := os.ReadFile(thumbnail.LocalPath)
		if readErr != nil {
			return fmt.Errorf("failed to read generated thumbnail %s: %w", thumbnail.Profile, readErr)
		}

		uploadKey := fmt.Sprintf("%s/%s.jpg", thumbnailBaseKey, thumbnail.Profile)
		if uploadErr := s.Storage.UploadThumbnail(ctx, &data, uploadKey, cfg.ThumbnailBucketName); uploadErr != nil {
			return fmt.Errorf("failed to upload thumbnail profile %s: %w", thumbnail.Profile, uploadErr)
		}
	}
	log.Printf("Thumbnails uploaded successfully: %s", thumbnailBaseKey)

	if s.CompletionProducer != nil {
		notification := ThumbnailGeneratedEvent{
			EventType: "thumbnailGenerated",
			VideoId:   job.VideoId,
			S3Key:     thumbnailBaseKey,
			Timestamp: time.Now(),
		}

		if err := s.CompletionProducer.Send(ctx, notification); err != nil {
			log.Printf("Failed to publish thumbnail-generated notification: %v", err)
		} else {
			log.Printf("Published thumbnail-generated notification for video %s", job.VideoId)
		}
	}

	return nil
}

func generateThumbnails(videoPath, videoId string) ([]GeneratedThumbnail, string, error) {
	tempDir := filepath.Join(os.TempDir(), "thumbnails", videoId)
	if err := os.MkdirAll(tempDir, 0755); err != nil {
		return nil, "", fmt.Errorf("failed to create thumbnail temp directory %s: %w", tempDir, err)
	}

	thumbnails := make([]GeneratedThumbnail, 0, len(thumbnailResolutions))
	for _, profile := range thumbnailResolutions {
		outputPath := filepath.Join(tempDir, fmt.Sprintf("%s.jpg", profile.Profile))
		args := buildThumbnailCommand(videoPath, profile, outputPath)
		if err := runFFmpeg(args); err != nil {
			return nil, tempDir, fmt.Errorf("ffmpeg failed for profile %s: %w", profile.Profile, err)
		}
		thumbnails = append(thumbnails, GeneratedThumbnail{Profile: profile.Profile, LocalPath: outputPath})
	}

	return thumbnails, tempDir, nil
}

func buildThumbnailCommand(input string, profile QualityConfig, outputPath string) []string {
	scale := fmt.Sprintf("scale=%dx%d", profile.Width, profile.Height)
	log.Printf("Built ffmpeg command for profile: %s", profile.Profile)

	return []string{
		"-i", input,
		"-ss", "00:00:01",
		"-vframes", "1",
		"-vf", scale,
		"-q:v", "2",
		"-y",
		outputPath,
	}
}

func runFFmpeg(args []string) error {
	cmd := exec.Command("ffmpeg", args...)

	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	if err := cmd.Run(); err != nil {
		return fmt.Errorf("ffmpeg command failed (%s): %w", strings.Join(args, " "), err)
	}

	log.Printf("FFmpeg completed successfully")
	return nil
}

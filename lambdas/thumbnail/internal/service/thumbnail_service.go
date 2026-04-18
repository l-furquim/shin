package service

import (
	"context"
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"thumbnail-processor/internal/config"
	"thumbnail-processor/internal/model"
	"time"
)

type ThumbnailProfile struct {
	Name   string
	Width  int
	Height int
}

var thumbnailProfiles = []ThumbnailProfile{
	{Name: "default", Width: 120, Height: 90},
	{Name: "medium", Width: 320, Height: 180},
	{Name: "high", Width: 480, Height: 360},
	{Name: "standard", Width: 640, Height: 480},
	{Name: "maxres", Width: 1280, Height: 720},
}

type ThumbnailService struct {
	Storage            StorageService
	CompletionProducer CompletionSender
}

type CompletionSender interface {
	Send(ctx context.Context, payload any) error
}

type ThumbnailGeneratedEvent struct {
	VideoID   string    `json:"videoId"`
	S3Key     string    `json:"s3Key"`
	Timestamp time.Time `json:"timestamp"`
}

type GeneratedThumbnail struct {
	Profile   string
	LocalPath string
}

func (s *ThumbnailService) ProcessJob(ctx context.Context, job *model.ThumbnailJob, cfg *config.Config) error {
	log.Printf(
		"Received thumbnail job videoId=%s sourceBucket=%s sourceKey=%s isCustom=%t",
		job.VideoID,
		job.SourceBucket,
		job.SourceKey,
		job.IsCustom,
	)

	sourcePath, err := s.Storage.DownloadObject(ctx, job.SourceKey, job.SourceBucket)
	if err != nil {
		return fmt.Errorf("failed to download source object: %w", err)
	}
	defer func() {
		_ = os.Remove(sourcePath)
	}()

	thumbnails, baseKey, tempDir, err := generateThumbnails(sourcePath, job, cfg.FFmpegPath)
	if err != nil {
		if tempDir != "" {
			_ = os.RemoveAll(tempDir)
		}
		return fmt.Errorf("failed to generate thumbnails: %w", err)
	}
	defer func() {
		_ = os.RemoveAll(tempDir)
	}()

	for _, thumbnail := range thumbnails {
		data, readErr := os.ReadFile(thumbnail.LocalPath)
		if readErr != nil {
			return fmt.Errorf("failed to read generated thumbnail %s: %w", thumbnail.Profile, readErr)
		}

		uploadKey := fmt.Sprintf("%s/%s", baseKey, thumbnail.Profile)
		contentType := "image/jpeg"
		if strings.HasSuffix(thumbnail.Profile, ".png") {
			contentType = "image/png"
		}

		metadata := map[string]string{
			"videoid":         job.VideoID,
			"thumbnailkind":   "generated",
			"thumbnailsource": map[bool]string{true: "custom", false: "video"}[job.IsCustom],
		}

		if uploadErr := s.Storage.UploadThumbnail(ctx, data, uploadKey, cfg.ThumbnailBucketName, contentType, metadata); uploadErr != nil {
			return fmt.Errorf("failed to upload thumbnail profile %s: %w", thumbnail.Profile, uploadErr)
		}
	}
	log.Printf("Thumbnails uploaded successfully under prefix: %s", baseKey)

	if s.CompletionProducer != nil {
		notification := ThumbnailGeneratedEvent{
			VideoID:   job.VideoID,
			S3Key:     baseKey,
			Timestamp: time.Now(),
		}

		if err := s.CompletionProducer.Send(ctx, notification); err != nil {
			log.Printf("Failed to publish thumbnail-generated notification: %v", err)
		} else {
			log.Printf("Published thumbnail-generated notification for video %s", job.VideoID)
		}
	}

	return nil
}

func generateThumbnails(sourcePath string, job *model.ThumbnailJob, ffmpegPath string) ([]GeneratedThumbnail, string, string, error) {
	tempDir, err := os.MkdirTemp("", fmt.Sprintf("thumbnail-%s-", job.VideoID))
	if err != nil {
		return nil, "", "", fmt.Errorf("failed to create temp directory: %w", err)
	}

	if job.IsCustom {
		thumbnails, customBaseKey, genErr := generateCustomThumbnails(sourcePath, job.VideoID, ffmpegPath, tempDir)
		if genErr != nil {
			return nil, "", tempDir, genErr
		}
		return thumbnails, customBaseKey, tempDir, nil
	}

	thumbnails, baseKey, genErr := generateVideoThumbnails(sourcePath, job.VideoID, ffmpegPath, tempDir)
	if genErr != nil {
		return nil, "", tempDir, genErr
	}

	return thumbnails, baseKey, tempDir, nil
}

func generateVideoThumbnails(videoPath, videoID, ffmpegPath, tempDir string) ([]GeneratedThumbnail, string, error) {
	thumbnails := make([]GeneratedThumbnail, 0, len(thumbnailProfiles))
	for _, profile := range thumbnailProfiles {
		outputPath := filepath.Join(tempDir, fmt.Sprintf("%s.jpg", profile.Name))
		args := buildVideoThumbnailCommand(videoPath, profile, outputPath)
		if err := runFFmpeg(ffmpegPath, args); err != nil {
			return nil, "", fmt.Errorf("ffmpeg failed for profile %s: %w", profile.Name, err)
		}
		thumbnails = append(thumbnails, GeneratedThumbnail{Profile: profile.Name + ".jpg", LocalPath: outputPath})
	}

	return thumbnails, fmt.Sprintf("thumbnails/%s", videoID), nil
}

func generateCustomThumbnails(imagePath, videoID, ffmpegPath, tempDir string) ([]GeneratedThumbnail, string, error) {
	thumbnails := make([]GeneratedThumbnail, 0, len(thumbnailProfiles))

	for _, profile := range thumbnailProfiles {
		outputPath := filepath.Join(tempDir, fmt.Sprintf("%s.jpg", profile.Name))
		args := buildCustomThumbnailCommand(imagePath, profile, outputPath)
		if err := runFFmpeg(ffmpegPath, args); err != nil {
			return nil, "", fmt.Errorf("ffmpeg failed for custom profile %s: %w", profile.Name, err)
		}
		thumbnails = append(thumbnails, GeneratedThumbnail{Profile: profile.Name + ".jpg", LocalPath: outputPath})
	}

	rootCustomPath := filepath.Join(tempDir, "custom.png")
	if err := runFFmpeg(ffmpegPath, buildCustomRootCommand(imagePath, rootCustomPath)); err != nil {
		return nil, "", fmt.Errorf("ffmpeg failed while normalizing custom root image: %w", err)
	}
	thumbnails = append(thumbnails, GeneratedThumbnail{Profile: "custom.png", LocalPath: rootCustomPath})

	return thumbnails, fmt.Sprintf("thumbnails/%s/custom", videoID), nil
}

func buildVideoThumbnailCommand(input string, profile ThumbnailProfile, outputPath string) []string {
	scale := fmt.Sprintf("scale=%dx%d", profile.Width, profile.Height)
	log.Printf("Built video thumbnail ffmpeg command for profile: %s", profile.Name)

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

func buildCustomThumbnailCommand(input string, profile ThumbnailProfile, outputPath string) []string {
	filter := fmt.Sprintf(
		"scale=%d:%d:force_original_aspect_ratio=decrease,pad=%d:%d:(ow-iw)/2:(oh-ih)/2:color=black",
		profile.Width,
		profile.Height,
		profile.Width,
		profile.Height,
	)

	return []string{
		"-i", input,
		"-frames:v", "1",
		"-vf", filter,
		"-q:v", "2",
		"-y",
		outputPath,
	}
}

func buildCustomRootCommand(input, outputPath string) []string {
	return []string{
		"-i", input,
		"-frames:v", "1",
		"-y",
		outputPath,
	}
}

func runFFmpeg(ffmpegPath string, args []string) error {
	cmd := exec.Command(ffmpegPath, args...)

	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	if err := cmd.Run(); err != nil {
		return fmt.Errorf("ffmpeg command failed (%s): %w", strings.Join(args, " "), err)
	}

	log.Printf("FFmpeg completed successfully")
	return nil
}

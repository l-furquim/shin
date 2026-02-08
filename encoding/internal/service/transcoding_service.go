package service

import (
	"context"
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
	"time"
	"transcoding-service/internal/config"
	"transcoding-service/internal/event"
	"transcoding-service/internal/model"
	"transcoding-service/internal/queue/sns"
)

type TranscodingService struct {
	Storage             StorageService
	ChunkPublisher      *sns.Publisher
	CompletionPublisher *sns.Publisher
}

type ResolutionConfig struct {
	Height  int
	Bitrate string
	Profile string
}

var resolutionMap = map[string]ResolutionConfig{
	"1080p": {Height: 1080, Bitrate: "5000k", Profile: "high"},
	"720p":  {Height: 720, Bitrate: "3000k", Profile: "main"},
	"480p":  {Height: 480, Bitrate: "1500k", Profile: "main"},
	"360p":  {Height: 360, Bitrate: "800k", Profile: "baseline"},
}

func (s *TranscodingService) ProcessJob(ctx context.Context, event *model.TranscodingJob, cfg *config.Config) (string, float64, error) {

	log.Printf("Received a process job, videoId: %s, key: %s, userId: %s", event.VideoId, event.S3Key, event.UserId)

	filePath, err := s.Storage.GetRawVideo(ctx, event.S3Key, event.FileName, cfg.RawBucketName)
	if err != nil {
		return "", 0, err
	}

	duration, err := getVideoDuration(filePath)
	if err != nil {
		log.Printf("Warning: failed to extract video duration: %v", err)
		duration = 0
	}
	log.Printf("Video duration: %.2f seconds", duration)

	jobDir := filepath.Join(os.TempDir(), "transcoding-jobs", event.VideoId)

	log.Printf("Creating job directory: %s", jobDir)
	err = os.MkdirAll(jobDir, 0755)
	if err != nil {
		return "", 0, fmt.Errorf("failed to create job directory: %w", err)
	}

	for _, res := range event.Resolutions {
		resolutionDir := filepath.Join(jobDir, strings.Trim(res, "[]"))
		log.Printf("Creating resolution directory: %s", resolutionDir)
		err = os.MkdirAll(resolutionDir, 0755)
		if err != nil {
			return "", 0, fmt.Errorf("failed to create resolution directory %s: %w", resolutionDir, err)
		}
	}

	for _, args := range BuildDashCommands(filePath, jobDir, event.Resolutions) {
		if err := runFFmpeg(args); err != nil {
			return "", 0, fmt.Errorf("ffmpeg failed: %w", err)
		}
	}

	log.Printf("Transcoding job processed successfully for file: %s\n", filePath)

	return jobDir, duration, nil
}

func BuildDashCommands(
	input string,
	baseOutputDir string,
	resolutions []string,
) [][]string {

	var commands [][]string

	for _, res := range resolutions {
		res = strings.Trim(res, "[]")

		cfg, ok := resolutionMap[res]
		if !ok {
			log.Printf("Resolução ignorada: %s", res)
			continue
		}

		outputDir := filepath.Join(baseOutputDir, res)

		args := []string{
			"-y",
			"-i", input,
			"-vf", fmt.Sprintf("scale=-2:%d", cfg.Height),

			"-c:v", "libx264",
			"-b:v", cfg.Bitrate,
			"-profile:v", cfg.Profile,

			"-g", "48",
			"-keyint_min", "48",
			"-sc_threshold", "0",

			"-map", "0:v",
			"-map", "0:a?",
			"-c:a", "aac",
			"-b:a", "128k",

			"-f", "dash",
			"-seg_duration", "6",
			"-use_timeline", "1",
			"-use_template", "1",
			"-adaptation_sets", "id=0,streams=v id=1,streams=a",

			"-init_seg_name", "init-$RepresentationID$.m4s",
			"-media_seg_name", "chunk-$RepresentationID$-$Number$.m4s",

			filepath.Join(outputDir, "manifest.mpd"),
		}

		log.Printf("Built FFmpeg command for resolution %s: output=%s", res, outputDir)
		commands = append(commands, args)
	}

	return commands
}

func runFFmpeg(args []string) error {
	cmd := exec.Command("ffmpeg", args...)

	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	if err := cmd.Run(); err != nil {
		return fmt.Errorf("ffmpeg command failed: %w", err)
	}

	log.Printf("FFmpeg completed successfully")
	return nil
}

func getVideoDuration(videoPath string) (float64, error) {
	cmd := exec.Command("ffprobe",
		"-v", "error",
		"-show_entries", "format=duration",
		"-of", "default=noprint_wrappers=1:nokey=1",
		videoPath,
	)

	output, err := cmd.Output()
	if err != nil {
		return 0, fmt.Errorf("ffprobe failed: %w", err)
	}

	durationStr := strings.TrimSpace(string(output))
	duration, err := strconv.ParseFloat(durationStr, 64)
	if err != nil {
		return 0, fmt.Errorf("failed to parse duration: %w", err)
	}

	return duration, nil
}

func (s *TranscodingService) SendChunksToStorage(ctx context.Context, path string, event *model.TranscodingJob, duration float64, cfg *config.Config) error {
	totalFiles := countTotalFiles(path, event.Resolutions)
	if totalFiles == 0 {
		return fmt.Errorf("no files found to upload")
	}

	var (
		filesUploaded      int
		uploadMutex        sync.Mutex
		milestones         = []int{20, 50, 70}
		notifiedMilestones = make(map[int]bool)
	)

	for _, resolution := range event.Resolutions {
		resolution = strings.Trim(resolution, "[]")
		resolutionPath := filepath.Join(path, resolution)

		sem := make(chan struct{}, 5)
		errChan := make(chan error, 10)
		var wg sync.WaitGroup

		err := filepath.Walk(resolutionPath, func(filePath string, info os.FileInfo, err error) error {
			if err != nil {
				return err
			}
			if info.IsDir() {
				return nil
			}

			wg.Add(1)
			sem <- struct{}{}

			go func(p string, name string, res string) {
				defer wg.Done()
				defer func() { <-sem }()

				buf, err := os.ReadFile(p)
				if err != nil {
					errChan <- fmt.Errorf("failed to read file %s: %w", p, err)
					return
				}

				if err := s.Storage.UploadChunk(ctx, &buf, name, cfg.ProcessedBucketName, event.VideoId, res); err != nil {
					errChan <- err
					return
				}

				uploadMutex.Lock()
				filesUploaded++
				currentProgress := (filesUploaded * 100) / totalFiles
				currentFilesUploaded := filesUploaded

				for _, milestone := range milestones {
					if currentProgress >= milestone && !notifiedMilestones[milestone] {
						notifiedMilestones[milestone] = true
						s.sendProgressNotification(ctx, event.VideoId, milestone, res, currentFilesUploaded, totalFiles)
					}
				}
				uploadMutex.Unlock()

			}(filePath, info.Name(), resolution)

			return nil
		})

		wg.Wait()
		close(errChan)

		if err != nil {
			return fmt.Errorf("failed to walk directory %s: %w", resolutionPath, err)
		}

		for err := range errChan {
			if err != nil {
				return err
			}
		}
	}

	s.sendCompletionNotification(ctx, event.VideoId, event.Resolutions, duration, totalFiles)

	return nil
}

func countTotalFiles(path string, resolutions []string) int {
	total := 0
	for _, resolution := range resolutions {
		resolution = strings.Trim(resolution, "[]")
		resolutionPath := filepath.Join(path, resolution)

		filepath.Walk(resolutionPath, func(_ string, info os.FileInfo, err error) error {
			if err == nil && !info.IsDir() {
				total++
			}
			return nil
		})
	}
	return total
}

func (s *TranscodingService) sendProgressNotification(ctx context.Context, videoId string, progress int, resolution string, filesUploaded, totalFiles int) {
	if s.ChunkPublisher == nil {
		return
	}

	notification := event.ProgressNotification{
		VideoId:       videoId,
		Progress:      progress,
		Status:        "transcoding",
		Resolution:    resolution,
		FilesUploaded: filesUploaded,
		TotalFiles:    totalFiles,
		Timestamp:     time.Now(),
	}

	if err := s.ChunkPublisher.Publish(ctx, notification); err != nil {
		log.Printf("Failed to send %d%% progress notification for video %s: %v", progress, videoId, err)
	} else {
		log.Printf("Sent %d%% progress notification for video %s", progress, videoId)
	}
}

func (s *TranscodingService) sendCompletionNotification(ctx context.Context, videoId string, resolutions []string, duration float64, totalFiles int) {
	if s.CompletionPublisher == nil {
		return
	}

	cleanResolutions := make([]string, len(resolutions))
	for i, res := range resolutions {
		cleanResolutions[i] = strings.Trim(res, "[]")
	}

	notification := event.CompletionNotification{
		VideoId:     videoId,
		Status:      "completed",
		Resolutions: cleanResolutions,
		Duration:    duration,
		TotalFiles:  totalFiles,
		Timestamp:   time.Now(),
	}

	if err := s.CompletionPublisher.Publish(ctx, notification); err != nil {
		log.Printf("Failed to send completion notification for video %s: %v", videoId, err)
	} else {
		log.Printf("Sent completion notification for video %s", videoId)
	}
}

func (s *TranscodingService) SendFailureNotification(ctx context.Context, videoId string, errorMessage string) {
	if s.CompletionPublisher == nil {
		return
	}

	notification := event.CompletionNotification{
		VideoId:   videoId,
		Status:    "failed",
		Timestamp: time.Now(),
	}

	if err := s.CompletionPublisher.Publish(ctx, notification); err != nil {
		log.Printf("Failed to send failure notification for video %s: %v", videoId, err)
	} else {
		log.Printf("Sent failure notification for video %s: %s", videoId, errorMessage)
	}
}

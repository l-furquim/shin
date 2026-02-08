package service

import (
	"context"
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"sync"
	"transcoding-service/internal/config"
	"transcoding-service/internal/model"
)

type TranscodingService struct {
	Storage StorageService
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

func (s *TranscodingService) ProcessJob(ctx context.Context, event *model.TranscodingJob, cfg *config.Config) (string, error) {

	log.Printf("Received a process job, videoId: %s, key: %s, userId: %s", event.VideoId, event.S3Key, event.UserId)

	filePath, err := s.Storage.GetRawVideo(ctx, event.S3Key, event.FileName, cfg.RawBucketName)
	if err != nil {
		return "", err
	}

	jobDir := filepath.Join(os.TempDir(), "transcoding-jobs", event.VideoId)

	log.Printf("Creating job directory: %s", jobDir)
	err = os.MkdirAll(jobDir, 0755)
	if err != nil {
		return "", fmt.Errorf("failed to create job directory: %w", err)
	}


	for _, res := range event.Resolutions {
		resolutionDir := filepath.Join(jobDir, strings.Trim(res, "[]"))
		log.Printf("Creating resolution directory: %s", resolutionDir)
		err = os.MkdirAll(resolutionDir, 0755)
		if err != nil {
			return "", fmt.Errorf("failed to create resolution directory %s: %w", resolutionDir, err)
		}
	}

	for _, args := range BuildDashCommands(filePath, jobDir, event.Resolutions) {
		if err := runFFmpeg(args); err != nil {
			return "", fmt.Errorf("ffmpeg failed: %w", err)
		}
	}

	log.Printf("Transcoding job processed successfully for file: %s\n", filePath)

	return jobDir, nil
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

func (s *TranscodingService) SendChunksToStorage(ctx context.Context, path string, event *model.TranscodingJob, cfg *config.Config) error {
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

			go func(p string, name string) {
				defer wg.Done()
				defer func() { <-sem }()

				buf, err := os.ReadFile(p)
				if err != nil {
					errChan <- fmt.Errorf("failed to read file %s: %w", p, err)
					return
				}

				if err := s.Storage.UploadChunk(ctx, &buf, name, cfg.ProcessedBucketName, event.VideoId, resolution); err != nil {
					errChan <- err
				}
			}(filePath, info.Name())

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
	return nil
}

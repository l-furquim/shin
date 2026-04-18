package service

import (
	"bufio"
	"context"
	"fmt"
	"log"
	"math"
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
)

type CompletionSender interface {
	Send(ctx context.Context, payload any) error
}

type ProgressSender interface {
	Send(ctx context.Context, payload any) error
}

type TranscodingService struct {
	Storage            StorageService
	ProgressProducer   ProgressSender
	CompletionProducer CompletionSender
}

type ResolutionConfig struct {
	Height       int
	Bitrate      string
	MaxBitrate   string
	BufSize      string
	Profile      string
	Level        string
	AudioBitrate string
}

var resolutionMap = map[string]ResolutionConfig{
	"1080p": {Height: 1080, Bitrate: "5000k", MaxBitrate: "5350k", BufSize: "7500k", Profile: "high", Level: "4.0", AudioBitrate: "192k"},
	"720p":  {Height: 720, Bitrate: "2800k", MaxBitrate: "2996k", BufSize: "4200k", Profile: "main", Level: "3.1", AudioBitrate: "128k"},
	"480p":  {Height: 480, Bitrate: "1400k", MaxBitrate: "1498k", BufSize: "2100k", Profile: "main", Level: "3.1", AudioBitrate: "128k"},
	"360p":  {Height: 360, Bitrate: "800k", MaxBitrate: "856k", BufSize: "1200k", Profile: "baseline", Level: "3.0", AudioBitrate: "96k"},
}

type progressTracker struct {
	mu             sync.Mutex
	transcoding    map[string]float64
	uploading      map[string]float64
	numResolutions int
	lastPublished  int
	startTime      time.Time
}

func (pt *progressTracker) aggregate() int {
	var sum float64
	for r := range pt.transcoding {
		sum += pt.transcoding[r]*0.8 + pt.uploading[r]*0.2
	}
	return int(sum / float64(pt.numResolutions))
}

func (pt *progressTracker) maybePublish(ctx context.Context, videoId string, producer ProgressSender) {
	current := pt.aggregate()
	if current-pt.lastPublished < 1 {
		return
	}
	pt.lastPublished = current
	elapsed := int64(time.Since(pt.startTime).Seconds())
	pt.mu.Unlock()
	evt := event.EncodingProgressEvent{
		VideoId:               videoId,
		Progress:              current,
		TimeProcessingSeconds: elapsed,
	}
	if err := producer.Send(ctx, evt); err != nil {
		log.Printf("[video=%s] failed to publish progress %d%%: %v", videoId, current, err)
	} else {
		log.Printf("[video=%s] progress %d%% published (%ds elapsed)", videoId, current, elapsed)
	}
	pt.mu.Lock()
}

func (pt *progressTracker) updateTranscoding(ctx context.Context, resolution string, pct float64, videoId string, producer ProgressSender) {
	pt.mu.Lock()
	pt.transcoding[resolution] = pct
	pt.maybePublish(ctx, videoId, producer)
	pt.mu.Unlock()
}

func (pt *progressTracker) updateUploading(ctx context.Context, resolution string, pct float64, videoId string, producer ProgressSender) {
	pt.mu.Lock()
	pt.uploading[resolution] = pct
	pt.maybePublish(ctx, videoId, producer)
	pt.mu.Unlock()
}

func (s *TranscodingService) ProcessJob(ctx context.Context, job *model.TranscodingJob, cfg *config.Config) error {
	jobStart := time.Now()
	log.Printf("[video=%s] job started: key=%s userId=%s resolutions=%v", job.VideoId, job.S3Key, job.UserId, job.Resolutions)

	filePath, fileInfo, err := s.Storage.GetRawVideo(ctx, job.S3Key, job.FileName, cfg.RawBucketName)
	if err != nil {
		return fmt.Errorf("[video=%s] failed to download raw video: %w", job.VideoId, err)
	}
	log.Printf("[video=%s] raw video downloaded in %.1fs", job.VideoId, time.Since(jobStart).Seconds())

	duration, err := getVideoDuration(filePath)
	if err != nil {
		log.Printf("[video=%s] warning: could not extract duration: %v", job.VideoId, err)
		duration = 0
	} else {
		log.Printf("[video=%s] duration: %ds", job.VideoId, duration)
	}

	jobDir := filepath.Join(os.TempDir(), "transcoding-jobs", job.VideoId)

	err = os.MkdirAll(jobDir, 0755)
	if err != nil {
		return fmt.Errorf("failed to create job directory: %w", err)
	}
	defer func() {
		log.Printf("[video=%s] cleaning up job directory", job.VideoId)
		os.RemoveAll(jobDir)
	}()

	cleanResolutions := make([]string, 0, len(job.Resolutions))
	for _, r := range job.Resolutions {
		res := strings.Trim(r, "[]")
		resolutionDir := filepath.Join(jobDir, res)
		if err := os.MkdirAll(resolutionDir, 0755); err != nil {
			return fmt.Errorf("failed to create resolution directory %s: %w", resolutionDir, err)
		}
		cleanResolutions = append(cleanResolutions, res)
	}

	tracker := &progressTracker{
		transcoding:    make(map[string]float64),
		uploading:      make(map[string]float64),
		numResolutions: len(cleanResolutions),
		startTime:      jobStart,
	}
	for _, res := range cleanResolutions {
		tracker.transcoding[res] = 0
		tracker.uploading[res] = 0
	}

	commandMap := BuildDashCommands(filePath, jobDir, cleanResolutions)

	log.Printf("[video=%s] launching %d resolution(s) in parallel: %v", job.VideoId, len(cleanResolutions), cleanResolutions)

	var wg sync.WaitGroup
	errCh := make(chan error, len(cleanResolutions))

	for _, res := range cleanResolutions {
		wg.Add(1)
		go func(res string) {
			defer wg.Done()
			label := fmt.Sprintf("[video=%s][%s]", job.VideoId, res)

			args, ok := commandMap[res]
			if !ok {
				errCh <- fmt.Errorf("%s no ffmpeg command found", label)
				return
			}

			log.Printf("%s transcoding started", label)
			transcodingStart := time.Now()

			if err := runFFmpegWithProgress(ctx, label, args, duration, func(pct float64) {
				tracker.updateTranscoding(ctx, res, pct, job.VideoId, s.ProgressProducer)
			}); err != nil {
				log.Printf("%s transcoding failed after %.1fs: %v", label, time.Since(transcodingStart).Seconds(), err)
				errCh <- fmt.Errorf("ffmpeg failed for %s: %w", res, err)
				return
			}

			tracker.updateTranscoding(ctx, res, 100, job.VideoId, s.ProgressProducer)
			log.Printf("%s transcoding done in %.1fs", label, time.Since(transcodingStart).Seconds())

			resPath := filepath.Join(jobDir, res)
			uploadStart := time.Now()

			if err := s.uploadResolution(ctx, resPath, res, job, tracker, cfg); err != nil {
				log.Printf("%s upload failed after %.1fs: %v", label, time.Since(uploadStart).Seconds(), err)
				errCh <- fmt.Errorf("upload failed for %s: %w", res, err)
				return
			}

			tracker.updateUploading(ctx, res, 100, job.VideoId, s.ProgressProducer)
			log.Printf("%s upload done in %.1fs", label, time.Since(uploadStart).Seconds())
		}(res)
	}

	wg.Wait()
	close(errCh)

	if err := <-errCh; err != nil {
		log.Printf("[video=%s] job failed after %.1fs: %v", job.VideoId, time.Since(jobStart).Seconds(), err)
		return err
	}

	totalFiles := 0
	for _, res := range cleanResolutions {
		resPath := filepath.Join(jobDir, res)
		filepath.Walk(resPath, func(_ string, info os.FileInfo, err error) error {
			if err == nil && !info.IsDir() {
				totalFiles++
			}
			return nil
		})
	}

	log.Printf("[video=%s] all resolutions complete in %.1fs — %d total files, %ds duration", job.VideoId, time.Since(jobStart).Seconds(), totalFiles, duration)

	s.sendCompletionNotification(ctx, job.VideoId, cleanResolutions, duration, totalFiles, fileInfo)

	return nil
}

func (s *TranscodingService) uploadResolution(ctx context.Context, resPath string, resolution string, job *model.TranscodingJob, tracker *progressTracker, cfg *config.Config) error {
	label := fmt.Sprintf("[video=%s][%s]", job.VideoId, resolution)

	var files []string
	err := filepath.Walk(resPath, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if !info.IsDir() {
			files = append(files, path)
		}
		return nil
	})
	if err != nil {
		return fmt.Errorf("failed to walk directory %s: %w", resPath, err)
	}

	if len(files) == 0 {
		return fmt.Errorf("no files found to upload for resolution %s", resolution)
	}

	total := len(files)
	log.Printf("%s uploading %d file(s) to S3", label, total)
	var (
		uploaded    int
		uploadMutex sync.Mutex
	)

	sem := make(chan struct{}, 5)
	errCh := make(chan error, total)
	var wg sync.WaitGroup

	for _, filePath := range files {
		wg.Add(1)
		sem <- struct{}{}

		go func(p string) {
			defer wg.Done()
			defer func() { <-sem }()

			buf, err := os.ReadFile(p)
			if err != nil {
				errCh <- fmt.Errorf("failed to read file %s: %w", p, err)
				return
			}

			name := filepath.Base(p)
			if err := s.Storage.UploadChunk(ctx, &buf, name, cfg.ProcessedBucketName, job.VideoId, resolution); err != nil {
				log.Printf("%s failed to upload %s: %v", label, name, err)
				errCh <- err
				return
			}

			uploadMutex.Lock()
			uploaded++
			current := uploaded
			pct := float64(current) / float64(total) * 100.0
			uploadMutex.Unlock()

			log.Printf("%s uploaded %s (%d/%d)", label, name, current, total)
			tracker.updateUploading(ctx, resolution, pct, job.VideoId, s.ProgressProducer)
		}(filePath)
	}

	wg.Wait()
	close(errCh)

	for err := range errCh {
		if err != nil {
			return err
		}
	}

	return nil
}

func BuildDashCommands(input string, baseOutputDir string, resolutions []string) map[string][]string {
	commands := make(map[string][]string)

	for _, res := range resolutions {
		res = strings.Trim(res, "[]")

		cfg, ok := resolutionMap[res]
		if !ok {
			log.Printf("Resolution ignored: %s", res)
			continue
		}

		outputDir := filepath.Join(baseOutputDir, res)

		args := []string{
			"-y",
			"-i", input,
			"-vf", fmt.Sprintf("scale=-2:%d,format=yuv420p", cfg.Height),

			"-c:v", "libx264",
			"-preset", "slow",
			"-b:v", cfg.Bitrate,
			"-maxrate", cfg.MaxBitrate,
			"-bufsize", cfg.BufSize,
			"-profile:v", cfg.Profile,
			"-level", cfg.Level,

			"-g", "120",
			"-keyint_min", "120",
			"-sc_threshold", "0",

			"-map", "0:v:0",
			"-map", "0:a?",
			"-c:a", "aac",
			"-b:a", cfg.AudioBitrate,
			"-ac", "2",
			"-ar", "44100",

			"-f", "dash",
			"-seg_duration", "4",
			"-use_timeline", "1",
			"-use_template", "1",
			"-adaptation_sets", "id=0,streams=v id=1,streams=a",

			"-init_seg_name", "init-$RepresentationID$.m4s",
			"-media_seg_name", "chunk-$RepresentationID$-$Number$.m4s",

			filepath.Join(outputDir, "manifest.mpd"),
		}

		commands[res] = args
	}

	return commands
}

func runFFmpegWithProgress(ctx context.Context, label string, args []string, duration int64, onProgress func(pct float64)) error {
	fullArgs := append([]string{"-progress", "pipe:1"}, args...)
	cmd := exec.CommandContext(ctx, "ffmpeg", fullArgs...)

	stdout, err := cmd.StdoutPipe()
	if err != nil {
		return fmt.Errorf("failed to create stdout pipe: %w", err)
	}
	cmd.Stderr = os.Stderr

	if err := cmd.Start(); err != nil {
		return fmt.Errorf("failed to start ffmpeg: %w", err)
	}

	var lastLoggedMilestone int
	scanner := bufio.NewScanner(stdout)
	for scanner.Scan() {
		line := scanner.Text()
		if strings.HasPrefix(line, "out_time_ms=") {
			valStr := strings.TrimPrefix(line, "out_time_ms=")
			if val, parseErr := strconv.ParseFloat(valStr, 64); parseErr == nil && duration > 0 {
				pct := val / 1000.0 / float64(duration) * 100.0
				if pct > 100 {
					pct = 100
				}
				milestone := int(pct/25) * 25
				if milestone > lastLoggedMilestone {
					lastLoggedMilestone = milestone
					log.Printf("%s ffmpeg %d%%", label, milestone)
				}
				onProgress(pct)
			}
		}
	}

	if err := cmd.Wait(); err != nil {
		return fmt.Errorf("ffmpeg command failed: %w", err)
	}

	return nil
}

func getVideoDuration(videoPath string) (int64, error) {
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
	durationFloat, err := strconv.ParseFloat(durationStr, 64)
	if err != nil {
		return 0, fmt.Errorf("failed to parse duration: %w", err)
	}

	if durationFloat <= 0 {
		return 0, nil
	}

	duration := int64(math.Ceil(durationFloat))

	return duration, nil
}

func (s *TranscodingService) sendCompletionNotification(ctx context.Context, videoId string, resolutions []string, duration int64, totalFiles int, fileInfo *model.VideoFileInfo) {
	if s.CompletionProducer == nil {
		return
	}

	notification := event.CompletionNotification{
		VideoId:       videoId,
		Status:        "completed",
		ProcessedPath: "videos/" + videoId,
		Resolutions:   resolutions,
		Duration:      duration,
		TotalFiles:    totalFiles,
		Timestamp:     time.Now(),
	}

	if fileInfo != nil {
		notification.FileName = fileInfo.FileName
		notification.FileSize = fileInfo.FileSize
		notification.FileType = fileInfo.ContentType
	}

	if err := s.CompletionProducer.Send(ctx, notification); err != nil {
		log.Printf("Failed to send completion notification for video %s: %v", videoId, err)
	} else {
		log.Printf("Sent completion notification for video %s", videoId)
	}
}

func (s *TranscodingService) SendFailureNotification(ctx context.Context, videoId string, errorMessage string) {
	if s.CompletionProducer == nil {
		return
	}

	notification := event.CompletionNotification{
		VideoId:       videoId,
		Status:        "failed",
		ProcessedPath: "videos/" + videoId,
		Timestamp:     time.Now(),
	}

	if err := s.CompletionProducer.Send(ctx, notification); err != nil {
		log.Printf("Failed to send failure notification for video %s: %v", videoId, err)
	} else {
		log.Printf("Sent failure notification for video %s: %s", videoId, errorMessage)
	}
}

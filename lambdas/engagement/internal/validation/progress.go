package validation

import (
	"errors"
	"fmt"
	"time"

	"engagement/internal/model"
)

const (
	maxWatchTimeRatio  = 1.2
	watchTimeBufferSec = 10
)

func ValidateProgressRecord(r *model.ProgressRecord) error {
	if r.SessionID == "" {
		return errors.New("sessionId is required")
	}
	if r.VideoID == "" {
		return errors.New("videoId is required")
	}
	if r.VideoDurationSeconds <= 0 {
		return errors.New("videoDurationSeconds must be greater than 0")
	}
	if r.VideoDurationSeconds > 86400 {
		return errors.New("videoDurationSeconds must not exceed 86400 seconds")
	}
	if r.WatchTimeSeconds <= 0 {
		return errors.New("watchTimeSeconds must be greater than 0")
	}
	if r.WatchTimeSeconds > r.VideoDurationSeconds {
		return errors.New("watchTimeSeconds must not exceed videoDurationSeconds")
	}
	if r.CurrentPositionSeconds < 0 {
		return errors.New("currentPositionSeconds must be greater than or equal to 0")
	}
	if r.CurrentPositionSeconds > r.VideoDurationSeconds {
		return errors.New("currentPositionSeconds must not exceed videoDurationSeconds")
	}
	return nil
}

func ValidateEventFreshness(eventTime time.Time, maxAge time.Duration) error {
	age := time.Since(eventTime)
	if age > maxAge {
		return fmt.Errorf("event is %v old, max allowed is %v", age.Round(time.Second), maxAge)
	}
	if age < -10*time.Second {
		return fmt.Errorf("event timestamp is in the future by %v", (-age).Round(time.Second))
	}
	return nil
}

func ValidateWatchTimeConsistency(watchTimeSeconds int, eventTime time.Time, lastUpdatedAt time.Time) error {
	if lastUpdatedAt.IsZero() {
		return nil
	}
	elapsed := eventTime.Sub(lastUpdatedAt).Seconds()
	maxAllowed := elapsed*maxWatchTimeRatio + watchTimeBufferSec
	if float64(watchTimeSeconds) > maxAllowed {
		return fmt.Errorf("watchTimeSeconds %d exceeds elapsed time %.0fs (max allowed %.0fs)", watchTimeSeconds, elapsed, maxAllowed)
	}
	return nil
}

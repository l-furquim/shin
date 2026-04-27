package event

import "time"

type EncodingProgressEvent struct {
	VideoId               string `json:"videoId"`
	Progress              int    `json:"progress"`
	TimeProcessingSeconds int64  `json:"timeProcessingSeconds"`
	Failure               string `json:"failure,omitempty"`
}

type CompletionNotification struct {
	VideoId       string    `json:"videoId"`
	Status        string    `json:"status"`
	ProcessedPath string    `json:"processedPath"`
	Resolutions   []string  `json:"resolutions"`
	Duration      int64     `json:"duration"`
	TotalFiles    int       `json:"totalFiles"`
	FileName      string    `json:"fileName"`
	FileSize      int64     `json:"fileSize"`
	FileType      string    `json:"fileType"`
	Timestamp     time.Time `json:"timestamp"`
}

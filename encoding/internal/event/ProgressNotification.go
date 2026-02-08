package event

import "time"

type ProgressNotification struct {
	VideoId       string    `json:"videoId"`
	Progress      int       `json:"progress"`
	Status        string    `json:"status"`
	Resolution    string    `json:"resolution"`
	FilesUploaded int       `json:"filesUploaded"`
	TotalFiles    int       `json:"totalFiles"`
	Timestamp     time.Time `json:"timestamp"`
}

type CompletionNotification struct {
	VideoId     string    `json:"videoId"`
	Status      string    `json:"status"`
	Resolutions []string  `json:"resolutions"`
	Duration    float64   `json:"duration"`
	TotalFiles  int       `json:"totalFiles"`
	Timestamp   time.Time `json:"timestamp"`
}

package model

import "time"

type ViewEvent struct {
	SessionID              string    `json:"sessionId"`
	VideoID                string    `json:"videoId"`
	UserID                 string    `json:"userId"`
	WatchTimeSeconds       int       `json:"watchTimeSeconds"`
	CurrentPositionSeconds int       `json:"currentPositionSeconds"`
	VideoDurationSeconds   int       `json:"videoDurationSeconds"`
	EventTimestamp         time.Time `json:"eventTimestamp"`
}

type ProgressRecord struct {
	SessionID              string    `json:"sessionId"`
	VideoID                string    `json:"videoId"`
	UserID                 string    `json:"userId"`
	WatchTimeSeconds       int       `json:"watchTimeSeconds"`
	CurrentPositionSeconds int       `json:"currentPositionSeconds"`
	VideoDurationSeconds   int       `json:"videoDurationSeconds"`
	EventTimestamp         time.Time `json:"eventTimestamp"`
}

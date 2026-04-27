package model

type Status int

const (
	IDLE Status = iota
	SUCCESS
	FAILED
	PROCESSING
)

type TranscodingJob struct {
	VideoId     string   `json:"videoId"`
	S3Key       string   `json:"s3Key"`
	UserId      string   `json:"userId"`
	FileName    string   `json:"fileName"`
	Status      Status   `json:"status"`
	Resolutions []string `json:"resolutions"`
}

type VideoFileInfo struct {
	FileName    string
	FileSize    int64
	ContentType string
}

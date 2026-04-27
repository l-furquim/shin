package event

import (
	"strings"
	"transcoding-service/internal/model"
)

type TranscodingEvent struct {
	VideoId     string   `json:"videoId"`
	S3Key       string   `json:"s3Key"`
	UserId      string   `json:"userId"`
	FileName    string   `json:"fileName"`
	Resolutions []string `json:"resolutions"`
}

func (e *TranscodingEvent) ToModel() *model.TranscodingJob {
	resolutions := make([]string, 0)
	for _, res := range e.Resolutions {
		clean := strings.TrimSpace(res)
		if clean != "" {
			resolutions = append(resolutions, clean)
		}
	}

	return &model.TranscodingJob{
		FileName:    e.FileName,
		S3Key:       e.S3Key,
		UserId:      e.UserId,
		VideoId:     e.VideoId,
		Status:      model.IDLE,
		Resolutions: resolutions,
	}
}

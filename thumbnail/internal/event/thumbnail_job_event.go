package event

import "thumbnail-service/internal/model"

type ThumbnailJobEvent struct {
	VideoId string `json:"videoId"`
	S3Key   string `json:"s3Key"`
}

func (e *ThumbnailJobEvent) ToModel() *model.ThumbnailJob {
	return &model.ThumbnailJob{
		VideoId: e.VideoId,
		S3Key:   e.S3Key,
	}
}

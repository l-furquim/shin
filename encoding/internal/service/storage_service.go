package service

import (
	"context"
	"transcoding-service/internal/model"
)

type StorageService interface {
	UploadChunk(ctx context.Context, data *[]byte, fileName string, bucketName string, videoId string, resolution string) error
	GetRawVideo(ctx context.Context, key string, fileName string, bucketName string) (string, *model.VideoFileInfo, error)
	GetObjectMetadata(ctx context.Context, key string, bucketName string) (map[string]string, error)
}

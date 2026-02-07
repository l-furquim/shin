package service

import "context"

type StorageService interface {
	UploadChunk(ctx context.Context, data *[]byte, fileName string, bucketName string, videoId string, resolution string) error
	GetRawVideo(ctx context.Context, key string, fileName string, bucketName string) (string, error)
}

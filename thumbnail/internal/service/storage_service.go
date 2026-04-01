package service

import "context"

type StorageService interface {
	GetRawVideo(ctx context.Context, key string, bucketName string) (string, error)
	GetObjectMetadata(ctx context.Context, key string, bucketName string) (map[string]string, error)
	UploadThumbnail(ctx context.Context, data *[]byte, key string, bucketName string) error
}

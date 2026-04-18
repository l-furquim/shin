package s3

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"io"
	"log"
	"os"
	"path/filepath"
	"strings"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/aws/aws-sdk-go-v2/service/s3/types"
)

type StorageService struct {
	c *s3.Client
}

const outputDir = "/tmp/thumbnails"

func NewStorageService(c *s3.Client) *StorageService {
	return &StorageService{
		c: c,
	}
}

func (s *StorageService) DownloadObject(ctx context.Context, key string, bucketName string) (string, error) {
	result, err := s.c.GetObject(ctx, &s3.GetObjectInput{
		Bucket: aws.String(bucketName),
		Key:    aws.String(key),
	})
	if err != nil {
		var noKey *types.NoSuchKey
		if errors.As(err, &noKey) {
			return "", fmt.Errorf("object %s not found in bucket %s: %w", key, bucketName, err)
		}
		return "", fmt.Errorf("failed to get object %s from bucket %s: %w", key, bucketName, err)
	}
	defer result.Body.Close()

	if err := os.MkdirAll(outputDir, 0755); err != nil {
		return "", fmt.Errorf("failed to create output directory %s: %w", outputDir, err)
	}

	safeKey := strings.ReplaceAll(key, "/", "_")
	fileName := filepath.Base(safeKey)
	filePath := filepath.Join(outputDir, fileName)
	file, err := os.Create(filePath)
	if err != nil {
		return "", fmt.Errorf("failed to create file %s: %w", filePath, err)
	}
	defer file.Close()

	if _, err := io.Copy(file, result.Body); err != nil {
		os.Remove(filePath)
		return "", fmt.Errorf("failed to write video data to %s: %w", filePath, err)
	}

	log.Printf("Successfully downloaded %s from %s to %s", key, bucketName, filePath)
	return filePath, nil
}

func (s *StorageService) UploadThumbnail(ctx context.Context, data []byte, key string, bucketName string, contentType string, metadata map[string]string) error {
	input := &s3.PutObjectInput{
		Bucket:      aws.String(bucketName),
		Key:         aws.String(key),
		Body:        bytes.NewReader(data),
		ContentType: aws.String(contentType),
	}

	if len(metadata) > 0 {
		input.Metadata = metadata
	}

	_, err := s.c.PutObject(ctx, input)
	if err != nil {
		return fmt.Errorf("failed to upload thumbnail %s to %s: %w", key, bucketName, err)
	}

	log.Printf("Successfully uploaded thumbnail to %s", key)
	return nil
}

func (s *StorageService) GetObjectMetadata(ctx context.Context, key string, bucketName string) (map[string]string, error) {
	result, err := s.c.HeadObject(ctx, &s3.HeadObjectInput{
		Bucket: aws.String(bucketName),
		Key:    aws.String(key),
	})
	if err != nil {
		return nil, fmt.Errorf("failed to head object %s from bucket %s: %w", key, bucketName, err)
	}

	metadata := make(map[string]string)
	for k, v := range result.Metadata {
		metadata[k] = v
	}

	return metadata, nil
}

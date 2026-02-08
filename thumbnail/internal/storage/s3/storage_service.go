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
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/aws/aws-sdk-go-v2/service/s3/types"
)

type StorageService struct {
	c *s3.Client
}

const OUTPUT_DIR = "/tmp/thumbnails"

func NewStorageService(c *s3.Client) *StorageService {
	return &StorageService{
		c: c,
	}
}

func (s *StorageService) GetRawVideo(ctx context.Context, key string, bucketName string) (string, error) {
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

	if err := os.MkdirAll(OUTPUT_DIR, 0755); err != nil {
		return "", fmt.Errorf("failed to create output directory %s: %w", OUTPUT_DIR, err)
	}

	fileName := filepath.Base(key)
	filePath := filepath.Join(OUTPUT_DIR, fileName)
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

func (s *StorageService) UploadThumbnail(ctx context.Context, data *[]byte, key string, bucketName string) error {
	_, err := s.c.PutObject(ctx, &s3.PutObjectInput{
		Bucket:      aws.String(bucketName),
		Key:         aws.String(key),
		Body:        bytes.NewReader(*data),
		ContentType: aws.String("image/jpeg"),
	})
	if err != nil {
		return fmt.Errorf("failed to upload thumbnail %s to %s: %w", key, bucketName, err)
	}

	if err := s3.NewObjectExistsWaiter(s.c).Wait(
		ctx, &s3.HeadObjectInput{Bucket: aws.String(bucketName), Key: aws.String(key)}, time.Minute,
	); err != nil {
		return fmt.Errorf("failed to verify thumbnail %s exists: %w", key, err)
	}

	log.Printf("Successfully uploaded thumbnail to %s", key)
	return nil
}

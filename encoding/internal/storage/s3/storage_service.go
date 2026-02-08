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
	"github.com/aws/smithy-go"
)

type StorageService struct {
	c *s3.Client
}

const OUTPUT_DIR = "/tmp/output"

func NewStorageService(c *s3.Client) *StorageService {
	return &StorageService{
		c: c,
	}
}

func (s *StorageService) UploadChunk(
	ctx context.Context, data *[]byte, fileName string,
	bucketName string, videoId string, resolution string,
) error {
	objectKey := fmt.Sprintf("%s/%s/%s", videoId, resolution, fileName)

	_, err := s.c.PutObject(ctx, &s3.PutObjectInput{
		Bucket: aws.String(bucketName),
		Key:    aws.String(objectKey),
		Body:   bytes.NewReader(*data),
	})
	if err != nil {
		var apiErr smithy.APIError
		if errors.As(err, &apiErr) && apiErr.ErrorCode() == "EntityTooLarge" {
			return fmt.Errorf("object too large for %s: use multipart upload for files >5GB: %w", objectKey, err)
		}
		return fmt.Errorf("failed to upload %s to %s: %w", fileName, objectKey, err)
	}

	if err := s3.NewObjectExistsWaiter(s.c).Wait(
		ctx, &s3.HeadObjectInput{Bucket: aws.String(bucketName), Key: aws.String(objectKey)}, time.Minute,
	); err != nil {
		return fmt.Errorf("failed to verify object %s exists: %w", objectKey, err)
	}

	log.Printf("Successfully uploaded %s to %s", fileName, objectKey)
	return nil
}

func (s *StorageService) GetRawVideo(ctx context.Context, key string, fileName string, bucketName string) (string, error) {
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

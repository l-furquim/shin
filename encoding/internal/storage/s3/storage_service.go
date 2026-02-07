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

	reader := bytes.NewReader(*data)

	objectKey := fmt.Sprintf("processed/%s/%s/%s", videoId, resolution, fileName)

	_, err := s.c.PutObject(ctx, &s3.PutObjectInput{
		Bucket: aws.String(bucketName),
		Key:    aws.String(objectKey),
		Body:   reader,
	})

	if err != nil {
		var apiErr smithy.APIError
		if errors.As(err, &apiErr) && apiErr.ErrorCode() == "EntityTooLarge" {
			log.Printf("Error while uploading object to %s. The object is too large.\n"+
				"To upload objects larger than 5GB, use the S3 console (160GB max)\n"+
				"or the multipart upload API (5TB max).", bucketName)
		} else {
			log.Printf("Couldn't upload file %v to %v:%v. Here's why: %v\n",
				fileName, bucketName, objectKey, err)
		}
	} else {
		err = s3.NewObjectExistsWaiter(s.c).Wait(
			ctx, &s3.HeadObjectInput{Bucket: aws.String(bucketName), Key: aws.String(objectKey)}, time.Minute)
		if err != nil {
			log.Printf("Failed attempt to wait for object %s to exist.\n", objectKey)
		}
	}

	return err
}

func (s *StorageService) GetRawVideo(ctx context.Context, key string, fileName string, bucketName string) (string, error) {
	result, err := s.c.GetObject(ctx, &s3.GetObjectInput{
		Bucket: aws.String(bucketName),
		Key:    aws.String(key),
	})

	if err != nil {
		var noKey *types.NoSuchKey
		if errors.As(err, &noKey) {
			log.Printf("Can't get object %s from bucket %s. No such key exists.\n", key, bucketName)
			err = noKey
		} else {
			log.Printf("Couldn't get object %v:%v. Here's why: %v\n", bucketName, key, err)
		}
		return "", err
	}

	defer result.Body.Close()

	err = os.MkdirAll(OUTPUT_DIR, os.ModePerm)

	if err != nil {
		log.Printf("Error while creating the output directory: %s", err)
	}

	filePath := filepath.Join(OUTPUT_DIR, fileName)

	file, err := os.Create(filePath)

	if err != nil {
		log.Printf("Error while creating the video downloaded from the bucket: %s, %s\n", bucketName, err)
		return "", err
	}

	defer file.Close()

	body, err := io.ReadAll(result.Body)

	if err != nil {
		log.Printf("Error while reading the downloaded file from the bucket: %s, %s\n", bucketName, err)
	}

	_, err = file.Write(body)

	return filePath, err
}

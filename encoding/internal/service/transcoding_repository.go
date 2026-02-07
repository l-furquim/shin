package service

import (
	"context"
	"transcoding-service/internal/model"
)

type TranscodingRepository interface {
	Save(ctx context.Context, model *model.TranscodingJob) error
	// FindByID(ctx context.Context, id string) (*model.TranscodingJob, error)
}

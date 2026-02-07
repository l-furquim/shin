package mongo

import (
	"context"
	"transcoding-service/internal/model"

	"go.mongodb.org/mongo-driver/mongo"
)

type TranscodingRepository struct {
	collection *mongo.Collection
}

func NewTranscodingRepository(db *mongo.Database) *TranscodingRepository {
	return &TranscodingRepository{
		collection: db.Collection("transcoding-jobs"),
	}
}

func (r *TranscodingRepository) Save(ctx context.Context, model *model.TranscodingJob) error {
	_, err := r.collection.InsertOne(ctx, model)

	return err
}

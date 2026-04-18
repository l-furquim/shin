package model

type ThumbnailJob struct {
	VideoID      string `json:"videoId"`
	SourceKey    string `json:"sourceKey"`
	SourceBucket string `json:"sourceBucket"`
	IsCustom     bool   `json:"isCustom"`
}

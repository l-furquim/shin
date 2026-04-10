variable "env" {
  description = "Environment name (e.g., dev, staging, prod)"
  type        = string
}

variable "region" {
  description = "AWS region where resources will be created"
  type        = string
  default     = "us-east-1"
}

variable "raw_bucket_name" {
  description = "Name of the S3 bucket for raw/uploaded files"
  type        = string
}

variable "processed_bucket_name" {
  description = "Name of the S3 bucket for processed files"
  type        = string
}

variable "thumbnail_bucket_name" {
  description = "Name of the S3 bucket for thumbnail files"
  type        = string
}

variable "creator_pictures_bucket_name" {
  description = "Name of the S3 bucket for creator pictures"
  type        = string
}

variable "sns_topic_names" {
  description = "List of SNS topic names to create"
  type        = list(string)
}

variable "sns_fanout_subscriptions" {
  description = "Map of SNS topic names to SQS queue names for fanout subscriptions"
  type        = map(list(string))
  default     = {}
}

variable "sqs_queue_names" {
  description = "List of SQS queue names to create"
  type        = list(string)
  default = [
    "decode-job",
    "thumbnail-job",
    "raw-upload-metadata-queue",
    "encoding-finished-events",
    "thumbnail-finished-events",
    "view-events",
    "thread-created",
    "comment-reply-created",
    "comment-updated",
    "comment-deleted",
    "video-initialized",
    "like-events",
    "dislike-events",
    "channel-subscribed",
    "channel-unsubscribed",
  ]
}

variable "enable_view_eventbridge_pipeline" {
  description = "Enable EventBridge pipeline for CloudFront/S3 view events"
  type        = bool
  default     = false
}

variable "sqs_message_retention_seconds" {
  description = "Number of seconds SQS retains a message"
  type        = number
  default     = 345600 # 4 days
}

variable "sqs_visibility_timeout_seconds" {
  description = "Visibility timeout for SQS messages"
  type        = number
  default     = 300 // 5 minutes
}

variable "sqs_receive_wait_time_seconds" {
  description = "Wait time for SQS long polling"
  type        = number
  default     = 20
}

variable "common_tags" {
  description = "Common tags to apply to all resources"
  type        = map(string)
  default     = {}
}

variable "jwt_secret" {
  description = "Secret key for signing playback JWT tokens"
  type        = string
  sensitive   = true
}

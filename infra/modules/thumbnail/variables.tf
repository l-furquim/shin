variable "env" {
  type = string
}

variable "thumbnail_job_queue_arn" {
  type        = string
  description = "ARN of the thumbnail-job SQS queue (Lambda trigger)"
}

variable "thumbnail_finished_queue_arn" {
  type        = string
  description = "ARN of the thumbnail-finished-events SQS queue"
}

variable "thumbnail_finished_queue_url" {
  type        = string
  description = "URL of the thumbnail-finished-events SQS queue"
}

variable "raw_bucket_arn" {
  type        = string
  description = "ARN of the raw video S3 bucket"
}

variable "raw_bucket_name" {
  type        = string
  description = "Name of the raw video S3 bucket"
}

variable "thumbnail_bucket_arn" {
  type        = string
  description = "ARN of the thumbnail S3 bucket"
}

variable "thumbnail_bucket_name" {
  type        = string
  description = "Name of the thumbnail S3 bucket"
}

variable "processor_zip" {
  type        = string
  description = "Path to the thumbnail-processor Lambda zip"
}

variable "processor_hash" {
  type        = string
  description = "base64sha256 of the processor zip"
}

variable "ffmpeg_layer_zip" {
  type        = string
  description = "Path to the FFmpeg Lambda Layer zip"
}

variable "ffmpeg_layer_hash" {
  type        = string
  description = "base64sha256 of the FFmpeg layer zip"
}

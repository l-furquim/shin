variable "env" {
  type = string
}

variable "view_events_queue_arn" {
  type        = string
  description = "ARN of the view-events SQS queue (processor publishes here after valid progress)"
}

variable "view_events_queue_url" {
  type        = string
  description = "URL of the view-events SQS queue"
}

variable "playback_progress_queue_arn" {
  type        = string
  description = "ARN of the video-playback-progress SQS queue (processor trigger)"
}

variable "playback_sessions_table_arn" {
  type = string
}

variable "processor_zip" {
  type        = string
  description = "Path to the engagement-processor Lambda zip"
}

variable "processor_hash" {
  type        = string
  description = "base64sha256 of the processor zip"
}

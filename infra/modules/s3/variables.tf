variable "env" {
  description = "Environment name"
  type        = string
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

variable "enable_lifecycle_rules" {
  description = "Enable lifecycle rules for transitioning objects to different storage classes"
  type        = bool
  default     = false
}

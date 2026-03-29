variable "env" {
  description = "Environment name"
  type        = string
}

variable "processed_s3_domain_name" {
  type = string
}

variable "processed_s3_arn" {
  type = string
}

variable "processed_s3_id" {
  type = string
}

variable "processed_s3_origin_name" {
  type    = string
  default = "s3Origin"
}

variable "thumbnail_s3_domain_name" {
  type = string
}

variable "thumbnail_s3_arn" {
  type = string
}

variable "thumbnail_s3_id" {
  type = string
}

variable "thumbnail_s3_origin_name" {
  type    = string
  default = "s3Origin"
}

variable "creators_s3_domain_name" {
  type = string
}

variable "creators_s3_arn" {
  type = string
}

variable "creators_s3_id" {
  type = string
}

variable "creators_s3_origin_name" {
  type    = string
  default = "s3Origin"
}

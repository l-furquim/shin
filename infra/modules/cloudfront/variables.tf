variable "env" {
  description = "Environment name"
  type        = string
}

variable "s3_domain_name" {
  type = string
}

variable "s3_arn" {
  type = string
}

variable "s3_id" {
  type = string
}

variable "s3_origin_name" {
  type    = string
  default = "s3Origin"
}

variable "env" {
  type = string
}

variable "namespace" {
  type = string
}

variable "oidc_provider_arn" {
  type = string
}

variable "oidc_provider_url" {
  type = string
}

variable "raw_bucket_arn" {
  type = string
}

variable "creator_pictures_bucket_arn" {
  type = string
}

variable "sqs_queue_arns" {
  type = map(string)
}

variable "sns_topic_arns" {
  type = map(string)
}

variable "dynamodb_table_arns" {
  type = map(string)
}

variable "cloudfront_private_key_secret_arn" {
  type = string
}

variable "opensearch_collection_arn" {
  type    = string
  default = ""
}

output "s3_bucket_names" {
  description = "Map of all S3 bucket names"
  value       = module.s3.bucket_names
}

output "raw_bucket_arn" {
  description = "ARN of the raw S3 bucket"
  value       = module.s3.raw_bucket_arn
}

output "processed_bucket_arn" {
  description = "ARN of the processed S3 bucket"
  value       = module.s3.processed_bucket_arn
}

output "thumbnail_bucket_arn" {
  description = "ARN of the thumbnail S3 bucket"
  value       = module.s3.thumbnail_bucket_arn
}

output "sns_topic_arns" {
  description = "Map of SNS topic names to their ARNs"
  value       = module.sns.topic_arns
}

output "sns_topic_names" {
  description = "List of created SNS topic names"
  value       = module.sns.topic_names
}

output "sqs_queue_arns" {
  description = "Map of SQS queue names to their ARNs"
  value       = module.sqs.queue_arns
}

output "sqs_queue_urls" {
  description = "Map of SQS queue names to their URLs"
  value       = module.sqs.queue_urls
}

output "sqs_queue_names" {
  description = "List of created SQS queue names"
  value       = module.sqs.queue_names
}

output "sqs_dlq_arns" {
  description = "Map of DLQ names to their ARNs"
  value       = module.sqs.dlq_arns
}

output "environment" {
  description = "Current environment"
  value       = var.env
}

output "region" {
  description = "AWS region"
  value       = var.region
}

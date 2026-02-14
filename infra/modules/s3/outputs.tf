output "raw_bucket_id" {
  description = "The ID of the raw S3 bucket"
  value       = aws_s3_bucket.raw_bucket.id
}

output "raw_bucket_arn" {
  description = "The ARN of the raw S3 bucket"
  value       = aws_s3_bucket.raw_bucket.arn
}

output "raw_bucket_domain_name" {
  description = "The bucket domain name of the raw S3 bucket"
  value       = aws_s3_bucket.raw_bucket.bucket_domain_name
}

output "processed_bucket_id" {
  description = "The ID of the processed S3 bucket"
  value       = aws_s3_bucket.processed_bucket.id
}

output "processed_bucket_arn" {
  description = "The ARN of the processed S3 bucket"
  value       = aws_s3_bucket.processed_bucket.arn
}

output "processed_bucket_domain_name" {
  description = "The bucket domain name of the processed S3 bucket"
  value       = aws_s3_bucket.processed_bucket.bucket_domain_name
}

output "thumbnail_bucket_id" {
  description = "The ID of the thumbnail S3 bucket"
  value       = aws_s3_bucket.thumbnail_bucket.id
}

output "thumbnail_bucket_arn" {
  description = "The ARN of the thumbnail S3 bucket"
  value       = aws_s3_bucket.thumbnail_bucket.arn
}

output "thumbnail_bucket_domain_name" {
  description = "The bucket domain name of the thumbnail S3 bucket"
  value       = aws_s3_bucket.thumbnail_bucket.bucket_domain_name
}

output "bucket_names" {
  description = "Map of all bucket names"
  value = {
    raw              = aws_s3_bucket.raw_bucket.id
    processed        = aws_s3_bucket.processed_bucket.id
    thumbnail        = aws_s3_bucket.thumbnail_bucket.id
    creator-pictures = aws_s3_bucket.creator_pictures_bucket.id
  }
}

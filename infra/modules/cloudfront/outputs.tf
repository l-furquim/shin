output "cloud_front_cdn_url" {
  value = aws_cloudfront_distribution.s3_distribution.domain_name
}

output "distribution_arn" {
  description = "CloudFront distribution ARN"
  value       = aws_cloudfront_distribution.s3_distribution.arn
}

output "distribution_id" {
  description = "CloudFront distribution ID"
  value       = aws_cloudfront_distribution.s3_distribution.id
}

output "key_pair_id" {
  description = "CloudFront public key ID used for signed cookies"
  value       = aws_cloudfront_public_key.processed_video_signing_key.id
}

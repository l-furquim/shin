output "cloud_front_cdn_url" {
  value = aws_cloudfront_distribution.s3_distribution.domain_name
}

output "key_pair_id" {
  description = "CloudFront public key ID used for signed cookies"
  value       = aws_cloudfront_public_key.processed_video_signing_key.id
}

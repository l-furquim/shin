output "cloudfront_private_key_secret_arn" {
  description = "ARN of the Secrets Manager secret holding the CloudFront private key"
  value       = aws_secretsmanager_secret.cloudfront_signed_key.arn
}

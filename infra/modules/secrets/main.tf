resource "aws_secretsmanager_secret" "cloudfront_signed_key" {
  name                    = "shin-${var.env}-cloudfront-private-key"
  description             = "RSA private key for CloudFront signed cookies"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "cloudfront_signed_key" {
  secret_id     = aws_secretsmanager_secret.cloudfront_signed_key.id
  secret_string = var.cloudfront_private_key
}

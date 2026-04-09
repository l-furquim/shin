variable "env" {
  description = "Environment name"
  type        = string
}

variable "cloudfront_private_key" {
  description = "PEM-encoded PKCS8 RSA private key for CloudFront signed cookies"
  type        = string
  sensitive   = true
}

data "aws_iam_policy_document" "processed_bucket_policy" {
  statement {
    sid    = "AllowCloudFrontServicePrincipalReadOnly"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    actions = [
      "s3:GetObject"
    ]
    resources = [
      "${var.s3_arn}/*"
    ]

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.s3_distribution.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "processed_policy" {
  bucket = var.s3_id
  policy = data.aws_iam_policy_document.processed_bucket_policy.json
}

resource "aws_cloudfront_origin_access_control" "default" {
  name                              = "processed-bucket-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "s3_distribution" {
  origin {
    domain_name              = var.s3_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.default.id
    origin_id                = var.s3_origin_name
  }

  enabled         = true
  is_ipv6_enabled = true
  web_acl_id      = "arn:aws:wafv2:us-east-1:304361287196:global/webacl/CreatedByCloudFront-fc8aac5b/723ef380-03e5-4cf2-9bd4-450f2485155f"

  default_cache_behavior {
    target_origin_id       = var.s3_origin_name
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true
    cache_policy_id        = "658327ea-f89d-4fab-a63d-7e88639e58f6"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  tags = {
    Env = var.env
  }

}

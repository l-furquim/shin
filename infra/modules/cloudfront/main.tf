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
      "${var.processed_s3_arn}/*"
    ]

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.s3_distribution.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "processed_policy" {
  bucket = var.processed_s3_id
  policy = data.aws_iam_policy_document.processed_bucket_policy.json
}

data "aws_iam_policy_document" "thumbnail_bucket_policy" {
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
      "${var.thumbnail_s3_arn}/*"
    ]

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.s3_distribution.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "thumbnail_policy" {
  bucket = var.thumbnail_s3_id
  policy = data.aws_iam_policy_document.thumbnail_bucket_policy.json
}

data "aws_iam_policy_document" "creator_bucket_policy" {
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
      "${var.creators_s3_arn}/*"
    ]

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.s3_distribution.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "creator_policy" {
  bucket = var.creators_s3_id
  policy = data.aws_iam_policy_document.creator_bucket_policy.json
}



resource "aws_cloudfront_origin_access_control" "default" {
  name                              = "processed-bucket-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "s3_distribution" {
  # Origin for processed bucket
  origin {
    domain_name              = var.processed_s3_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.default.id
    origin_id                = var.processed_s3_id
  }

  // Origin for thumbnail bucket
  origin {
    domain_name              = var.thumbnail_s3_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.default.id
    origin_id                = var.thumbnail_s3_id
  }

  // Origin for creators bucket
  origin {
    domain_name              = var.creators_s3_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.default.id
    origin_id                = var.creators_s3_id
  }

  enabled         = true
  is_ipv6_enabled = true
  web_acl_id      = "arn:aws:wafv2:us-east-1:304361287196:global/webacl/CreatedByCloudFront-fc8aac5b/723ef380-03e5-4cf2-9bd4-450f2485155f"

  default_cache_behavior {
    target_origin_id       = var.processed_s3_id
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true
    cache_policy_id        = "658327ea-f89d-4fab-a63d-7e88639e58f6"
    trusted_key_groups     = [aws_cloudfront_key_group.video_key_group.id]
  }

  ordered_cache_behavior {
    path_pattern           = "/thumbnails/*"
    target_origin_id       = var.thumbnail_s3_id
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    cache_policy_id        = "658327ea-f89d-4fab-a63d-7e88639e58f6"
  }

  ordered_cache_behavior {
    path_pattern           = "/creators/*"
    target_origin_id       = var.creators_s3_id
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
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

resource "aws_cloudfront_public_key" "processed_video_signing_key" {
  name        = "video-signing-key"
  comment     = "Public key for signed cookies"
  encoded_key = file("${path.module}/cloudfront_public_key.pem")
}

resource "aws_cloudfront_key_group" "video_key_group" {
  name    = "shin-${var.env}-video-key-group"
  comment = "Key group for signed cookie access to processed videos"
  items   = [aws_cloudfront_public_key.processed_video_signing_key.id]
}

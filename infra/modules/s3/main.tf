resource "aws_s3_bucket" "raw_bucket" {
  bucket = var.raw_bucket_name

  tags = {
    Name        = var.raw_bucket_name
    Environment = var.env
  }
}

resource "aws_s3_bucket" "processed_bucket" {
  bucket = var.processed_bucket_name

  tags = {
    Name        = var.processed_bucket_name
    Environment = var.env
  }
}

resource "aws_s3_bucket" "thumbnail_bucket" {
  bucket = var.thumbnail_bucket_name

  tags = {
    Name        = var.thumbnail_bucket_name
    Environment = var.env
  }
}

resource "aws_s3_bucket" "creator_pictures_bucket" {
  bucket = var.creator_pictures_bucket_name

  tags = {
    Name        = var.creator_pictures_bucket_name
    Environment = var.env
  }
}

resource "aws_s3_bucket" "frontend_bucket" {
  bucket = "shin-${var.env}-frontend"

  tags = {
    Environment = var.env
  }
}

resource "aws_s3_bucket_versioning" "raw_bucket" {
  bucket = aws_s3_bucket.raw_bucket.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_public_access_block" "raw_bucket" {
  bucket = aws_s3_bucket.raw_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_public_access_block" "frontend_bucket" {
  bucket = aws_s3_bucket.frontend_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}


resource "aws_s3_bucket_public_access_block" "processed_bucket" {
  bucket = aws_s3_bucket.processed_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_public_access_block" "thumbnail_bucket" {
  bucket = aws_s3_bucket.thumbnail_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "raw_bucket" {
  bucket = aws_s3_bucket.raw_bucket.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "processed_bucket" {
  bucket = aws_s3_bucket.processed_bucket.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "thumbnail_bucket" {
  bucket = aws_s3_bucket.thumbnail_bucket.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_cors_configuration" "processed_bucket" {
  bucket = aws_s3_bucket.processed_bucket.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST", "HEAD"]
    allowed_origins = ["*"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3600
  }
}

resource "aws_s3_bucket_cors_configuration" "frontend_bucket" {
  bucket = aws_s3_bucket.frontend_bucket.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "OPTIONS", "HEAD"]
    allowed_origins = ["*"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3600
  }
}

resource "aws_s3_bucket_cors_configuration" "raw_bucket" {
  bucket = aws_s3_bucket.raw_bucket.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST", "HEAD"]
    allowed_origins = ["*"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3600
  }
}

resource "aws_s3_bucket_cors_configuration" "thumbnail_bucket" {
  bucket = aws_s3_bucket.thumbnail_bucket.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST", "HEAD"]
    allowed_origins = ["*"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3600
  }
}

resource "aws_s3_bucket_notification" "raw_bucket_notifications" {
  bucket = aws_s3_bucket.raw_bucket.id

  topic {
    events        = ["s3:ObjectCreated:*"]
    topic_arn     = var.raw_upload_events_topic_arn
    filter_suffix = "/original.mp4"
  }
}

data "aws_iam_policy_document" "thumbnail_s3_to_sqs" {
  statement {
    sid    = "AllowS3ToSendThumbnailQueue"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["s3.amazonaws.com"]
    }

    actions   = ["sqs:SendMessage"]
    resources = [var.thumbnail_upload_arn]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_s3_bucket.thumbnail_bucket.arn]
    }
  }
}

resource "aws_sqs_queue_policy" "thumbnail_upload_from_s3" {
  queue_url = var.thumbnail_upload_queue_url
  policy    = data.aws_iam_policy_document.thumbnail_s3_to_sqs.json
}

resource "aws_s3_bucket_notification" "thumbnail_bucket_notifications" {

  bucket = aws_s3_bucket.thumbnail_bucket.id

  queue {
    queue_arn     = var.thumbnail_upload_arn
    events        = ["s3:ObjectCreated:*"]
    filter_suffix = "/custom.png"
  }

  queue {
    queue_arn     = var.thumbnail_upload_arn
    events        = ["s3:ObjectCreated:*"]
    filter_suffix = "/custom.jpg"
  }

  queue {
    queue_arn     = var.thumbnail_upload_arn
    events        = ["s3:ObjectCreated:*"]
    filter_suffix = "/custom.webp"
  }

  depends_on = [aws_sqs_queue_policy.thumbnail_upload_from_s3]
}

resource "aws_s3_bucket_lifecycle_configuration" "processed_bucket" {
  bucket = aws_s3_bucket.processed_bucket.id

  # Transition old files to cheaper classes
  rule {
    id     = "transition-old-files"
    status = var.enable_lifecycle_rules ? "Enabled" : "Disabled"

    filter {
      prefix = ""
    }

    transition {
      days          = 90
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = 180
      storage_class = "GLACIER"
    }
  }
}

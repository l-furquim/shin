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
    allowed_methods = ["GET", "HEAD"]
    allowed_origins = ["*"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3600
  }
}

# resource "aws_s3_bucket_lifecycle_configuration" "processed_bucket" {
#   bucket = aws_s3_bucket.processed_bucket.id

#   # Transition old files to cheaper classes
#   rule {
#     id     = "transition-old-files"
#     status = var.enable_lifecycle_rules ? "Enabled" : "Disabled"

#     transition {
#       days          = 90
#       storage_class = "STANDARD_IA"
#     }

#     transition {
#       days          = 180
#       storage_class = "GLACIER"
#     }
#   }
# }

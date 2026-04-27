locals {
  oidc_url = replace(var.oidc_provider_url, "https://", "")
}

resource "aws_iam_role" "user" {
  name = "shin-${var.env}-irsa-user"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = var.oidc_provider_arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${local.oidc_url}:sub" = "system:serviceaccount:${var.namespace}:user"
          "${local.oidc_url}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "user" {
  name = "shin-${var.env}-irsa-user"
  role = aws_iam_role.user.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3CreatorPictures"
        Effect = "Allow"
        Action = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
        Resource = [
          "${var.creator_pictures_bucket_arn}",
          "${var.creator_pictures_bucket_arn}/*"
        ]
      }
    ]
  })
}

resource "aws_iam_role" "metadata" {
  name = "shin-${var.env}-irsa-metadata"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = var.oidc_provider_arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${local.oidc_url}:sub" = "system:serviceaccount:${var.namespace}:metadata"
          "${local.oidc_url}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "metadata" {
  name = "shin-${var.env}-irsa-metadata"
  role = aws_iam_role.metadata.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3RawRead"
        Effect = "Allow"
        Action = ["s3:GetObject", "s3:ListBucket"]
        Resource = [
          var.raw_bucket_arn,
          "${var.raw_bucket_arn}/*"
        ]
      },
      {
        Sid    = "SQSConsume"
        Effect = "Allow"
        Action = [
          "sqs:ReceiveMessage", "sqs:DeleteMessage",
          "sqs:GetQueueAttributes", "sqs:ChangeMessageVisibility"
        ]
        Resource = [
          for q in ["video-upload-created", "encoding-finished-events", "thumbnail-finished-events",
          "view-events", "encoding-progress"] : lookup(var.sqs_queue_arns, q, "")
        ]
      },
      {
        Sid      = "SNSPublish"
        Effect   = "Allow"
        Action   = ["sns:Publish"]
        Resource = [lookup(var.sns_topic_arns, "video-published", "")]
      }
    ]
  })
}

resource "aws_iam_role" "upload" {
  name = "shin-${var.env}-irsa-upload"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = var.oidc_provider_arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${local.oidc_url}:sub" = "system:serviceaccount:${var.namespace}:upload"
          "${local.oidc_url}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "upload" {
  name = "shin-${var.env}-irsa-upload"
  role = aws_iam_role.upload.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "S3RawWrite"
        Effect   = "Allow"
        Action   = ["s3:PutObject", "s3:GetObject", "s3:GeneratePresignedUrl"]
        Resource = ["${var.raw_bucket_arn}/*"]
      },
      {
        Sid      = "SQSPublish"
        Effect   = "Allow"
        Action   = ["sqs:SendMessage", "sqs:GetQueueUrl"]
        Resource = [lookup(var.sqs_queue_arns, "video-upload-created", "")]
      }
    ]
  })
}

resource "aws_iam_role" "interaction" {
  name = "shin-${var.env}-irsa-interaction"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = var.oidc_provider_arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${local.oidc_url}:sub" = "system:serviceaccount:${var.namespace}:interaction"
          "${local.oidc_url}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "interaction" {
  name = "shin-${var.env}-irsa-interaction"
  role = aws_iam_role.interaction.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "DynamoDB"
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:DeleteItem",
          "dynamodb:UpdateItem", "dynamodb:Query", "dynamodb:Scan"
        ]
        Resource = [
          for t in ["video_reactions", "video_reaction_counters"] :
          lookup(var.dynamodb_table_arns, t, "")
        ]
      },
      {
        Sid    = "SQSConsume"
        Effect = "Allow"
        Action = ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes"]
        Resource = [
          for q in ["like-events", "dislike-events", "video-playback-progress"] :
          lookup(var.sqs_queue_arns, q, "")
        ]
      }
    ]
  })
}

resource "aws_iam_role" "subscription" {
  name = "shin-${var.env}-irsa-subscription"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = var.oidc_provider_arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${local.oidc_url}:sub" = "system:serviceaccount:${var.namespace}:subscription"
          "${local.oidc_url}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "subscription" {
  name = "shin-${var.env}-irsa-subscription"
  role = aws_iam_role.subscription.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "DynamoDB"
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:DeleteItem",
          "dynamodb:UpdateItem", "dynamodb:Query", "dynamodb:Scan"
        ]
        Resource = [lookup(var.dynamodb_table_arns, "subscriptions", "")]
      },
      {
        Sid    = "SQSConsume"
        Effect = "Allow"
        Action = ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes"]
        Resource = [
          for q in ["channel-subscribed", "channel-unsubscribed"] :
          lookup(var.sqs_queue_arns, q, "")
        ]
      }
    ]
  })
}

resource "aws_iam_role" "comment" {
  name = "shin-${var.env}-irsa-comment"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = var.oidc_provider_arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${local.oidc_url}:sub" = "system:serviceaccount:${var.namespace}:comment"
          "${local.oidc_url}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "comment" {
  name = "shin-${var.env}-irsa-comment"
  role = aws_iam_role.comment.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "DynamoDB"
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:DeleteItem",
          "dynamodb:UpdateItem", "dynamodb:Query", "dynamodb:Scan"
        ]
        Resource = [
          for t in ["comments", "comment_threads"] :
          lookup(var.dynamodb_table_arns, t, "")
        ]
      },
      {
        Sid    = "SQSPublish"
        Effect = "Allow"
        Action = ["sqs:SendMessage", "sqs:GetQueueUrl"]
        Resource = [
          for q in ["thread-created", "comment-reply-created", "comment-deleted"] :
          lookup(var.sqs_queue_arns, q, "")
        ]
      }
    ]
  })
}

resource "aws_iam_role" "search" {
  name = "shin-${var.env}-irsa-search"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = var.oidc_provider_arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${local.oidc_url}:sub" = "system:serviceaccount:${var.namespace}:search"
          "${local.oidc_url}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "search" {
  name = "shin-${var.env}-irsa-search"
  role = aws_iam_role.search.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "SQSConsume"
        Effect   = "Allow"
        Action   = ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes"]
        Resource = [lookup(var.sqs_queue_arns, "video-published-opensearch-indexer", "")]
      },
      {
        Sid      = "OpenSearch"
        Effect   = "Allow"
        Action   = ["aoss:APIAccessAll"]
        Resource = [var.opensearch_collection_arn]
      }
    ]
  })
}

resource "aws_iam_role" "streaming" {
  name = "shin-${var.env}-irsa-streaming"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = var.oidc_provider_arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${local.oidc_url}:sub" = "system:serviceaccount:${var.namespace}:streaming"
          "${local.oidc_url}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "streaming" {
  name = "shin-${var.env}-irsa-streaming"
  role = aws_iam_role.streaming.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "SecretsManager"
        Effect   = "Allow"
        Action   = ["secretsmanager:GetSecretValue"]
        Resource = [var.cloudfront_private_key_secret_arn]
      },
      {
        Sid      = "SQSPublish"
        Effect   = "Allow"
        Action   = ["sqs:SendMessage", "sqs:GetQueueUrl"]
        Resource = [lookup(var.sqs_queue_arns, "view-events", "")]
      },
      {
        Sid    = "DynamoDB"
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:DeleteItem",
          "dynamodb:UpdateItem", "dynamodb:Query"
        ]
        Resource = [lookup(var.dynamodb_table_arns, "playback_sessions", "")]
      }
    ]
  })
}

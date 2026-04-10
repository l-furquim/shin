terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Environment = var.env
    }
  }
}

module "s3" {
  source = "./modules/s3"

  env                          = var.env
  raw_bucket_name              = var.raw_bucket_name
  processed_bucket_name        = var.processed_bucket_name
  thumbnail_bucket_name        = var.thumbnail_bucket_name
  creator_pictures_bucket_name = var.creator_pictures_bucket_name
  raw_upload_events_topic_arn  = module.sns.topic_arns["raw-upload-created"]

  depends_on = [module.sns]
}

module "sns" {
  source = "./modules/sns"

  env         = var.env
  topic_names = var.sns_topic_names
}

module "sqs" {
  source = "./modules/sqs"

  env         = var.env
  queue_names = var.sqs_queue_names
}

locals {
  subscriptions = {
    for pair in flatten([
      for topic_name, queue_names in var.sns_fanout_subscriptions : [
        for queue_name in queue_names : {
          key        = "${topic_name}-to-${queue_name}"
          topic_name = topic_name
          queue_name = queue_name
        }
      ]
      ]) : pair.key => {
      topic_arn  = module.sns.topic_arns[pair.topic_name]
      queue_name = pair.queue_name
      queue_arn  = module.sqs.queue_arns[pair.queue_name]
      queue_url  = module.sqs.queue_urls[pair.queue_name]
    }
  }

  sns_fanout_bindings = {
    for topic_name, queue_names in var.sns_fanout_subscriptions : topic_name => {
      topic_arn = module.sns.topic_arns[topic_name]
      queues = {
        for queue_name in queue_names : queue_name => {
          arn = module.sqs.queue_arns[queue_name]
          url = module.sqs.queue_urls[queue_name]
        }
      }
    }
  }
}

data "aws_iam_policy_document" "s3_to_sns" {
  statement {
    sid    = "AllowS3Publish"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["s3.amazonaws.com"]
    }

    actions   = ["sns:Publish"]
    resources = [module.sns.topic_arns["raw-upload-created"]]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [module.s3.raw_bucket_arn]
    }
  }
}

resource "aws_sns_topic_policy" "raw_upload_events" {
  arn    = module.sns.topic_arns["raw-upload-created"]
  policy = data.aws_iam_policy_document.s3_to_sns.json
}

module "cloudfront" {
  source = "./modules/cloudfront"

  env = var.env

  processed_s3_id          = module.s3.processed_bucket_id
  processed_s3_arn         = module.s3.processed_bucket_arn
  processed_s3_domain_name = module.s3.processed_bucket_domain_name

  thumbnail_s3_id          = module.s3.thumbnail_bucket_id
  thumbnail_s3_arn         = module.s3.thumbnail_bucket_arn
  thumbnail_s3_domain_name = module.s3.thumbnail_bucket_domain_name

  creators_s3_id          = module.s3.creator_pictures_bucket_id
  creators_s3_arn         = module.s3.creator_pictures_bucket_arn
  creators_s3_domain_name = module.s3.creator_pictures_bucket_domain_name
}

module "subscriptions" {
  source = "./modules/subscriptions"

  subscriptions = local.subscriptions
}

module "secrets" {
  source = "./modules/secrets"

  env                    = var.env
  cloudfront_private_key = file("${path.module}/modules/cloudfront/cloudfront_private_key_pkcs8.pem")
}

module "dynamodb" {
  source = "./modules/dynamodb"

  env = var.env
}

module "engagement" {
  source = "./modules/engagement"

  env                         = var.env
  view_events_queue_arn       = module.sqs.queue_arns["view-events"]
  view_events_queue_url       = module.sqs.queue_urls["view-events"]
  playback_progress_queue_arn = module.sqs.queue_arns["video-playback-progress"]
  playback_sessions_table_arn = module.dynamodb.table_arns["playback_sessions"]
  processor_zip               = "${path.root}/../lambdas/engagement/processor/bootstrap.zip"
  processor_hash              = filebase64sha256("${path.root}/../lambdas/engagement/processor/bootstrap.zip")
}

module "thumbnail" {
  source = "./modules/thumbnail"

  env                          = var.env
  thumbnail_job_queue_arn      = module.sqs.queue_arns["thumbnail-job"]
  thumbnail_finished_queue_arn = module.sqs.queue_arns["thumbnail-finished-events"]
  thumbnail_finished_queue_url = module.sqs.queue_urls["thumbnail-finished-events"]
  raw_bucket_arn               = module.s3.raw_bucket_arn
  raw_bucket_name              = var.raw_bucket_name
  thumbnail_bucket_arn         = module.s3.thumbnail_bucket_arn
  thumbnail_bucket_name        = var.thumbnail_bucket_name
  processor_zip                = "${path.root}/../lambdas/thumbnail/bootstrap.zip"
  processor_hash               = filebase64sha256("${path.root}/../lambdas/thumbnail/bootstrap.zip")
  ffmpeg_layer_zip             = "${path.root}/../lambdas/thumbnail/ffmpeg-layer/ffmpeg-layer.zip"
  ffmpeg_layer_hash            = filebase64sha256("${path.root}/../lambdas/thumbnail/ffmpeg-layer/ffmpeg-layer.zip")
}

module "open_search" {
  source = "./modules/open-search"

  env = var.env

}

resource "aws_cloudwatch_event_rule" "view_events" {
  count = var.enable_view_eventbridge_pipeline ? 1 : 0

  name        = "shin-${var.env}-view-events"
  description = "Routes qualified S3 GetObject events for views"

  event_pattern = jsonencode({
    source        = ["aws.s3"],
    "detail-type" = ["AWS API Call via CloudTrail"],
    detail = {
      eventSource = ["s3.amazonaws.com"],
      eventName   = ["GetObject"],
      requestParameters = {
        bucketName = [module.s3.processed_bucket_id]
      }
    }
  })
}

resource "aws_cloudwatch_event_target" "view_events_to_sqs" {
  count = var.enable_view_eventbridge_pipeline ? 1 : 0

  rule      = aws_cloudwatch_event_rule.view_events[0].name
  target_id = "view-events-sqs"
  arn       = module.sqs.queue_arns["view-events"]
}

data "aws_iam_policy_document" "view_events_sqs_policy" {
  statement {
    sid    = "AllowEventBridgeSendMessage"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["events.amazonaws.com"]
    }

    actions   = ["sqs:SendMessage"]
    resources = [module.sqs.queue_arns["view-events"]]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = var.enable_view_eventbridge_pipeline ? [aws_cloudwatch_event_rule.view_events[0].arn] : []
    }
  }
}

resource "aws_sqs_queue_policy" "view_events" {
  count = var.enable_view_eventbridge_pipeline ? 1 : 0

  queue_url = module.sqs.queue_urls["view-events"]
  policy    = data.aws_iam_policy_document.view_events_sqs_policy.json
}

resource "aws_cloudtrail" "view_events" {
  count = var.enable_view_eventbridge_pipeline ? 1 : 0

  name                          = "shin-${var.env}-view-events"
  s3_bucket_name                = module.s3.raw_bucket_id
  include_global_service_events = false
  is_multi_region_trail         = false
  enable_logging                = true

  event_selector {
    read_write_type           = "ReadOnly"
    include_management_events = false

    data_resource {
      type   = "AWS::S3::Object"
      values = ["${module.s3.processed_bucket_arn}/"]
    }
  }

  depends_on = [aws_s3_bucket_policy.cloudtrail_logs]
}

data "aws_iam_policy_document" "cloudtrail_logs_bucket_policy" {
  count = var.enable_view_eventbridge_pipeline ? 1 : 0

  statement {
    sid    = "AWSCloudTrailAclCheck"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["cloudtrail.amazonaws.com"]
    }

    actions   = ["s3:GetBucketAcl"]
    resources = [module.s3.raw_bucket_arn]
  }

  statement {
    sid    = "AWSCloudTrailWrite"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["cloudtrail.amazonaws.com"]
    }

    actions = ["s3:PutObject"]
    resources = [
      "${module.s3.raw_bucket_arn}/AWSLogs/*"
    ]

    condition {
      test     = "StringEquals"
      variable = "s3:x-amz-acl"
      values   = ["bucket-owner-full-control"]
    }
  }
}

resource "aws_s3_bucket_policy" "cloudtrail_logs" {
  count = var.enable_view_eventbridge_pipeline ? 1 : 0

  bucket = module.s3.raw_bucket_id
  policy = data.aws_iam_policy_document.cloudtrail_logs_bucket_policy[0].json
}

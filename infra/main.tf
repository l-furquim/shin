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

module "cloudfront" {
  source = "./modules/cloudfront"

  s3_id          = module.s3.processed_bucket_id
  s3_arn         = module.s3.processed_bucket_arn
  s3_domain_name = module.s3.processed_bucket_domain_name
  env            = var.env
}

module "subscriptions" {
  source = "./modules/subscriptions"

  subscriptions = {
    "metadata-view-counted" = {
      topic_arn = module.sns.topic_arns["view-counted"]
      queue_arn = module.sqs.queue_arns["view-events"]
      queue_url = module.sqs.queue_urls["view-events"]
    }
  }
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

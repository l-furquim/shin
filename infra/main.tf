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

  env                   = var.env
  raw_bucket_name       = var.raw_bucket_name
  processed_bucket_name = var.processed_bucket_name
  thumbnail_bucket_name = var.thumbnail_bucket_name
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
    "metadata-encode-finished" = {
      topic_arn = module.sns.topic_arns["encode-finished"]
      queue_arn = module.sqs.queue_arns["metadata-events"]
      queue_url = module.sqs.queue_urls["metadata-events"]
    }
    "metadata-thumbnail-generated" = {
      topic_arn = module.sns.topic_arns["thumbnail-generated"]
      queue_arn = module.sqs.queue_arns["metadata-events"]
      queue_url = module.sqs.queue_urls["metadata-events"]
    }
  }
}

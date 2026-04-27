terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.95.0, < 6.0.0"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.12"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.25"
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

data "aws_eks_cluster_auth" "this" {
  name = module.eks.cluster_name
}

provider "helm" {
  kubernetes {
    host                   = module.eks.cluster_endpoint
    cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)
    token                  = data.aws_eks_cluster_auth.this.token
  }
}

provider "kubernetes" {
  host                   = module.eks.cluster_endpoint
  cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)
  token                  = data.aws_eks_cluster_auth.this.token
}

module "s3" {
  source = "./modules/s3"

  env                          = var.env
  raw_bucket_name              = var.raw_bucket_name
  processed_bucket_name        = var.processed_bucket_name
  thumbnail_bucket_name        = var.thumbnail_bucket_name
  creator_pictures_bucket_name = var.creator_pictures_bucket_name
  raw_upload_events_topic_arn  = module.sns.topic_arns["raw-upload-created"]
  thumbnail_upload_arn         = module.sqs.queue_arns["thumbnail-upload"]
  thumbnail_upload_queue_url   = module.sqs.queue_urls["thumbnail-upload"]

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
  thumbnail_upload_queue_arn   = module.sqs.queue_arns["thumbnail-upload"]
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

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "shin-${var.env}"
  cidr = var.vpc_cidr

  azs             = slice(data.aws_availability_zones.available.names, 0, 3)
  private_subnets = [for i in range(3) : cidrsubnet(var.vpc_cidr, 4, i)]
  public_subnets  = [for i in range(3) : cidrsubnet(var.vpc_cidr, 4, i + 4)]
  intra_subnets   = [for i in range(3) : cidrsubnet(var.vpc_cidr, 4, i + 8)]

  enable_nat_gateway   = true
  single_nat_gateway   = true
  enable_dns_hostnames = true

  public_subnet_tags = {
    "kubernetes.io/role/elb"                = 1
    "kubernetes.io/cluster/shin-${var.env}" = "shared"
  }

  private_subnet_tags = {
    "kubernetes.io/role/internal-elb"       = 1
    "kubernetes.io/cluster/shin-${var.env}" = "shared"
  }

  tags = {
    Terraform   = "true"
    Environment = var.env
  }
}

data "aws_availability_zones" "available" {
  state = "available"
}

module "eks" {
  source = "./modules/eks"

  env                            = var.env
  vpc_id                         = module.vpc.vpc_id
  private_subnet_ids             = module.vpc.private_subnets
  intra_subnet_ids               = module.vpc.intra_subnets
  eks_cluster_version            = var.eks_cluster_version
  node_group_apps_instance_types = var.node_group_apps_instance_types
  node_group_apps_min_size       = var.node_group_apps_min_size
  node_group_apps_max_size       = var.node_group_apps_max_size
  node_group_apps_desired_size   = var.node_group_apps_desired_size
  node_group_spot_instance_types = var.node_group_spot_instance_types
  node_group_spot_min_size       = var.node_group_spot_min_size
  node_group_spot_max_size       = var.node_group_spot_max_size
  node_group_spot_desired_size   = var.node_group_spot_desired_size
}

module "ecr" {
  source = "./modules/ecr"

  env              = var.env
  repository_names = var.ecr_repository_names
}

module "eks_irsa" {
  source = "./modules/eks-irsa"

  env               = var.env
  namespace         = "shin-${var.env}"
  oidc_provider_arn = module.eks.oidc_provider_arn
  oidc_provider_url = module.eks.oidc_provider_url

  raw_bucket_arn                    = module.s3.raw_bucket_arn
  creator_pictures_bucket_arn       = module.s3.creator_pictures_bucket_arn
  sqs_queue_arns                    = module.sqs.queue_arns
  sns_topic_arns                    = module.sns.topic_arns
  dynamodb_table_arns               = module.dynamodb.table_arns
  cloudfront_private_key_secret_arn = module.secrets.cloudfront_private_key_secret_arn
  opensearch_collection_arn         = module.open_search.open_search_collection_arn
}

resource "helm_release" "metrics_server" {
  name       = "metrics-server"
  repository = "https://kubernetes-sigs.github.io/metrics-server/"
  chart      = "metrics-server"
  version    = "3.12.1"
  namespace  = "kube-system"

  set {
    name  = "args[0]"
    value = "--kubelet-insecure-tls"
  }

  depends_on = [module.eks]
}

resource "helm_release" "aws_load_balancer_controller" {
  name       = "aws-load-balancer-controller"
  repository = "https://aws.github.io/eks-charts"
  chart      = "aws-load-balancer-controller"
  namespace  = "kube-system"

  set {
    name  = "clusterName"
    value = module.eks.cluster_name
  }

  set {
    name  = "serviceAccount.annotations.eks\\.amazonaws\\.com/role-arn"
    value = module.lbc_irsa.iam_role_arn
  }

  depends_on = [module.eks]
}

module "lbc_irsa" {
  source = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"

  version = "~> 5.0"

  role_name                              = "shin-${var.env}-lbc"
  attach_load_balancer_controller_policy = true

  oidc_providers = {
    main = {
      provider_arn               = module.eks.oidc_provider_arn
      namespace_service_accounts = ["kube-system:aws-load-balancer-controller"]
    }
  }
}

resource "aws_secretsmanager_secret" "argocd_admin" {
  name                    = "shin/${var.env}/argocd-admin-password"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "argocd_admin" {
  secret_id     = aws_secretsmanager_secret.argocd_admin.id
  secret_string = var.argocdAdminPassword
}

resource "helm_release" "argocd" {
  name             = "argocd"
  repository       = "https://argoproj.github.io/argo-helm"
  chart            = "argo-cd"
  namespace        = "argocd"
  create_namespace = true

  set {
    name  = "configs.secret.argocdServerAdminPassword"
    value = bcrypt(var.argocdAdminPassword)
  }

  set {
    name  = "server.service.type"
    value = "ClusterIP"
  }

  depends_on = [module.eks]
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

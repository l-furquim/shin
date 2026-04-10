resource "aws_sns_topic_subscription" "subscriptions" {
  for_each = var.subscriptions

  topic_arn = each.value.topic_arn
  protocol  = "sqs"
  endpoint  = each.value.queue_arn

  raw_message_delivery = false
}

locals {
  subscriptions_by_queue = {
    for queue_name in distinct([for _, s in var.subscriptions : s.queue_name]) : queue_name => {
      queue_arn  = [for _, s in var.subscriptions : s.queue_arn if s.queue_name == queue_name][0]
      queue_url  = [for _, s in var.subscriptions : s.queue_url if s.queue_name == queue_name][0]
      topic_arns = distinct([for _, s in var.subscriptions : s.topic_arn if s.queue_name == queue_name])
    }
  }
}

resource "aws_sqs_queue_policy" "subscription_policies" {
  for_each = local.subscriptions_by_queue

  queue_url = each.value.queue_url
  policy    = data.aws_iam_policy_document.subscription_policy[each.key].json
}

data "aws_iam_policy_document" "subscription_policy" {
  for_each = local.subscriptions_by_queue

  statement {
    sid    = "AllowSNSPublish"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }

    actions = [
      "sqs:SendMessage"
    ]

    resources = [
      each.value.queue_arn
    ]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = each.value.topic_arns
    }
  }
}

resource "aws_sns_topic_subscription" "subscriptions" {
  for_each = var.subscriptions

  topic_arn = each.value.topic_arn
  protocol  = "sqs"
  endpoint  = each.value.queue_arn

  raw_message_delivery = false
}

resource "aws_sqs_queue_policy" "subscription_policies" {
  for_each = var.subscriptions

  queue_url = each.value.queue_url
  policy    = data.aws_iam_policy_document.subscription_policy[each.key].json
}

data "aws_iam_policy_document" "subscription_policy" {
  for_each = var.subscriptions

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
      values   = [each.value.topic_arn]
    }
  }
}

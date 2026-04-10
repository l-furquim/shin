data "aws_iam_policy_document" "processor" {
  statement {
    actions   = ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes"]
    resources = [var.playback_progress_queue_arn]
  }
  statement {
    actions   = ["dynamodb:UpdateItem", "dynamodb:GetItem"]
    resources = [var.playback_sessions_table_arn]
  }
  statement {
    actions   = ["sqs:SendMessage"]
    resources = [var.view_events_queue_arn]
  }
}

resource "aws_iam_policy" "processor" {
  name   = "shin-${var.env}-engagement-processor-policy"
  policy = data.aws_iam_policy_document.processor.json
}

module "lambda_processor" {
  source                              = "../lambda"
  env                                 = var.env
  function_name                       = "engagement-processor"
  description                         = "Aggregates playback progress into DynamoDB and publishes valid view events"
  filename                            = var.processor_zip
  source_hash                         = var.processor_hash
  timeout                             = 60
  sqs_trigger_arn                     = var.playback_progress_queue_arn
  sqs_batch_size                      = 25
  sqs_maximum_batching_window_seconds = 1
  environment_variables = {
    TABLE_NAME            = "playback_sessions"
    VIEW_EVENTS_QUEUE_URL = var.view_events_queue_url
    ENV                   = var.env
  }
  extra_policy_arns = [
    aws_iam_policy.processor.arn,
    "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole"
  ]
}

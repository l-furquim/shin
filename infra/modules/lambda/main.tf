data "aws_iam_policy_document" "lambda_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "lambda" {
  name               = "shin-${var.env}-${var.function_name}-role"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume.json
}

resource "aws_iam_role_policy_attachment" "basic_execution" {
  role       = aws_iam_role.lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "extra" {
  for_each   = { for idx, arn in var.extra_policy_arns : tostring(idx) => arn }
  role       = aws_iam_role.lambda.name
  policy_arn = each.value
}

resource "aws_lambda_function" "this" {
  function_name    = "shin-${var.env}-${var.function_name}"
  description      = var.description
  role             = aws_iam_role.lambda.arn
  runtime          = var.runtime
  handler          = var.handler
  memory_size      = var.memory_size
  timeout          = var.timeout
  filename         = var.filename
  source_code_hash = var.source_hash
  layers           = var.layers

  environment {
    variables = var.environment_variables
  }

  lifecycle {
    ignore_changes = [filename, source_code_hash]
  }
}

resource "aws_lambda_event_source_mapping" "sqs" {
  count            = var.sqs_trigger_arn != null ? 1 : 0
  event_source_arn = var.sqs_trigger_arn
  function_name    = aws_lambda_function.this.arn
  batch_size       = var.sqs_batch_size

  maximum_batching_window_in_seconds = var.sqs_maximum_batching_window_seconds
}

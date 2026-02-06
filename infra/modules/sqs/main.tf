resource "aws_sqs_queue" "queues" {
  for_each = toset(var.queue_names)

  name                       = "shin-${var.env}-${each.key}"
  message_retention_seconds  = var.message_retention_seconds
  visibility_timeout_seconds = var.visibility_timeout_seconds
  receive_wait_time_seconds  = var.receive_wait_time_seconds
  delay_seconds              = var.delay_seconds

  tags = {
    Name        = "shin-${var.env}-${each.key}"
    Environment = var.env
  }
}

resource "aws_sqs_queue" "dlq" {
  for_each = var.enable_dlq ? toset(var.queue_names) : []

  name                      = "shin-${var.env}-${each.key}-dlq"
  message_retention_seconds = var.dlq_message_retention_seconds

  tags = {
    Name        = "shin-${var.env}-${each.key}-dlq"
    Environment = var.env
    Type        = "DLQ"
  }
}

resource "aws_sqs_queue_redrive_policy" "queues" {
  for_each = var.enable_dlq ? toset(var.queue_names) : []

  queue_url = aws_sqs_queue.queues[each.key].id
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq[each.key].arn
    maxReceiveCount     = var.max_receive_count
  })
}

resource "aws_sqs_queue_redrive_allow_policy" "dlq" {
  for_each = var.enable_dlq ? toset(var.queue_names) : []

  queue_url = aws_sqs_queue.dlq[each.key].id

  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue",
    sourceQueueArns   = [aws_sqs_queue.queues[each.key].arn]
  })
}

resource "aws_sns_topic" "topics" {
  for_each = toset(var.topic_names)

  name = "shin-${var.env}-${each.key}"

  tags = {
    Name        = "shin-${var.env}-${each.key}"
    Environment = var.env
  }
}

resource "aws_sns_topic_subscription" "dead_letter_queue" {
  for_each = var.enable_dlq ? toset(var.topic_names) : []

  topic_arn = aws_sns_topic.topics[each.key].arn
  protocol  = "sqs"
  endpoint  = var.dlq_arn
}

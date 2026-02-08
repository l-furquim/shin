variable "subscriptions" {
  description = "Map of SNS to SQS subscriptions"
  type = map(object({
    topic_arn = string
    queue_arn = string
    queue_url = string
  }))
}

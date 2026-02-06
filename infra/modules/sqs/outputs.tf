output "queue_arns" {
  description = "Map of SQS queue names to their ARNs"
  value = {
    for name, queue in aws_sqs_queue.queues : name => queue.arn
  }
}

output "queue_urls" {
  description = "Map of SQS queue names to their URLs"
  value = {
    for name, queue in aws_sqs_queue.queues : name => queue.url
  }
}

output "queue_ids" {
  description = "Map of SQS queue names to their IDs"
  value = {
    for name, queue in aws_sqs_queue.queues : name => queue.id
  }
}

output "dlq_arns" {
  description = "Map of DLQ names to their ARNs"
  value = {
    for name, queue in aws_sqs_queue.dlq : name => queue.arn
  }
}

output "dlq_urls" {
  description = "Map of DLQ names to their URLs"
  value = {
    for name, queue in aws_sqs_queue.dlq : name => queue.url
  }
}

output "queue_names" {
  description = "List of created SQS queue names"
  value       = [for queue in aws_sqs_queue.queues : queue.name]
}

output "queues" {
  description = "Complete SQS queue objects"
  value       = aws_sqs_queue.queues
}

output "topic_arns" {
  description = "Map of SNS topic names to their ARNs"
  value = {
    for name, topic in aws_sns_topic.topics : name => topic.arn
  }
}

output "topic_ids" {
  description = "Map of SNS topic names to their IDs"
  value = {
    for name, topic in aws_sns_topic.topics : name => topic.id
  }
}

output "topic_names" {
  description = "List of created SNS topic names"
  value       = [for topic in aws_sns_topic.topics : topic.name]
}

output "topics" {
  description = "Complete SNS topic objects"
  value       = aws_sns_topic.topics
}

output "table_arns" {
  description = "Map of DynamoDB table names to their ARNs"
  value = {
    "video_reactions"               = aws_dynamodb_table.video_reactions.arn
    "video_reaction_counters"       = aws_dynamodb_table.video_reaction_counters.arn
    "playback_sessions"             = aws_dynamodb_table.playback_sessions.arn
    "channel_subscriptions"         = aws_dynamodb_table.channel_subscriptions.arn
    "user_subscriptions"            = aws_dynamodb_table.user_subscriptions.arn
    "channel_subscription_counters" = aws_dynamodb_table.channel_subscription_counters.arn
    "comments"                      = aws_dynamodb_table.comments.arn
    "comment_threads"               = aws_dynamodb_table.comment_threads.arn
  }
}

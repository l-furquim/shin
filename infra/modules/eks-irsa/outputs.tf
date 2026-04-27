output "role_arns" {
  description = "Map of service name to IRSA role ARN"
  value = {
    user         = aws_iam_role.user.arn
    metadata     = aws_iam_role.metadata.arn
    upload       = aws_iam_role.upload.arn
    interaction  = aws_iam_role.interaction.arn
    subscription = aws_iam_role.subscription.arn
    comment      = aws_iam_role.comment.arn
    search       = aws_iam_role.search.arn
    streaming    = aws_iam_role.streaming.arn
  }
}

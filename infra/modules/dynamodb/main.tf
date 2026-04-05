resource "aws_dynamodb_table" "video_metrics_daily" {
  name         = "video_metrics_daily"
  billing_mode = var.billing_mode

  hash_key  = "videoId"
  range_key = "date"

  attribute {
    name = "videoId"
    type = "S"
  }

  attribute {
    name = "date"
    type = "S"
  }

  tags = {
    Env = var.env
  }
}

resource "aws_dynamodb_table" "channel_metrics_daily" {
  name         = "channel_metrics_daily"
  billing_mode = var.billing_mode

  hash_key  = "channelId"
  range_key = "date"

  attribute {
    name = "channelId"
    type = "S"
  }

  attribute {
    name = "date"
    type = "S"
  }

  tags = {
    Env = var.env
  }
}

resource "aws_dynamodb_table" "video_reactions" {
  name         = "video_reactions"
  billing_mode = var.billing_mode

  hash_key  = "videoId"
  range_key = "userId"

  attribute {
    name = "videoId"
    type = "S"
  }

  attribute {
    name = "userId"
    type = "S"
  }

  attribute {
    name = "createdAt"
    type = "S"
  }

  global_secondary_index {
    name            = "userIdIndex"
    hash_key        = "userId"
    range_key       = "createdAt"
    projection_type = "ALL"
  }

  tags = {
    Env = var.env
  }
}

resource "aws_dynamodb_table" "video_reaction_counters" {
  name         = "video_reaction_counters"
  billing_mode = var.billing_mode

  hash_key = "videoId"

  attribute {
    name = "videoId"
    type = "S"
  }

  tags = {
    Env = var.env
  }
}

resource "aws_dynamodb_table" "playback_sessions" {
  name         = "playback_sessions"
  billing_mode = var.billing_mode

  hash_key = "sessionId"

  attribute {
    name = "sessionId"
    type = "S"
  }

  tags = {
    Env = var.env
  }
}

resource "aws_dynamodb_table" "channel_subscriptions" {
  name         = "channel_subscriptions"
  billing_mode = var.billing_mode

  hash_key  = "channelId"
  range_key = "userId"

  attribute {
    name = "channelId"
    type = "S"
  }

  attribute {
    name = "userId"
    type = "S"
  }

  tags = {
    Env = var.env
  }
}

resource "aws_dynamodb_table" "user_subscriptions" {
  name         = "user_subscriptions"
  billing_mode = var.billing_mode

  hash_key  = "userId"
  range_key = "channelId"

  attribute {
    name = "userId"
    type = "S"
  }

  attribute {
    name = "channelId"
    type = "S"
  }

  tags = {
    Env = var.env
  }
}

resource "aws_dynamodb_table" "channel_subscription_counters" {
  name         = "channel_subscription_counters"
  billing_mode = var.billing_mode

  hash_key = "channelId"

  attribute {
    name = "channelId"
    type = "S"
  }

  tags = {
    Env = var.env
  }
}

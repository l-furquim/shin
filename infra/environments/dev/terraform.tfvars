env    = "dev"
region = "us-east-1"

raw_bucket_name              = "shin-dev-raw-uploads"
processed_bucket_name        = "shin-dev-processed-videos"
thumbnail_bucket_name        = "shin-dev-thumbnails"
creator_pictures_bucket_name = "shin-dev-creator-pictures"

sns_topic_names = [
  "raw-upload-created",
  "video-published"
]

sns_fanout_subscriptions = {
  "raw-upload-created" = [
    "decode-job",
    "thumbnail-job",
    "raw-upload-metadata-queue"
  ]

  "video-published" = [
    "video-video-published-opensearch-indexer",
    "video-video-published-notification-service"
  ]
}

sqs_queue_names = [
  "decode-job",
  "thumbnail-job",
  "raw-upload-metadata-queue",
  "encoding-finished-events",
  "thumbnail-finished-events",
  "view-events",
  "video-initialized",
  "like-events",
  "dislike-events",
  "channel-subscribed",
  "channel-unsubscribed",
  "thread-created",
  "comment-reply-created",
  "comment-updated",
  "comment-deleted",
  "video-playback-progress",
  "video-video-published-opensearch-indexer",
  "video-video-published-notification-service",
  "video-updated"
]

jwt_secret = "shin-dev-secret"

enable_view_eventbridge_pipeline = false

sqs_message_retention_seconds  = 345600 # 4 days
sqs_visibility_timeout_seconds = 300    # 5 minutes
# sqs_receive_wait_time_seconds  = 20    # Use this only in prod

common_tags = {
  Owner      = "DevTeam"
  CostCenter = "Engineering"
  Terraform  = "true"
}

env    = "dev"
region = "us-east-1"

raw_bucket_name              = "shin-dev-raw-uploads"
processed_bucket_name        = "shin-dev-processed-videos"
thumbnail_bucket_name        = "shin-dev-thumbnails"
creator_pictures_bucket_name = "shin-dev-creator-pictures"

sns_topic_names = [
  "raw-upload-created",
  "video-published",
  "thread-created",
  "comment-reply",
]

sns_fanout_subscriptions = {
  "raw-upload-created" = [
    "decode-job",
    "thumbnail-job"
  ]


  "comment-reply" : [
    "comment-reply-metadata",
    "comment-reply-notification"
  ]

  "thread-created" : [
    "thread-created-metadata",
    "thread-created-notification"
  ]

  "video-published" = [
    "video-published-opensearch-indexer",
    "video-published-notification"
  ]
}

sqs_queue_names = [
  "decode-job",
  "thumbnail-job",
  "thumbnail-upload",
  "video-upload-created",
  "encoding-finished-events",
  "thumbnail-finished-events",
  "view-events",
  "like-events",
  "dislike-events",
  "channel-subscribed",
  "channel-unsubscribed",
  "thread-created",
  "comment-reply-created",
  "comment-updated",
  "comment-deleted",
  "video-playback-progress",
  "video-published-opensearch-indexer",
  "video-published-notification",
  "thread-created-metadata",
  "thread-created-notification",
  "comment-reply-metadata",
  "comment-reply-notification",
  "video-updated-events",
  "encoding-progress",
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

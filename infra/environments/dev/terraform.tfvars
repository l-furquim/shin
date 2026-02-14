env    = "dev"
region = "us-east-1"

raw_bucket_name              = "shin-dev-raw-uploads"
processed_bucket_name        = "shin-dev-processed-videos"
thumbnail_bucket_name        = "shin-dev-thumbnails"
creator_pictures_bucket_name = "shin-dev-creator-pictures"

sns_topic_names = [
  "chunk-processed",
  "thumbnail-generated",
  "encode-started",
  "encode-finished",
]

sqs_queue_names = [
  "decode-job",
  "thumbnail-job",
  "metadata-events",
]

sqs_message_retention_seconds  = 345600 # 4 days
sqs_visibility_timeout_seconds = 300    # 5 minutes
# sqs_receive_wait_time_seconds  = 20    # Use this only in prod

common_tags = {
  Owner      = "DevTeam"
  CostCenter = "Engineering"
  Terraform  = "true"
}

#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TERRAFORM_PATH="$PROJECT_ROOT/infra"

cd "$PROJECT_ROOT"

echo "Initializing development environment..."

echo "Tearing down existing dev environment"
docker compose down -v
terraform -chdir=$TERRAFORM_PATH destroy -var-file="environments/dev/terraform.tfvars" -auto-approve

echo "Recreating dev environment"
terraform -chdir=$TERRAFORM_PATH init

set +e
APPLY_OUTPUT=$(terraform -chdir=$TERRAFORM_PATH apply -var-file="environments/dev/terraform.tfvars" -auto-approve 2>&1)
APPLY_STATUS=$?
set -e

printf "%s\n" "$APPLY_OUTPUT"

if [ $APPLY_STATUS -ne 0 ]; then
  if printf "%s" "$APPLY_OUTPUT" | grep -q "OriginAccessControlAlreadyExists"; then
    echo "CloudFront OAC already exists. Reapplying without cloudfront resources..."
    terraform -chdir=$TERRAFORM_PATH apply \
      -var-file="environments/dev/terraform.tfvars" \
      -target=module.s3 \
      -target=module.sns \
      -target=module.sqs \
      -target=module.subscriptions \
      -auto-approve
  else
    exit $APPLY_STATUS
  fi
fi

ENCODING_FINISHED_EVENTS_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."encoding-finished-events"')
THUMBNAIL_FINISHED_EVENTS_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."thumbnail-finished-events"')

echo "Exporting Terraform outputs..."

export RAW_BUCKET_NAME=$(terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."raw"')
export PROCESSED_BUCKET_NAME=$(terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."processed"')
export THUMBNAIL_BUCKET_NAME=$(terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."thumbnail"')
export CREATOR_PICTURES_BUCKET_NAME=$(terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."creator-pictures"')

export DECODE_JOB_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."decode-job"')
export THUMBNAIL_JOB_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."thumbnail-job"')
export ENCODING_FINISHED_EVENTS_QUEUE_URL=$ENCODING_FINISHED_EVENTS_QUEUE_URL
export THUMBNAIL_FINISHED_EVENTS_QUEUE_URL=$THUMBNAIL_FINISHED_EVENTS_QUEUE_URL
export RAW_UPLOAD_METADATA_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."raw-upload-metadata-queue"')
export ENCODING_FINISHED_EVENTS_QUEUE_NAME=$(basename "$ENCODING_FINISHED_EVENTS_QUEUE_URL")
export THUMBNAIL_FINISHED_EVENTS_QUEUE_NAME=$(basename "$THUMBNAIL_FINISHED_EVENTS_QUEUE_URL")
export RAW_UPLOAD_METADATA_QUEUE_NAME=$(basename "$RAW_UPLOAD_METADATA_QUEUE_URL")
export VIDEO_INITIALIZED_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."video-initialized"')
export LIKE_EVENTS_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."like-events"')
export DISLIKE_EVENTS_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."dislike-events"')
export CHANNEL_SUBSCRIBED_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."channel-subscribed"')
export CHANNEL_UNSUBSCRIBED_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."channel-unsubscribed"')
export VIEW_EVENTS_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."view-events"')
export DECODE_JOB_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."decode-job"')
export THUMBNAIL_JOB_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."thumbnail-job"')
export ENCODING_FINISHED_EVENTS_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."encoding-finished-events"')
export THUMBNAIL_FINISHED_EVENTS_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."thumbnail-finished-events"')
export RAW_UPLOAD_METADATA_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."raw-upload-metadata-queue"')

export CHUNK_PROCESSED_TOPIC_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sns_topic_arns | jq -r '."chunk-processed"')

if CLOUDFRONT_CDN_URL=$(terraform -chdir=$TERRAFORM_PATH output -raw cloud_front_cdn_url 2>/dev/null); then
  export CLOUDFRONT_CDN_URL
  export THUMBNAIL_BASE_URL="https://$CLOUDFRONT_CDN_URL"
fi
export AWS_REGION=$(terraform -chdir=$TERRAFORM_PATH output -raw region)
export SHIN_AWS_REGION=$AWS_REGION

export AWS_ACCESS_KEY_ID=$(aws configure get aws_access_key_id)
export AWS_SECRET_ACCESS_KEY=$(aws configure get aws_secret_access_key)
export AWS_SESSION_TOKEN=$(aws configure get aws_session_token)

echo "Starting Docker services..."
docker compose up -d

echo "Dev environment initialized successfully"

#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TERRAFORM_PATH="$PROJECT_ROOT/infra"
TFVARS_FILE="environments/dev/terraform.tfvars"

cd "$PROJECT_ROOT"

echo "Initializing development environment..."

CLOUDFRONT_KEY_DIR="$TERRAFORM_PATH/modules/cloudfront"
if [ ! -f "$CLOUDFRONT_KEY_DIR/cloudfront_private_key.pem" ]; then
  echo "Generating CloudFront RSA key pair..."
  openssl genrsa -out "$CLOUDFRONT_KEY_DIR/cloudfront_private_key.pem" 2048
  openssl rsa -pubout \
    -in "$CLOUDFRONT_KEY_DIR/cloudfront_private_key.pem" \
    -out "$CLOUDFRONT_KEY_DIR/cloudfront_public_key.pem"
  openssl pkcs8 -topk8 -nocrypt \
    -in "$CLOUDFRONT_KEY_DIR/cloudfront_private_key.pem" \
    -out "$CLOUDFRONT_KEY_DIR/cloudfront_private_key_pkcs8.pem"
  echo "Key pair generated."
fi

ENGAGEMENT_DIR="$PROJECT_ROOT/lambdas/engagement"

if [ ! -f "$ENGAGEMENT_DIR/processor/bootstrap.zip" ]; then
  echo "Building engagement-processor Lambda..."
  (cd "$ENGAGEMENT_DIR/processor" && GOOS=linux GOARCH=amd64 go build -o bootstrap . && zip bootstrap.zip bootstrap && rm bootstrap)
  echo "engagement-processor built."
fi

THUMBNAIL_DIR="$PROJECT_ROOT/lambdas/thumbnail"
FFMPEG_LAYER_DIR="$THUMBNAIL_DIR/ffmpeg-layer"

if [ ! -s "$FFMPEG_LAYER_DIR/ffmpeg-layer.zip" ]; then
  echo "Building FFmpeg Lambda layer..."
  mkdir -p "$FFMPEG_LAYER_DIR/bin"
  curl -fL "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz" -o "/tmp/ffmpeg-static.tar.xz"
  tar -xf /tmp/ffmpeg-static.tar.xz --strip-components=1 -C /tmp/ --wildcards "*/ffmpeg"
  mv /tmp/ffmpeg "$FFMPEG_LAYER_DIR/bin/ffmpeg"
  chmod +x "$FFMPEG_LAYER_DIR/bin/ffmpeg"
  (
    cd "$FFMPEG_LAYER_DIR"
    rm -f ffmpeg-layer.zip
    zip -r ffmpeg-layer.zip bin/
  )
  if [ ! -s "$FFMPEG_LAYER_DIR/ffmpeg-layer.zip" ]; then
    echo "Failed to build ffmpeg-layer.zip"
    exit 1
  fi
  rm -rf "$FFMPEG_LAYER_DIR/bin"
  rm -f /tmp/ffmpeg-static.tar.xz /tmp/ffmpeg
  echo "FFmpeg layer built."
fi

if ! unzip -l "$FFMPEG_LAYER_DIR/ffmpeg-layer.zip" 2>/dev/null | grep -q "bin/ffmpeg"; then
  echo "Invalid ffmpeg-layer.zip: bin/ffmpeg not found. Rebuild required."
  exit 1
fi

if [ ! -f "$THUMBNAIL_DIR/go.sum" ]; then
  echo "Running go mod tidy for thumbnail-processor..."
  (cd "$THUMBNAIL_DIR" && go mod tidy)
fi

if [ ! -f "$THUMBNAIL_DIR/bootstrap.zip" ]; then
  echo "Building thumbnail-processor Lambda..."
  (cd "$THUMBNAIL_DIR" && GOOS=linux GOARCH=amd64 go build -o bootstrap . && zip bootstrap.zip bootstrap && rm bootstrap)
  echo "thumbnail-processor built."
fi

echo "Tearing down existing dev environment"
docker compose down -v
terraform -chdir=$TERRAFORM_PATH destroy -var-file="$TFVARS_FILE" -auto-approve

echo "Recreating dev environment"
terraform -chdir=$TERRAFORM_PATH init

set +e
APPLY_OUTPUT=$(terraform -chdir=$TERRAFORM_PATH apply -var-file="$TFVARS_FILE" -auto-approve 2>&1)
APPLY_STATUS=$?
set -e

printf "%s\n" "$APPLY_OUTPUT"

if [ $APPLY_STATUS -ne 0 ]; then
  if printf "%s" "$APPLY_OUTPUT" | grep -q "OriginAccessControlAlreadyExists"; then
    echo "CloudFront OAC already exists. Importing existing CloudFront resources and retrying apply..."

    EXISTING_OAC_ID=$(aws cloudfront list-origin-access-controls \
      --query "OriginAccessControlList.Items[?Name=='processed-bucket-oac'].Id | [0]" \
      --output text 2>/dev/null || true)

    if [ -n "$EXISTING_OAC_ID" ] && [ "$EXISTING_OAC_ID" != "None" ] && [ "$EXISTING_OAC_ID" != "null" ]; then
      terraform -chdir=$TERRAFORM_PATH import -var-file="$TFVARS_FILE" module.cloudfront.aws_cloudfront_origin_access_control.default "$EXISTING_OAC_ID" >/dev/null 2>&1 || true
    fi

    DIST_ID=""
    if [ -n "$EXISTING_OAC_ID" ] && [ "$EXISTING_OAC_ID" != "None" ] && [ "$EXISTING_OAC_ID" != "null" ]; then
      DIST_ID=$(aws cloudfront list-distributions \
        --query "DistributionList.Items[?contains(join(',', Origins.Items[].OriginAccessControlId), '$EXISTING_OAC_ID')].Id | [0]" \
        --output text 2>/dev/null || true)
    fi

    CF_DOMAIN="${CLOUDFRONT_CDN_URL:-}"
    if [ -z "$DIST_ID" ] && [ -z "$CF_DOMAIN" ]; then
      CF_DOMAIN=$(terraform -chdir=$TERRAFORM_PATH output -raw cloud_front_cdn_url 2>/dev/null || true)
    fi

    if [ -z "$DIST_ID" ] && [ -n "$CF_DOMAIN" ] && [ "$CF_DOMAIN" != "None" ] && [ "$CF_DOMAIN" != "null" ]; then
      DIST_ID=$(aws cloudfront list-distributions \
        --query "DistributionList.Items[?DomainName=='$CF_DOMAIN'].Id | [0]" \
        --output text 2>/dev/null || true)
    fi

    if [ -n "$DIST_ID" ] && [ "$DIST_ID" != "None" ] && [ "$DIST_ID" != "null" ]; then
      terraform -chdir=$TERRAFORM_PATH import -var-file="$TFVARS_FILE" module.cloudfront.aws_cloudfront_distribution.s3_distribution "$DIST_ID" >/dev/null 2>&1 || true
    fi

    terraform -chdir=$TERRAFORM_PATH apply \
      -var-file="$TFVARS_FILE" \
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
export CREATOR_PICTURES_BUCKET_NAME=$(terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."creator_pictures"')

export OPENSEARCH_COLLECTION_ENDPOINT=$(terraform -chdir=$TERRAFORM_PATH output -raw open_search_collection_endpoint)
export OPENSEARCH_COLLETION_ENDPOINT=$OPENSEARCH_COLLECTION_ENDPOINT
export OPENSEARCH_DASHBOARD_ENDPOINT=$(terraform -chdir=$TERRAFORM_PATH output -raw open_search_dashboard_endpoint)

export DECODE_JOB_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."decode-job"')
export THUMBNAIL_JOB_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."thumbnail-job"')
export ENCODING_FINISHED_EVENTS_QUEUE_URL=$ENCODING_FINISHED_EVENTS_QUEUE_URL
export THUMBNAIL_FINISHED_EVENTS_QUEUE_URL=$THUMBNAIL_FINISHED_EVENTS_QUEUE_URL
export VIDEO_UPLOAD_CREATED_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."video-upload-created"')
export VIEW_EVENTS_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."view-events"')
export ENCODING_FINISHED_EVENTS_QUEUE_NAME=$(basename "$ENCODING_FINISHED_EVENTS_QUEUE_URL")
export THUMBNAIL_FINISHED_EVENTS_QUEUE_NAME=$(basename "$THUMBNAIL_FINISHED_EVENTS_QUEUE_URL")
export VIDEO_UPLOAD_CREATED_QUEUE_NAME=$(basename "$VIDEO_UPLOAD_CREATED_QUEUE_URL")
export VIEW_EVENTS_QUEUE_NAME=$(basename "$VIEW_EVENTS_QUEUE_URL")
export ENCODING_FINISHED_DLQ_NAME="${ENCODING_FINISHED_EVENTS_QUEUE_NAME}-dlq"
export THUMBNAIL_FINISHED_DLQ_NAME="${THUMBNAIL_FINISHED_EVENTS_QUEUE_NAME}-dlq"
export VIEW_EVENTS_DLQ_NAME="${VIEW_EVENTS_QUEUE_NAME}-dlq"
export THREAD_CREATED_METADATA_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."thread-created-metadata"')
export THREAD_CREATED_NOTIFICATION_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."thread-created-notification"')
export COMMENT_REPLY_METADATA_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."comment-reply-metadata"')
export COMMENT_REPLY_NOTIFICATION_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."comment-reply-notification"')
export VIDEO_PUBLISHED_OPENSEARCH_INDEXER_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."video-published-opensearch-indexer"')
export VIDEO_PUBLISHED_NOTIFICATION_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."video-published-notification"')
export VIDEO_UPDATED_EVENTS_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."video-updated-events"')
export THREAD_CREATED_METADATA_QUEUE_NAME=$(basename "$THREAD_CREATED_METADATA_QUEUE_URL")
export THREAD_CREATED_NOTIFICATION_QUEUE_NAME=$(basename "$THREAD_CREATED_NOTIFICATION_QUEUE_URL")
export COMMENT_REPLY_METADATA_QUEUE_NAME=$(basename "$COMMENT_REPLY_METADATA_QUEUE_URL")
export COMMENT_REPLY_NOTIFICATION_QUEUE_NAME=$(basename "$COMMENT_REPLY_NOTIFICATION_QUEUE_URL")
export VIDEO_PUBLISHED_QUEUE_NAME=$(basename "$VIDEO_PUBLISHED_OPENSEARCH_INDEXER_QUEUE_URL")
export VIDEO_PUBLISHED_NOTIFICATION_QUEUE_NAME=$(basename "$VIDEO_PUBLISHED_NOTIFICATION_QUEUE_URL")
export VIDEO_UPDATED_EVENTS_QUEUE_NAME=$(basename "$VIDEO_UPDATED_EVENTS_QUEUE_URL")
export LIKE_EVENTS_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."like-events"')
export DISLIKE_EVENTS_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."dislike-events"')
export CHANNEL_SUBSCRIBED_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."channel-subscribed"')
export CHANNEL_UNSUBSCRIBED_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."channel-unsubscribed"')
export VIEW_EVENTS_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."view-events"')
export THREAD_CREATED_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."thread-created"')
export COMMENT_REPLY_CREATED_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."comment-reply-created"')
export COMMENT_UPDATED_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."comment-updated"')
export COMMENT_DELETED_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."comment-deleted"')
export DECODE_JOB_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."decode-job"')
export THUMBNAIL_JOB_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."thumbnail-job"')
export ENCODING_FINISHED_EVENTS_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."encoding-finished-events"')
export THUMBNAIL_FINISHED_EVENTS_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."thumbnail-finished-events"')
export VIDEO_UPLOAD_CREATED_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."video-upload-created"')
export THREAD_CREATED_METADATA_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."thread-created-metadata"')
export THREAD_CREATED_NOTIFICATION_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."thread-created-notification"')
export COMMENT_REPLY_METADATA_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."comment-reply-metadata"')
export COMMENT_REPLY_NOTIFICATION_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."comment-reply-notification"')
export VIDEO_UPDATED_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."video-updated-events"')
export VIDEO_UPDATED_EVENTS_QUEUE_ARN=$VIDEO_UPDATED_QUEUE_ARN
export VIDEO_PUBLISHED_OPENSEARCH_INDEXER_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."video-published-opensearch-indexer"')
export VIDEO_PUBLISHED_NOTIFICATION_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."video-published-notification"')

# Backward-compatible aliases
export VIDEO_VIDEO_PUBLISHED_OPENSEARCH_INDEXER_QUEUE_ARN=$VIDEO_PUBLISHED_OPENSEARCH_INDEXER_QUEUE_ARN
export VIDEO_VIDEO_PUBLISHED_NOTIFICATION_SERVICE_QUEUE_ARN=$VIDEO_PUBLISHED_NOTIFICATION_QUEUE_ARN
export VIDEO_VIDEO_PUBLISHED_OPENSEARCH_INDEXER_QUEUE_URL=$VIDEO_PUBLISHED_OPENSEARCH_INDEXER_QUEUE_URL
export VIDEO_VIDEO_PUBLISHED_NOTIFICATION_SERVICE_QUEUE_URL=$VIDEO_PUBLISHED_NOTIFICATION_QUEUE_URL

export RAW_UPLOAD_CREATED_TOPIC_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sns_topic_arns | jq -r '."raw-upload-created"')
export VIDEO_PUBLISHED_TOPIC_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sns_topic_arns | jq -r '."video-published"')
export THREAD_CREATED_TOPIC_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sns_topic_arns | jq -r '."thread-created"')
export COMMENT_REPLY_TOPIC_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sns_topic_arns | jq -r '."comment-reply"')

if CLOUDFRONT_CDN_URL=$(terraform -chdir=$TERRAFORM_PATH output -raw cloud_front_cdn_url 2>/dev/null); then
  export CLOUDFRONT_CDN_URL
  export THUMBNAIL_BASE_URL="https://$CLOUDFRONT_CDN_URL"
fi

if CLOUDFRONT_KEY_PAIR_ID=$(terraform -chdir=$TERRAFORM_PATH output -raw cloudfront_key_pair_id 2>/dev/null); then
  export CLOUDFRONT_KEY_PAIR_ID
fi

if CLOUDFRONT_PRIVATE_KEY_SECRET_ARN=$(terraform -chdir=$TERRAFORM_PATH output -raw cloudfront_private_key_secret_arn 2>/dev/null); then
  export CLOUDFRONT_PRIVATE_KEY_SECRET_ARN
fi

export AWS_REGION=$(terraform -chdir=$TERRAFORM_PATH output -raw region)
export SHIN_AWS_REGION=$AWS_REGION

export AWS_ACCESS_KEY_ID=$(aws configure get aws_access_key_id)
export AWS_SECRET_ACCESS_KEY=$(aws configure get aws_secret_access_key)
export AWS_SESSION_TOKEN=$(aws configure get aws_session_token)

echo "Starting Docker services..."
docker compose up -d

echo "Dev environment initialized successfully"

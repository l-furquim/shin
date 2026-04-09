#!/usr/bin/env fish

set SCRIPT_DIR (dirname (status --current-filename))
set PROJECT_ROOT (realpath "$SCRIPT_DIR/..")
set TERRAFORM_PATH "$PROJECT_ROOT/infra"

cd $PROJECT_ROOT

echo "Initializing development environment..."

set -l CLOUDFRONT_KEY_DIR "$TERRAFORM_PATH/modules/cloudfront"
if not test -f "$CLOUDFRONT_KEY_DIR/cloudfront_private_key.pem"
    echo "Generating CloudFront RSA key pair..."
    openssl genrsa -out "$CLOUDFRONT_KEY_DIR/cloudfront_private_key.pem" 2048
    openssl rsa -pubout \
        -in "$CLOUDFRONT_KEY_DIR/cloudfront_private_key.pem" \
        -out "$CLOUDFRONT_KEY_DIR/cloudfront_public_key.pem"
    openssl pkcs8 -topk8 -nocrypt \
        -in "$CLOUDFRONT_KEY_DIR/cloudfront_private_key.pem" \
        -out "$CLOUDFRONT_KEY_DIR/cloudfront_private_key_pkcs8.pem"
    echo "Key pair generated."
end

set ENGAGEMENT_DIR "$PROJECT_ROOT/lambdas/engagement"

if not test -f "$ENGAGEMENT_DIR/processor/bootstrap.zip"
    echo "Building engagement-processor Lambda..."
    cd "$ENGAGEMENT_DIR/processor"
    GOOS=linux GOARCH=amd64 go build -o bootstrap .
    zip bootstrap.zip bootstrap
    rm bootstrap
    cd -
    echo "engagement-processor built."
end

set THUMBNAIL_DIR "$PROJECT_ROOT/lambdas/thumbnail"
set FFMPEG_LAYER_DIR "$THUMBNAIL_DIR/ffmpeg-layer"

if not test -f "$FFMPEG_LAYER_DIR/ffmpeg-layer.zip"
    echo "Downloading FFmpeg static binary for Lambda layer..."
    mkdir -p "$FFMPEG_LAYER_DIR/bin"
    curl -L "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz" \
        -o "/tmp/ffmpeg-static.tar.xz"
    tar -xf /tmp/ffmpeg-static.tar.xz --strip-components=1 -C /tmp/ --wildcards "*/ffmpeg"
    mv /tmp/ffmpeg "$FFMPEG_LAYER_DIR/bin/ffmpeg"
    chmod +x "$FFMPEG_LAYER_DIR/bin/ffmpeg"
    cd "$FFMPEG_LAYER_DIR"
    zip -r ffmpeg-layer.zip bin/
    rm -rf bin/
    rm /tmp/ffmpeg-static.tar.xz
    cd -
    echo "FFmpeg layer built."
end

if not test -f "$THUMBNAIL_DIR/go.sum"
    echo "Running go mod tidy for thumbnail-processor..."
    cd "$THUMBNAIL_DIR"
    go mod tidy
    cd -
end

if not test -f "$THUMBNAIL_DIR/bootstrap.zip"
    echo "Building thumbnail-processor Lambda..."
    cd "$THUMBNAIL_DIR"
    GOOS=linux GOARCH=amd64 go build -o bootstrap .
    zip bootstrap.zip bootstrap
    rm bootstrap
    cd -
    echo "thumbnail-processor built."
end

# echo "Tearing down existing dev environment"
# docker compose down -v
# terraform -chdir=$TERRAFORM_PATH destroy \
#   -var-file="environments/dev/terraform.tfvars" \
#   -auto-approve

echo "Recreating dev environment"
terraform -chdir=$TERRAFORM_PATH init
or return 1

set -l apply_output (terraform -chdir=$TERRAFORM_PATH apply \
  -var-file="environments/dev/terraform.tfvars" \
  -auto-approve 2>&1)
set -l apply_status $status

printf "%s\n" "$apply_output"

if test $apply_status -ne 0
    if string match -q "*OriginAccessControlAlreadyExists*" "$apply_output"
        echo "CloudFront OAC already exists. Reapplying without cloudfront resources..."
        terraform -chdir=$TERRAFORM_PATH apply \
          -var-file="environments/dev/terraform.tfvars" \
          -target=module.s3 \
          -target=module.sns \
          -target=module.sqs \
          -target=module.subscriptions \
          -target=module.secrets \
          -auto-approve
        or return 1
    else
        return $apply_status
    end
end

echo "Exporting Terraform outputs..."

set -Ux RAW_BUCKET_NAME (terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."raw"')
set -Ux PROCESSED_BUCKET_NAME (terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."processed"')
set -Ux THUMBNAIL_BUCKET_NAME (terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."thumbnail"')
set -Ux CREATOR_PICTURES_BUCKET_NAME (terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."creator-pictures"')

set -Ux DECODE_JOB_QUEUE_URL (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."decode-job"')
set -Ux THUMBNAIL_JOB_QUEUE_URL (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."thumbnail-job"')
set -Ux ENCODING_FINISHED_EVENTS_QUEUE_URL (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."encoding-finished-events"')
set -Ux THUMBNAIL_FINISHED_EVENTS_QUEUE_URL (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."thumbnail-finished-events"')
set -Ux RAW_UPLOAD_METADATA_QUEUE_URL (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."raw-upload-metadata-queue"')
set -Ux ENCODING_FINISHED_EVENTS_QUEUE_NAME (basename $ENCODING_FINISHED_EVENTS_QUEUE_URL)
set -Ux THUMBNAIL_FINISHED_EVENTS_QUEUE_NAME (basename $THUMBNAIL_FINISHED_EVENTS_QUEUE_URL)
set -Ux RAW_UPLOAD_METADATA_QUEUE_NAME (basename $RAW_UPLOAD_METADATA_QUEUE_URL)
set -Ux SUBSCRIPTION_EVENTS_QUEUE_URL (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."subscription-events"')
set -Ux LIKE_EVENTS_QUEUE_URL (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."like-events"')
set -Ux VIEW_EVENTS_QUEUE_URL (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."view-events"')
set -Ux DECODE_JOB_QUEUE_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."decode-job"')
set -Ux THUMBNAIL_JOB_QUEUE_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."thumbnail-job"')
set -Ux ENCODING_FINISHED_EVENTS_QUEUE_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."encoding-finished-events"')
set -Ux THUMBNAIL_FINISHED_EVENTS_QUEUE_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."thumbnail-finished-events"')
set -Ux RAW_UPLOAD_METADATA_QUEUE_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."raw-upload-metadata-queue"')
set -Ux VIDEO_INITIALIZED_QUEUE_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."video-initialized"')
set -Ux LIKE_EVENTS_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."like-events"')
set -Ux DISLIKE_EVENTS_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."dislike-events"')
set -Ux CHANNEL_SUBSCRIBED_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."channel-subscribed"')
set -Ux CHANNEL_UNSUBSCRIBED_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."channel-unsubscribed"')
set -Ux VIEW_EVENTS_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."view-events"')
set -Ux THREAD_CREATED_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."thread-created"')
set -Ux COMMENT_REPLY_CREATED_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."comment-reply-created"')
set -Ux COMMENT_UPDATED_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."comment-updated"')
set -Ux COMMENT_DELETED_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."comment-deleted"')
set -Ux PLAYBACK_PROGRESS_QUEUE_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."video-playback-started"')

set -Ux CHUNK_PROCESSED_TOPIC_ARN (terraform -chdir=$TERRAFORM_PATH output -json sns_topic_arns | jq -r '."chunk-processed"')

set -l cf_url (terraform -chdir=$TERRAFORM_PATH output -raw cloud_front_cdn_url 2>/dev/null)
if test $status -eq 0 -a -n "$cf_url"
    set -Ux CLOUDFRONT_CDN_URL $cf_url
    set -Ux THUMBNAIL_BASE_URL "https://$CLOUDFRONT_CDN_URL"
end

set -l cf_key_pair_id (terraform -chdir=$TERRAFORM_PATH output -raw cloudfront_key_pair_id 2>/dev/null)
if test $status -eq 0 -a -n "$cf_key_pair_id"
    set -Ux CLOUDFRONT_KEY_PAIR_ID $cf_key_pair_id
end

set -l cf_secret_arn (terraform -chdir=$TERRAFORM_PATH output -raw cloudfront_private_key_secret_arn 2>/dev/null)
if test $status -eq 0 -a -n "$cf_secret_arn"
    set -Ux CLOUDFRONT_PRIVATE_KEY_SECRET_ARN $cf_secret_arn
end


set -Ux AWS_REGION (terraform -chdir=$TERRAFORM_PATH output -raw region)
set -Ux SHIN_AWS_REGION $AWS_REGION

set -Ux AWS_ACCESS_KEY_ID (aws configure get aws_access_key_id)
set -Ux AWS_SECRET_ACCESS_KEY (aws configure get aws_secret_access_key)
set -Ux AWS_SESSION_TOKEN (aws configure get aws_session_token)

echo "Starting Docker services..."
docker compose up -d
or return 1

echo "Dev environment initialized successfully"

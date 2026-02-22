#!/usr/bin/env fish

set SCRIPT_DIR (dirname (status --current-filename))
set PROJECT_ROOT (realpath "$SCRIPT_DIR/..")
set TERRAFORM_PATH "$PROJECT_ROOT/infra"

cd $PROJECT_ROOT

echo "Initializing development environment..."

# echo "Tearing down existing dev environment"
# docker compose down -v
# terraform -chdir=$TERRAFORM_PATH destroy \
#   -var-file="environments/dev/terraform.tfvars" \
#   -auto-approve

echo "Recreating dev environment"
terraform -chdir=$TERRAFORM_PATH init
terraform -chdir=$TERRAFORM_PATH apply \
  -var-file="environments/dev/terraform.tfvars" \
  -auto-approve

echo "Exporting Terraform outputs..."

set -Ux RAW_BUCKET_NAME (terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."raw"')
set -Ux PROCESSED_BUCKET_NAME (terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."processed"')
set -Ux THUMBNAIL_BUCKET_NAME (terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."thumbnail"')
set -Ux CREATOR_PICTURES_BUCKET_NAME (terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."creator-pictures"')

set -Ux DECODE_JOB_QUEUE_URL (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."decode-job"')
set -Ux THUMBNAIL_JOB_QUEUE_URL (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."thumbnail-job"')
set -Ux METADATA_EVENT_QUEUE (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_names | jq -r '."metadata-events"')
set -Ux SUBSCRIPTION_EVENTS_QUEUE_URL (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."subscription-events"')
set -Ux LIKE_EVENTS_QUEUE_URL (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."like-events"')
set -Ux SUBSCRIPTION_EVENTS_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."subscription-events"')
set -Ux LIKE_EVENTS_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."like-events"')

set -Ux CHUNK_PROCESSED_TOPIC_ARN (terraform -chdir=$TERRAFORM_PATH output -json sns_topic_arns | jq -r '."chunk-processed"')
set -Ux ENCODE_FINISHED_TOPIC_ARN (terraform -chdir=$TERRAFORM_PATH output -json sns_topic_arns | jq -r '."encode-finished"')
set -Ux THUMBNAIL_GENERATED_TOPIC_ARN (terraform -chdir=$TERRAFORM_PATH output -json sns_topic_arns | jq -r '."thumbnail-generated"')

set -Ux CLOUDFRONT_CDN_URL (terraform -chdir=$TERRAFORM_PATH output -raw cloud_front_cdn_url)
set -Ux AWS_REGION (terraform -chdir=$TERRAFORM_PATH output -raw region)

set -Ux AWS_ACCESS_KEY_ID (aws configure get aws_access_key_id)
set -Ux AWS_SECRET_ACCESS_KEY (aws configure get aws_secret_access_key)
set -Ux AWS_SESSION_TOKEN (aws configure get aws_session_token)

echo "Starting Docker services..."
docker compose up -d

echo "Dev environment initialized successfully"

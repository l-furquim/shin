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
terraform -chdir=$TERRAFORM_PATH apply -var-file="environments/dev/terraform.tfvars" -auto-approve

echo "Exporting Terraform outputs..."

export RAW_BUCKET_NAME=$(terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."raw"')
export PROCESSED_BUCKET_NAME=$(terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."processed"')
export THUMBNAIL_BUCKET_NAME=$(terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."thumbnail"')
export CREATOR_PICTURES_BUCKET_NAME=$(terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."creator-pictures"')

export DECODE_JOB_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."decode-job"')
export THUMBNAIL_JOB_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."thumbnail-job"')
export METADATA_EVENT_QUEUE=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_names | jq -r '."metadata-events"')
export SUBSCRIPTION_EVENTS_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."subscription-events"')
export LIKE_EVENTS_QUEUE_URL=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."like-events"')
export SUBSCRIPTION_EVENTS_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."subscription-events"')
export LIKE_EVENTS_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."like-events"')

export CHUNK_PROCESSED_TOPIC_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sns_topic_arns | jq -r '."chunk-processed"')
export ENCODE_FINISHED_TOPIC_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sns_topic_arns | jq -r '."encode-finished"')
export THUMBNAIL_GENERATED_TOPIC_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sns_topic_arns | jq -r '."thumbnail-generated"')

export CLOUDFRONT_CDN_URL=$(terraform -chdir=$TERRAFORM_PATH output -raw cloud_front_cdn_url)
export AWS_REGION=$(terraform -chdir=$TERRAFORM_PATH output -raw region)

export AWS_ACCESS_KEY_ID=$(aws configure get aws_access_key_id)
export AWS_SECRET_ACCESS_KEY=$(aws configure get aws_secret_access_key)
export AWS_SESSION_TOKEN=$(aws configure get aws_session_token)

echo "Starting Docker services..."
docker compose up -d

echo "Dev environment initialized successfully"

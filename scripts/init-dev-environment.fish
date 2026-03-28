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
set -Ux ENCODING_FINISHED_EVENTS_QUEUE_NAME (basename $ENCODING_FINISHED_EVENTS_QUEUE_URL)
set -Ux THUMBNAIL_FINISHED_EVENTS_QUEUE_NAME (basename $THUMBNAIL_FINISHED_EVENTS_QUEUE_URL)
set -Ux SUBSCRIPTION_EVENTS_QUEUE_URL (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."subscription-events"')
set -Ux LIKE_EVENTS_QUEUE_URL (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."like-events"')
set -Ux VIEW_EVENTS_QUEUE_URL (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."view-events"')
set -Ux DECODE_JOB_QUEUE_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."decode-job"')
set -Ux THUMBNAIL_JOB_QUEUE_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."thumbnail-job"')
set -Ux ENCODING_FINISHED_EVENTS_QUEUE_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."encoding-finished-events"')
set -Ux THUMBNAIL_FINISHED_EVENTS_QUEUE_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."thumbnail-finished-events"')
set -Ux SUBSCRIPTION_EVENTS_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."subscription-events"')
set -Ux LIKE_EVENTS_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."like-events"')
set -Ux VIEW_EVENTS_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."view-events"')

set -Ux CHUNK_PROCESSED_TOPIC_ARN (terraform -chdir=$TERRAFORM_PATH output -json sns_topic_arns | jq -r '."chunk-processed"')

set -l cf_url (terraform -chdir=$TERRAFORM_PATH output -raw cloud_front_cdn_url 2>/dev/null)
if test $status -eq 0 -a -n "$cf_url"
    set -Ux CLOUDFRONT_CDN_URL $cf_url
    set -Ux THUMBNAIL_BASE_URL "https://$CLOUDFRONT_CDN_URL"
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

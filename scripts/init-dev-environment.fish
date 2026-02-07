#!/usr/bin/env fish
set -e

set TERRAFORM_PATH "../infra"

echo "Initializing development environment..."
echo "Tearing down existing dev environment"

docker compose down -v
# terraform -chdir=$TERRAFORM_PATH destroy \
#   -var-file="environments/dev/terraform.tfvars" \
#   -auto-approve

echo "Recreating dev environment"

terraform -chdir=$TERRAFORM_PATH init
terraform -chdir=$TERRAFORM_PATH apply \
  -var-file="environments/dev/terraform.tfvars" \
  -auto-approve

echo "Exporting environment variables for local development"

set -x DECODE_JOB_QUEUE_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."decode-job"')
set -x PROCESSED_BUCKET_NAME (terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."processed"')
set -x RAW_BUCKET_NAME (terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."raw"')
set -x THUMBNAIL_BUCKET_NAME (terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."thumbnail"')
set -x SHIN_AWS_REGION (terraform -chdir=$TERRAFORM_PATH output -raw region)

echo "Environment variables exported successfully"

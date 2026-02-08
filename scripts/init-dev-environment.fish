#!/usr/bin/env fish

set SCRIPT_DIR (dirname (status --current-filename))
set PROJECT_ROOT (realpath "$SCRIPT_DIR/..")
set TERRAFORM_PATH "$PROJECT_ROOT/infra"

cd $PROJECT_ROOT

echo "Initializing development environment..."

echo "Tearing down existing dev environment"
# docker compose down -v
# terraform -chdir=$TERRAFORM_PATH destroy \
#   -var-file="environments/dev/terraform.tfvars" \
#   -auto-approve

echo "Recreating dev environment"
terraform -chdir=$TERRAFORM_PATH init
# terraform -chdir=$TERRAFORM_PATH apply \
#   -var-file="environments/dev/terraform.tfvars" \
#   -auto-approve

echo "Exporting Terraform outputs..."

set -Ux DECODE_JOB_QUEUE_ARN (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."decode-job"')
set -Ux PROCESSED_BUCKET_NAME (terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."processed"')
set -Ux RAW_BUCKET_NAME (terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."raw"')
set -Ux THUMBNAIL_BUCKET_NAME (terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."thumbnail"')
set -Ux AWS_REGION (terraform -chdir=$TERRAFORM_PATH output -raw region)
set -Ux DECODE_JOB_QUEUE_URL (terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_urls | jq -r '."decode-job"')

echo "Dev environment initialized successfully"

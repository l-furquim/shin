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

export DECODE_JOB_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."decode-job"')
export PROCESSED_BUCKET_NAME=$(terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."processed"')
export RAW_BUCKET_NAME=$(terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."raw"')
export THUMBNAIL_BUCKET_NAME=$(terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."thumbnail"')
export AWS_REGION=$(terraform -chdir=$TERRAFORM_PATH output -raw region)

echo "Starting Docker services..."
docker compose up -d

echo "Development environment initialized successfully."

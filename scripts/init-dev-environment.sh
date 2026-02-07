#!/bin/bash
set -e

TERRAFORM_PATH="../infra"

echo "Initializing development environment..."
echo "Tearing down existing dev environment"

docker compose down -v
terraform  -chdir=$TERRAFORM_PATH destroy -var-file="environments/dev/terraform.tfvars" -auto-approve


echo "Recreating dev environment"
#docker compose up -d
terraform -chdir=$TERRAFORM_PATH init
terraform -chdir=$TERRAFORM_PATH apply -var-file="environments/dev/terraform.tfvars" -auto-approve

echo "Exporting environment variables for local development"

export DECODE_JOB_QUEUE_ARN=$(terraform -chdir=$TERRAFORM_PATH output -json sqs_queue_arns | jq -r '."decode-job"')
export PROCESSED_BUCKET_NAME=$(terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."processed"')
export RAW_BUCKET_NAME=$(terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."raw"')
export THUMBNAIL_BUCKET_NAME=$(terraform -chdir=$TERRAFORM_PATH output -json s3_bucket_names | jq -r '."thumbnail"')
export SHIN_AWS_REGION=$(terraform -chdir=$TERRAFORM_PATH output -raw region)
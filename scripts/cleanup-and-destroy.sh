#!/bin/bash

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

AWS_REGION="${SHIN_AWS_REGION:-us-east-1}"
ENV="${ENV:-dev}"

S3_BUCKETS=(
    "shin-${ENV}-raw-uploads"
    "shin-${ENV}-processed-videos"
    "shin-${ENV}-thumbnails"
    "shin-${ENV}-creator-pictures"
)

SQS_QUEUES=(
    "shin-${ENV}-decode-job"
    "shin-${ENV}-decode-job-dlq"
    "shin-${ENV}-thumbnail-job"
    "shin-${ENV}-thumbnail-job-dlq"
    "shin-${ENV}-metadata-events"
    "shin-${ENV}-metadata-events-dlq"
)

SNS_TOPICS=(
    "shin-${ENV}-chunk-processed"
    "shin-${ENV}-thumbnail-generated"
    "shin-${ENV}-encode-started"
    "shin-${ENV}-encode-finished"
)

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_aws_cli() {
    if ! command -v aws &> /dev/null; then
        log_error "Error while running aws command, check if its installed."
        exit 1
    fi

    log_info "Checking AWS credentials..."
    if ! aws sts get-caller-identity &> /dev/null; then
        log_error "AWS credentials not configured. Please run 'aws configure' first."
        exit 1
    fi

    log_success "AWS CLI configured correctly"
}

purge_sqs_queues() {
    log_info "Purging SQS queues..."

    for queue_name in "${SQS_QUEUES[@]}"; do
        log_info "Purging queue: $queue_name"

        queue_url=$(aws sqs get-queue-url \
            --queue-name "$queue_name" \
            --region "$AWS_REGION" \
            --output text 2>/dev/null || echo "")

        if [ -z "$queue_url" ]; then
            log_warning "Queue $queue_name not found, skipping..."
            continue
        fi

        msg_count=$(aws sqs get-queue-attributes \
            --queue-url "$queue_url" \
            --attribute-names ApproximateNumberOfMessages \
            --region "$AWS_REGION" \
            --query 'Attributes.ApproximateNumberOfMessages' \
            --output text)

        if [ "$msg_count" -gt 0 ]; then
            log_info "Queue has approximately $msg_count messages. Purging..."
            aws sqs purge-queue \
                --queue-url "$queue_url" \
                --region "$AWS_REGION"
            log_success "Purged $queue_name"
        else
            log_info "Queue $queue_name is already empty"
        fi
    done

    log_success "SQS queue purging complete"
}

empty_s3_buckets() {
    log_info "Emptying S3 buckets..."

    for bucket in "${S3_BUCKETS[@]}"; do
        log_info "Emptying bucket: $bucket"

        if ! aws s3api head-bucket --bucket "$bucket" --region "$AWS_REGION" 2>/dev/null; then
            log_warning "Bucket $bucket not found, skipping..."
            continue
        fi

        object_count=$(aws s3 ls "s3://$bucket" --recursive --summarize 2>/dev/null | grep "Total Objects:" | awk '{print $3}')

        if [ -z "$object_count" ] || [ "$object_count" -eq 0 ]; then
            log_info "Bucket $bucket is already empty"
        else
            log_info "Bucket has $object_count objects. Deleting..."

            aws s3 rm "s3://$bucket" --recursive --region "$AWS_REGION"

            aws s3api delete-objects \
                --bucket "$bucket" \
                --delete "$(aws s3api list-object-versions \
                    --bucket "$bucket" \
                    --output json \
                    --region "$AWS_REGION" \
                    --query '{Objects: Versions[].{Key:Key,VersionId:VersionId}}')" \
                2>/dev/null || true

            aws s3api delete-objects \
                --bucket "$bucket" \
                --delete "$(aws s3api list-object-versions \
                    --bucket "$bucket" \
                    --output json \
                    --region "$AWS_REGION" \
                    --query '{Objects: DeleteMarkers[].{Key:Key,VersionId:VersionId}}')" \
                2>/dev/null || true

            log_success "Emptied $bucket"
        fi
    done

    log_success "S3 bucket emptying complete"
}

list_sns_subscriptions() {
    log_info "Listing SNS topic subscriptions..."

    for topic_name in "${SNS_TOPICS[@]}"; do
        log_info "Checking subscriptions for topic: $topic_name"

        topic_arn=$(aws sns list-topics \
            --region "$AWS_REGION" \
            --query "Topics[?contains(TopicArn, '$topic_name')].TopicArn" \
            --output text 2>/dev/null || echo "")

        if [ -z "$topic_arn" ]; then
            log_warning "Topic $topic_name not found"
            continue
        fi

        subscriptions=$(aws sns list-subscriptions-by-topic \
            --topic-arn "$topic_arn" \
            --region "$AWS_REGION" \
            --query 'Subscriptions[].SubscriptionArn' \
            --output text)

        if [ -n "$subscriptions" ]; then
            log_info "Found subscriptions for $topic_name"
            echo "$subscriptions"
        fi
    done
}

destroy_infrastructure() {
    log_warning "Starting infrastructure destruction..."

    echo ""

    INFRA_DIR="$(dirname "$0")/../infra"

    if [ ! -d "$INFRA_DIR" ]; then
        log_error "Infrastructure directory not found at $INFRA_DIR"
        exit 1
    fi

    cd "$INFRA_DIR"

    log_info "Checking Terraform state..."
    if [ ! -f "terraform.tfstate" ]; then
        log_warning "No terraform.tfstate found. Infrastructure may not exist."
        return
    fi

    log_info "Backing up Terraform state..."
    cp terraform.tfstate "terraform.tfstate.backup.$(date +%Y%m%d_%H%M%S)"

    CLOUDFRONT_ID=$(terraform output -raw cloud_front_cdn_url 2>/dev/null | cut -d'.' -f1 || echo "")

    if [ -n "$CLOUDFRONT_ID" ]; then
        log_info "Found CloudFront distribution: $CLOUDFRONT_ID"
        log_info "Removing CloudFront from Terraform state to preserve it..."

        terraform state rm 'module.cloudfront.aws_cloudfront_distribution.s3_distribution' 2>/dev/null || true
        terraform state rm 'module.cloudfront.aws_cloudfront_origin_access_control.default' 2>/dev/null || true
        terraform state rm 'module.cloudfront.aws_s3_bucket_policy.processed_policy' 2>/dev/null || true

        log_success "CloudFront resources removed from Terraform state"
    fi

    log_warning "Running terraform destroy..."

    terraform destroy \
        -var-file="environments/dev/terraform.tfvars" \
        -auto-approve

    log_success "Infrastructure destruction complete"
}

main() {
    echo "Starting the cleanup proccess."

    check_aws_cli

    echo ""
    log_info "Purging SQS queues"
    purge_sqs_queues

    echo ""
    log_info "Emptying S3 buckets"
    empty_s3_buckets

    echo ""
    log_info "Listing SNS subscriptions"
    list_sns_subscriptions

    echo ""
    log_warning "Destroying infrastructure"
    destroy_infrastructure

}

main

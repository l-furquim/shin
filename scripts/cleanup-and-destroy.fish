#!/usr/bin/env fish

set RED '\033[0;31m'
set GREEN '\033[0;32m'
set YELLOW '\033[1;33m'
set BLUE '\033[0;34m'
set NC '\033[0m'

set -x AWS_REGION (test -n "$SHIN_AWS_REGION" && echo $SHIN_AWS_REGION || echo "us-east-1")
set ENV (test -n "$ENV" && echo $ENV || echo "dev")

set S3_BUCKETS \
    "shin-$ENV-raw-uploads" \
    "shin-$ENV-processed-videos" \
    "shin-$ENV-thumbnails" \
    "shin-$ENV-creator-pictures"

set SQS_QUEUES \
    "shin-$ENV-decode-job" \
    "shin-$ENV-decode-job-dlq" \
    "shin-$ENV-thumbnail-job" \
    "shin-$ENV-thumbnail-job-dlq" \
    "shin-$ENV-metadata-events" \
    "shin-$ENV-metadata-events-dlq" \
    "shin-$ENV-subscription-events" \
    "shin-$ENV-subscription-events-dlq" \
    "shin-$ENV-like-events" \
    "shin-$ENV-like-events-dlq"

set SNS_TOPICS \
    "shin-$ENV-chunk-processed" \
    "shin-$ENV-thumbnail-generated" \
    "shin-$ENV-encode-started" \
    "shin-$ENV-encode-finished"

function log_info
    printf "%b[INFO]%b %s\n" $BLUE $NC "$argv"
end

function log_success
    printf "%b[SUCCESS]%b %s\n" $GREEN $NC "$argv"
end

function log_warning
    printf "%b[WARNING]%b %s\n" $YELLOW $NC "$argv"
end

function log_error
    printf "%b[ERROR]%b %s\n" $RED $NC "$argv"
end

function check_aws_cli
    if not command -v aws &> /dev/null
        log_error "Error while running aws command, check if its installed."
        exit 1
    end

    log_info "Checking AWS credentials..."
    if not aws sts get-caller-identity &> /dev/null
        log_error "AWS credentials not configured. Please run 'aws configure' first."
        exit 1
    end

    log_success "AWS CLI configured correctly"
end

function purge_sqs_queues
    log_info "Purging SQS queues..."

    for queue_name in $SQS_QUEUES
        log_info "Purging queue: $queue_name"

        set queue_url (aws sqs get-queue-url \
            --queue-name "$queue_name" \
            --region "$AWS_REGION" \
            --output text 2>/dev/null || echo "")

        if test -z "$queue_url"
            log_warning "Queue $queue_name not found, skipping..."
            continue
        end

        set msg_count (aws sqs get-queue-attributes \
            --queue-url "$queue_url" \
            --attribute-names ApproximateNumberOfMessages \
            --region "$AWS_REGION" \
            --query 'Attributes.ApproximateNumberOfMessages' \
            --output text)

        if test "$msg_count" -gt 0
            log_info "Queue has approximately $msg_count messages. Purging..."
            aws sqs purge-queue \
                --queue-url "$queue_url" \
                --region "$AWS_REGION"
            log_success "Purged $queue_name"
        else
            log_info "Queue $queue_name is already empty"
        end
    end

    log_success "SQS queue purging complete"
end

function empty_s3_buckets
    log_info "Emptying S3 buckets..."

    for bucket in $S3_BUCKETS
        log_info "Emptying bucket: $bucket"

        if not aws s3api head-bucket --bucket "$bucket" --region "$AWS_REGION" 2>/dev/null
            log_warning "Bucket $bucket not found, skipping..."
            continue
        end

        set object_count (aws s3 ls "s3://$bucket" --recursive --summarize 2>/dev/null | grep "Total Objects:" | awk '{print $3}')

        if test -z "$object_count" -o "$object_count" -eq 0
            log_info "Bucket $bucket is already empty"
        else
            log_info "Bucket has $object_count objects. Deleting..."

            aws s3 rm "s3://$bucket" --recursive --region "$AWS_REGION"

            set versions (aws s3api list-object-versions \
                --bucket "$bucket" \
                --output json \
                --region "$AWS_REGION" \
                --query '{Objects: Versions[].{Key:Key,VersionId:VersionId}}' 2>/dev/null)

            if test -n "$versions" -a "$versions" != "null"
                aws s3api delete-objects \
                    --bucket "$bucket" \
                    --delete "$versions" \
                    2>/dev/null || true
            end

            set markers (aws s3api list-object-versions \
                --bucket "$bucket" \
                --output json \
                --region "$AWS_REGION" \
                --query '{Objects: DeleteMarkers[].{Key:Key,VersionId:VersionId}}' 2>/dev/null)

            if test -n "$markers" -a "$markers" != "null"
                aws s3api delete-objects \
                    --bucket "$bucket" \
                    --delete "$markers" \
                    2>/dev/null || true
            end

            log_success "Emptied $bucket"
        end
    end

    log_success "S3 bucket emptying complete"
end

function list_sns_subscriptions
    log_info "Listing SNS topic subscriptions..."

    for topic_name in $SNS_TOPICS
        log_info "Checking subscriptions for topic: $topic_name"

        set topic_arn (aws sns list-topics \
            --region "$AWS_REGION" \
            --query "Topics[?contains(TopicArn, '$topic_name')].TopicArn" \
            --output text 2>/dev/null || echo "")

        if test -z "$topic_arn"
            log_warning "Topic $topic_name not found"
            continue
        end

        set subscriptions (aws sns list-subscriptions-by-topic \
            --topic-arn "$topic_arn" \
            --region "$AWS_REGION" \
            --query 'Subscriptions[].SubscriptionArn' \
            --output text)

        if test -n "$subscriptions"
            log_info "Found subscriptions for $topic_name"
            echo $subscriptions
        end
    end
end

function destroy_infrastructure
    log_warning "Starting infrastructure destruction..."

    echo ""

    set INFRA_DIR (dirname (status -f))/../infra

    if not test -d "$INFRA_DIR"
        log_error "Infrastructure directory not found at $INFRA_DIR"
        exit 1
    end

    cd "$INFRA_DIR"
    log_info "Checking Terraform state..."
    if not test -f "terraform.tfstate"
        log_warning "No terraform.tfstate found. Infrastructure may not exist."
        return
    end

    log_info "Backing up Terraform state..."
    set backup_file "terraform.tfstate.backup."(date +%Y%m%d_%H%M%S)
    cp terraform.tfstate "$backup_file"

    set CLOUDFRONT_ID (terraform output -raw cloud_front_cdn_url 2>/dev/null | cut -d'.' -f1 || echo "")

    # Do not remove cloudfront since its using my free plan.
    if test -n "$CLOUDFRONT_ID"
        log_info "Found CloudFront distribution: $CLOUDFRONT_ID"
        log_info "Removing CloudFront from Terraform state to preserve it..."

        terraform state rm 'module.cloudfront.aws_cloudfront_distribution.s3_distribution' 2>/dev/null || true
        terraform state rm 'module.cloudfront.aws_cloudfront_origin_access_control.default' 2>/dev/null || true
        terraform state rm 'module.cloudfront.aws_s3_bucket_policy.processed_policy' 2>/dev/null || true

        log_success "CloudFront resources removed from Terraform state"
    end

    log_warning "Running terraform destroy..."

    terraform destroy \
        -var-file="environments/dev/terraform.tfvars" \
        -auto-approve

    log_success "Infrastructure destruction complete"
end

function main
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

end

main

#!/usr/bin/env fish

set RED '\033[0;31m'
set GREEN '\033[0;32m'
set YELLOW '\033[1;33m'
set BLUE '\033[0;34m'
set NC '\033[0m'

set SCRIPT_DIR (dirname (status --current-filename))
set PROJECT_ROOT (realpath "$SCRIPT_DIR/..")
set ENV_FILE "$PROJECT_ROOT/.env"

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

function ensure_command --argument-names cmd
    if not command -q "$cmd"
        log_error "Missing required command: $cmd"
        exit 1
    end
end

function load_dotenv
    if not test -f "$ENV_FILE"
        return
    end

    for raw_line in (string split \n -- (cat "$ENV_FILE"))
        set line (string trim -- "$raw_line")

        if test -z "$line"
            continue
        end

        if string match -qr '^#' -- "$line"
            continue
        end

        set parts (string split -m 1 '=' -- "$line")
        if test (count $parts) -lt 2
            continue
        end

        set key (string trim -- "$parts[1]")
        set value (string trim -- "$parts[2]")
        set value (string replace -r '^"(.*)"$' '$1' -- "$value")
        if string match -qr "^'.*'\\z" -- "$value"
            set value (string sub -s 2 -l (math (string length -- "$value") - 2) -- "$value")
        end

        if not set -q $key
            set -gx $key "$value"
        end
    end
end

function check_aws_cli
    ensure_command aws
    ensure_command jq

    if not aws sts get-caller-identity --output json >/dev/null 2>&1
        log_error "AWS credentials not configured. Run 'aws configure' first."
        exit 1
    end

    log_success "AWS CLI configured correctly"
end

function unique_items
    set -l out

    for item in $argv
        if test -z "$item" -o "$item" = "None"
            continue
        end

        if not contains -- "$item" $out
            set out $out "$item"
        end
    end

    if test (count $out) -gt 0
        printf "%s\n" $out
    end
end

function empty_s3_bucket --argument-names bucket
    if not aws s3api head-bucket --bucket "$bucket" --region "$AWS_REGION" >/dev/null 2>&1
        log_warning "Bucket not found or inaccessible: $bucket"
        return 0
    end

    log_info "Emptying S3 bucket: $bucket"
    aws s3 rm "s3://$bucket" --recursive --region "$AWS_REGION" >/dev/null 2>&1

    set versions_output (aws s3api list-object-versions --bucket "$bucket" --region "$AWS_REGION" --output json 2>/dev/null)
    if test -n "$versions_output"
        set versions_payload (printf '%s' "$versions_output" | jq -c '{Objects: ((.Versions // []) | map({Key: .Key, VersionId: .VersionId}))}')
        set versions_count (printf '%s' "$versions_payload" | jq '.Objects | length')

        if test "$versions_count" -gt 0
            aws s3api delete-objects --bucket "$bucket" --region "$AWS_REGION" --delete "$versions_payload" >/dev/null 2>&1
        end

        set markers_payload (printf '%s' "$versions_output" | jq -c '{Objects: ((.DeleteMarkers // []) | map({Key: .Key, VersionId: .VersionId}))}')
        set markers_count (printf '%s' "$markers_payload" | jq '.Objects | length')

        if test "$markers_count" -gt 0
            aws s3api delete-objects --bucket "$bucket" --region "$AWS_REGION" --delete "$markers_payload" >/dev/null 2>&1
        end
    end

    log_success "Bucket cleaned: $bucket"
end

function clean_s3_data
    log_info "Removing data from S3 buckets"

    set discovered_buckets (aws s3api list-buckets --query "Buckets[?starts_with(Name, 'shin-$ENV-')].Name" --output text 2>/dev/null)
    set candidate_buckets \
        $discovered_buckets \
        "$RAW_BUCKET_NAME" \
        "$PROCESSED_BUCKET_NAME" \
        "$THUMBNAIL_BUCKET_NAME" \
        "$CREATOR_PICTURES_BUCKET_NAME"

    set buckets (unique_items $candidate_buckets)

    if test (count $buckets) -eq 0
        log_warning "No S3 buckets found for prefix shin-$ENV-"
        return 0
    end

    for bucket in $buckets
        empty_s3_bucket "$bucket"
    end

    log_success "S3 cleanup complete"
end

function purge_sqs_queues
    log_info "Purging SQS queues (normal and DLQ)"

    set queue_urls (aws sqs list-queues --queue-name-prefix "shin-$ENV-" --region "$AWS_REGION" --query 'QueueUrls[]' --output text 2>/dev/null)

    if test -z "$queue_urls" -o "$queue_urls" = "None"
        log_warning "No SQS queues found for prefix shin-$ENV-"
        return 0
    end

    for queue_url in $queue_urls
        set queue_name (basename "$queue_url")
        if aws sqs purge-queue --queue-url "$queue_url" --region "$AWS_REGION" >/dev/null 2>&1
            log_success "Purged queue: $queue_name"
        else
            log_warning "Could not purge queue right now (possibly purge in progress): $queue_name"
        end
    end

    log_success "SQS purge complete"
end

function wipe_dynamodb_table --argument-names table_name
    set key_names (aws dynamodb describe-table --table-name "$table_name" --region "$AWS_REGION" --query 'Table.KeySchema[].AttributeName' --output text 2>/dev/null)

    if test -z "$key_names" -o "$key_names" = "None"
        log_warning "Skipping table with unknown key schema: $table_name"
        return 0
    end

    set key_csv (string join ',' $key_names)
    set last_evaluated_key ""

    log_info "Cleaning DynamoDB table: $table_name"

    while true
        set scan_output ""

        if test -n "$last_evaluated_key"
            set scan_output (aws dynamodb scan \
                --table-name "$table_name" \
                --region "$AWS_REGION" \
                --exclusive-start-key "$last_evaluated_key" \
                --output json 2>/dev/null)
        else
            set scan_output (aws dynamodb scan \
                --table-name "$table_name" \
                --region "$AWS_REGION" \
                --output json 2>/dev/null)
        end

        if test -z "$scan_output"
            break
        end

        set item_count (printf '%s' "$scan_output" | jq '.Items | length')
        if test "$item_count" -gt 0
            for payload in (printf '%s' "$scan_output" | jq -c --arg table "$table_name" --arg key_csv "$key_csv" '
                ($key_csv | split(",")) as $keys
                | [ .Items[]
                    | {DeleteRequest: {Key: with_entries(select(.key as $k | $keys | index($k)))}}
                  ] as $deletes
                | range(0; ($deletes | length); 25) as $i
                | {($table): $deletes[$i:($i + 25)]}
            ')
                set batch_output (aws dynamodb batch-write-item --request-items "$payload" --region "$AWS_REGION" --output json 2>/dev/null)
                if test $status -ne 0
                    log_warning "Batch delete failed for table: $table_name"
                    continue
                end

                set retry_count 0
                set unprocessed (printf '%s' "$batch_output" | jq -c '.UnprocessedItems')

                while test "$unprocessed" != "{}" -a "$retry_count" -lt 10
                    sleep 1
                    set batch_output (aws dynamodb batch-write-item --request-items "$unprocessed" --region "$AWS_REGION" --output json 2>/dev/null)
                    set unprocessed (printf '%s' "$batch_output" | jq -c '.UnprocessedItems')
                    set retry_count (math "$retry_count + 1")
                end
            end
        end

        set last_evaluated_key (printf '%s' "$scan_output" | jq -c '.LastEvaluatedKey // empty')
        if test -z "$last_evaluated_key"
            break
        end
    end

    log_success "DynamoDB table cleaned: $table_name"
end

function wipe_dynamodb_data
    log_info "Removing data from all DynamoDB tables"

    set tables (aws dynamodb list-tables --region "$AWS_REGION" --query 'TableNames[]' --output text 2>/dev/null)

    if test -z "$tables" -o "$tables" = "None"
        log_warning "No DynamoDB tables found"
        return 0
    end

    for table_name in $tables
        wipe_dynamodb_table "$table_name"
    end

    log_success "DynamoDB cleanup complete"
end

function run_postgres_sql --argument-names sql
    if command -q psql
        env PGPASSWORD="$POSTGRES_PASSWORD" psql \
            -h "$POSTGRES_HOST" \
            -p "$POSTGRES_PORT" \
            -U "$POSTGRES_USER" \
            -d "$POSTGRES_DB" \
            -v ON_ERROR_STOP=1 \
            -c "$sql"
        return $status
    end

    if command -q docker
        set running_containers (docker ps --format '{{.Names}}')
        if contains -- "shin-postgres" $running_containers
            docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" -i shin-postgres \
                psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -c "$sql"
            return $status
        end
    end

    log_error "Could not connect to PostgreSQL. Install psql or run shin-postgres container."
    return 1
end

function clean_postgres_data
    log_info "Cleaning PostgreSQL data (keeping user.users and user.creators)"

    set sql "DO \$cleanup\$ DECLARE r RECORD; BEGIN FOR r IN SELECT table_schema, table_name FROM information_schema.tables WHERE table_type = 'BASE TABLE' AND table_schema NOT IN ('pg_catalog', 'information_schema') AND table_name <> 'flyway_schema_history' AND NOT (table_schema = 'user' AND table_name IN ('users', 'creators')) LOOP EXECUTE format('TRUNCATE TABLE %I.%I CASCADE', r.table_schema, r.table_name); END LOOP; END \$cleanup\$;"

    if run_postgres_sql "$sql"
        log_success "PostgreSQL cleanup complete"
    else
        return 1
    end
end

function flush_redis_data
    log_info "Flushing Redis data"

    if command -q redis-cli
        redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning FLUSHALL >/dev/null
        if test $status -eq 0
            log_success "Redis cleanup complete"
            return 0
        end
    end

    if command -q docker
        set running_containers (docker ps --format '{{.Names}}')
        if contains -- "shin-redis" $running_containers
            docker exec -i shin-redis redis-cli -a "$REDIS_PASSWORD" --no-auth-warning FLUSHALL >/dev/null
            if test $status -eq 0
                log_success "Redis cleanup complete"
                return 0
            end
        end
    end

    log_error "Could not flush Redis. Install redis-cli or run shin-redis container."
    return 1
end

function validate_safety
    set env_normalized (string lower -- "$ENV")
    if contains -- "$env_normalized" prod production
        if test "$SHIN_ALLOW_PROD_CLEANUP" != "true"
            log_error "Refusing to run against production. Set SHIN_ALLOW_PROD_CLEANUP=true to override."
            exit 1
        end
    end
end

function configure_runtime
    load_dotenv

    if not set -q ENV
        set -gx ENV dev
    end

    if test (count $argv) -ge 1
        set -gx ENV "$argv[1]"
    end

    if set -q SHIN_AWS_REGION
        set -gx AWS_REGION "$SHIN_AWS_REGION"
    else if not set -q AWS_REGION
        set -gx AWS_REGION us-east-1
    end

    if not set -q POSTGRES_HOST
        set -gx POSTGRES_HOST localhost
    end

    if not set -q POSTGRES_PORT
        set -gx POSTGRES_PORT 5432
    end

    if not set -q POSTGRES_USER
        set -gx POSTGRES_USER root
    end

    if not set -q POSTGRES_PASSWORD
        set -gx POSTGRES_PASSWORD root
    end

    if not set -q POSTGRES_DB
        set -gx POSTGRES_DB shin
    end

    if not set -q REDIS_HOST
        set -gx REDIS_HOST localhost
    end

    if not set -q REDIS_PORT
        set -gx REDIS_PORT 6379
    end

    if not set -q REDIS_PASSWORD
        set -gx REDIS_PASSWORD redis
    end
end

function main
    configure_runtime $argv
    validate_safety

    log_warning "Starting full data cleanup for ENV=$ENV in AWS region $AWS_REGION"
    echo ""

    check_aws_cli

    set failed_steps

    clean_s3_data; or set failed_steps $failed_steps "S3"
    purge_sqs_queues; or set failed_steps $failed_steps "SQS"
    wipe_dynamodb_data; or set failed_steps $failed_steps "DynamoDB"
    clean_postgres_data; or set failed_steps $failed_steps "PostgreSQL"
    flush_redis_data; or set failed_steps $failed_steps "Redis"

    echo ""
    if test (count $failed_steps) -gt 0
        log_error "Cleanup finished with failures in: "(string join ', ' $failed_steps)
        return 1
    end

    log_success "Cleanup completed successfully"
    log_info "Preserved data only in PostgreSQL tables: user.users and user.creators"
end

main $argv

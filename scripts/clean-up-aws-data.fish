#!/usr/bin/env fish

echo "Removing data from s3"

set env 'dev'

echo "Removing raw bucket data"

aws s3 rm s3://shin-$env-raw-uploads --recursive

echo "Removing processed bucket data"

aws s3 rm s3://shin-$env-processed-videos

echo "Removing thumbnail bucket data"

aws s3 rm s3://shin-$env-thumbnails

echo "Removing creator bucket data"

aws s3 rm s3://shin-$env-creator-pictures

echo "Removing data from dynamodb"

echo "Removing data from channel_subscriptions"

aws dynamodb delete-item --table-name channel_subscriptions

echo "Removing data from channel_subscription_counters

aws dynamodb delete-item --table-name channel_subscription_counters"

echo "Removing data from channel_subscription_counters

aws dynamodb delete-item --table-name channel_subscription_counters"

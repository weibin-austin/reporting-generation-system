#!/usr/bin/env bash
# Creates the SNS/SQS/S3 resources the reporting system expects, including
# dead-letter queues with redrive policies (maxReceiveCount 3). Idempotent.
#
# Defaults target LocalStack on localhost:4566; override with:
#   AWS_ENDPOINT      endpoint url (default http://localhost:4566)
#   AWS_ACCOUNT_ID    account id used in ARNs (default 000000000000)
#   MAX_RECEIVE_COUNT deliveries before a message moves to the DLQ (default 3)
set -euo pipefail

ENDPOINT=${AWS_ENDPOINT:-http://localhost:4566}
REGION=${AWS_DEFAULT_REGION:-us-east-1}
ACCOUNT_ID=${AWS_ACCOUNT_ID:-000000000000}
MAX_RECEIVE_COUNT=${MAX_RECEIVE_COUNT:-3}

awsq() { aws --endpoint-url "$ENDPOINT" "$@"; }

echo "Creating SNS topic"
awsq sns create-topic --name reporting_topic > /dev/null

echo "Creating plain queues"
for q in email_queue Email_Queue; do
  awsq sqs create-queue --queue-name "$q" > /dev/null
done

echo "Creating pipeline queues with DLQs (maxReceiveCount=$MAX_RECEIVE_COUNT)"
for q in Excel_Request_Queue PDF_Request_Queue Excel_Response_Queue PDF_Response_Queue; do
  awsq sqs create-queue --queue-name "${q}_DLQ" > /dev/null
  awsq sqs create-queue --queue-name "$q" > /dev/null
  QUEUE_URL=$(awsq sqs get-queue-url --queue-name "$q" --query QueueUrl --output text)
  ATTRS=$(printf '{"RedrivePolicy":"{\\"deadLetterTargetArn\\":\\"arn:aws:sqs:%s:%s:%s_DLQ\\",\\"maxReceiveCount\\":\\"%s\\"}","VisibilityTimeout":"10"}' \
      "$REGION" "$ACCOUNT_ID" "$q" "$MAX_RECEIVE_COUNT")
  awsq sqs set-queue-attributes --queue-url "$QUEUE_URL" --attributes "$ATTRS"
done

echo "Creating S3 bucket"
awsq s3 mb "s3://reporting-generated-file" 2>/dev/null || true

echo "Subscribing request queues to the topic"
for q in Excel_Request_Queue PDF_Request_Queue; do
  awsq sns subscribe \
    --topic-arn "arn:aws:sns:${REGION}:${ACCOUNT_ID}:reporting_topic" \
    --protocol sqs \
    --notification-endpoint "arn:aws:sqs:${REGION}:${ACCOUNT_ID}:${q}" > /dev/null
done

echo "Done"

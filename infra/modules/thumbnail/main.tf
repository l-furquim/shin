resource "aws_lambda_layer_version" "ffmpeg" {
  layer_name          = "shin-${var.env}-ffmpeg"
  filename            = var.ffmpeg_layer_zip
  source_code_hash    = var.ffmpeg_layer_hash
  compatible_runtimes = ["provided.al2023"]
}

data "aws_iam_policy_document" "processor" {
  statement {
    actions   = ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes"]
    resources = [var.thumbnail_job_queue_arn, var.thumbnail_upload_queue_arn]
  }
  statement {
    actions   = ["s3:GetObject", "s3:HeadObject"]
    resources = ["${var.raw_bucket_arn}/*", "${var.thumbnail_bucket_arn}/*"]
  }
  statement {
    actions   = ["s3:PutObject"]
    resources = ["${var.thumbnail_bucket_arn}/*"]
  }
  statement {
    actions   = ["sqs:SendMessage"]
    resources = [var.thumbnail_finished_queue_arn]
  }
}

resource "aws_iam_policy" "processor" {
  name   = "shin-${var.env}-thumbnail-processor-policy"
  policy = data.aws_iam_policy_document.processor.json
}

resource "aws_lambda_event_source_mapping" "thumbnail_upload" {
  event_source_arn = var.thumbnail_upload_queue_arn
  function_name    = module.lambda_processor.function_name
  batch_size       = 1

  depends_on = [module.lambda_processor]
}

module "lambda_processor" {
  source             = "../lambda"
  env                = var.env
  function_name      = "thumbnail-processor"
  description        = "Generates video thumbnails at multiple resolutions using FFmpeg"
  filename           = var.processor_zip
  source_hash        = var.processor_hash
  timeout            = 120
  memory_size        = 1024
  enable_sqs_trigger = true
  sqs_trigger_arn    = var.thumbnail_job_queue_arn
  sqs_batch_size     = 1
  layers             = [aws_lambda_layer_version.ffmpeg.arn]
  environment_variables = {
    RAW_BUCKET_NAME                     = var.raw_bucket_name
    THUMBNAIL_BUCKET_NAME               = var.thumbnail_bucket_name
    THUMBNAIL_FINISHED_EVENTS_QUEUE_URL = var.thumbnail_finished_queue_url
    FFMPEG_PATH                         = "/opt/bin/ffmpeg"
    ENV                                 = var.env
  }
  extra_policy_arns = [
    aws_iam_policy.processor.arn,
    "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole"
  ]
}

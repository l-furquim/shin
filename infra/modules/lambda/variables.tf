variable "env" {
  type = string
}

variable "function_name" {
  type = string
}

variable "description" {
  type    = string
  default = ""
}

variable "runtime" {
  type    = string
  default = "provided.al2023"
}

variable "handler" {
  type    = string
  default = "bootstrap"
}

variable "memory_size" {
  type    = number
  default = 128
}

variable "timeout" {
  type    = number
  default = 30
}

variable "filename" {
  type        = string
  description = "Path to the Lambda deployment zip"
}

variable "source_hash" {
  type        = string
  description = "base64sha256 of the zip for change detection"
}

variable "environment_variables" {
  type    = map(string)
  default = {}
}

variable "sqs_trigger_arn" {
  type    = string
  default = null
}

variable "sqs_batch_size" {
  type    = number
  default = 10
}

variable "sqs_maximum_batching_window_seconds" {
  type        = number
  default     = null
  description = "Maximum batching window for SQS event source mapping"
}

variable "extra_policy_arns" {
  type    = list(string)
  default = []
}

variable "layers" {
  type        = list(string)
  default     = []
  description = "List of Lambda Layer ARNs to attach"
}

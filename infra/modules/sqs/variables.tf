variable "env" {
  description = "Environment name"
  type        = string
}


variable "queue_names" {
  description = "List of SQS queue names to create"
  type        = list(string)
}

variable "message_retention_seconds" {
  description = "Number of seconds SQS retains a message"
  type        = number
  default     = 345600 # 4 days
}

variable "visibility_timeout_seconds" {
  description = "Visibility timeout for SQS messages"
  type        = number
  default     = 300 # 5 minutes
}

variable "receive_wait_time_seconds" {
  description = "Wait time for SQS long polling"
  type        = number
  default     = 20
}

variable "delay_seconds" {
  description = "Delay seconds for message delivery"
  type        = number
  default     = 0
}

variable "enable_dlq" {
  description = "Enable dead letter queue for SQS queues"
  type        = bool
  default     = true
}

variable "max_receive_count" {
  description = "Maximum number of receives before sending to DLQ"
  type        = number
  default     = 3
}

variable "dlq_message_retention_seconds" {
  description = "Message retention seconds for DLQ"
  type        = number
  default     = 1209600 # 14 days
}

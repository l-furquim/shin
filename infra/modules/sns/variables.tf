variable "env" {
  description = "Environment name"
  type        = string
}


variable "topic_names" {
  description = "List of SNS topic names to create"
  type        = list(string)
}

variable "enable_dlq" {
  description = "Enable dead letter queue for SNS topics"
  type        = bool
  default     = false
}

variable "dlq_arn" {
  description = "ARN of the dead letter queue for failed messages"
  type        = string
  default     = ""
}

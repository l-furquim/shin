variable "env" {
  type = string
}

variable "billing_mode" {
  type        = string
  default     = "PAY_PER_REQUEST"
  description = "DynamoDB billing mode. Use PAY_PER_REQUEST for dev/test, PROVISIONED for production."
}

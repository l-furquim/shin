variable "env" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "intra_subnet_ids" {
  type = list(string)
}

variable "eks_cluster_version" {
  type    = string
  default = "1.31"
}

variable "node_group_apps_instance_types" {
  type    = list(string)
  default = ["t3.large"]
}

variable "node_group_apps_min_size" {
  type    = number
  default = 2
}

variable "node_group_apps_max_size" {
  type    = number
  default = 6
}

variable "node_group_apps_desired_size" {
  type    = number
  default = 2
}

variable "node_group_spot_instance_types" {
  type    = list(string)
  default = ["t3.large", "t3a.large", "m5.large"]
}

variable "node_group_spot_min_size" {
  type    = number
  default = 0
}

variable "node_group_spot_max_size" {
  type    = number
  default = 4
}

variable "node_group_spot_desired_size" {
  type    = number
  default = 0
}

variable "eks_admin_role_arns" {
  type    = list(string)
  default = []
}

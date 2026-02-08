# Shin Infrastructure - Terraform

This directory contains Terraform infrastructure as code for the Shin project

```
infra/
├── main.tf                 # Main terraform configuration with provider and module calls
├── variables.tf            # Root-level variable declarations
├── outputs.tf             # Root-level outputs
├── environments/          # Environment-specific configurations
│   ├── dev/
│   │   ├── terraform.tfvars    # Dev environment variable values
│   │   └── backend.tfvars      # Dev backend configuration
│   ├── staging/
│   │   ├── terraform.tfvars    # Staging environment variable values
│   │   └── backend.tfvars      # Staging backend configuration
│   └── prod/
│       ├── terraform.tfvars    # Production environment variable values
│       └── backend.tfvars      # Production backend configuration
└── modules/               # Reusable Terraform modules
    ├── s3/               # S3 bucket module
    │   ├── main.tf
    │   ├── variables.tf
    │   └── outputs.tf
    ├── sns/              # SNS topic module
    │   ├── main.tf
    │   ├── variables.tf
    │   └── outputs.tf
    └── sqs/              # SQS queue module
        ├── main.tf
        ├── variables.tf
        └── outputs.tf
```

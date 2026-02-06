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

## Setup Instructions

### 1. Create Backend Resources (One-time setup per environment)

Before using Terraform, you need to create the S3 bucket and DynamoDB table for state management:

```bash
# For dev environment
aws s3 mb s3://shin-terraform-state-dev --region us-east-1
aws dynamodb create-table \
  --table-name shin-terraform-locks-dev \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1

# Enable versioning on state bucket
aws s3api put-bucket-versioning \
  --bucket shin-terraform-state-dev \
  --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket shin-terraform-state-dev \
  --server-side-encryption-configuration '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'
```

Repeat for staging and prod environments with appropriate bucket names.

### 2. Initialize Terraform

```bash
# Initialize for dev environment
terraform init -backend-config=environments/dev/backend.tfvars

# Initialize for staging environment
terraform init -backend-config=environments/staging/backend.tfvars -reconfigure

# Initialize for prod environment
terraform init -backend-config=environments/prod/backend.tfvars -reconfigure
```

## Usage

### Planning Changes

```bash
# Plan changes for dev environment
terraform plan -var-file=environments/dev/terraform.tfvars

# Plan changes for staging environment
terraform plan -var-file=environments/staging/terraform.tfvars

# Plan changes for production environment
terraform plan -var-file=environments/prod/terraform.tfvars
```

### Applying Changes

```bash
# Apply changes to dev environment
terraform apply -var-file=environments/dev/terraform.tfvars

# Apply changes to staging environment
terraform apply -var-file=environments/staging/terraform.tfvars

# Apply changes to production environment
terraform apply -var-file=environments/prod/terraform.tfvars
```

### Destroying Resources

```bash
# Destroy dev environment resources
terraform destroy -var-file=environments/dev/terraform.tfvars

# Destroy staging environment resources
terraform destroy -var-file=environments/staging/terraform.tfvars

# Destroy production environment resources (use with extreme caution!)
terraform destroy -var-file=environments/prod/terraform.tfvars
```

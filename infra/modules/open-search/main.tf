resource "aws_opensearchserverless_security_policy" "encryption" {
  name        = "main-encryption-policy"
  type        = "encryption"
  description = "Encryption policy for main collection"
  policy = jsonencode({
    Rules = [{
      Resource     = ["collection/main-collection"]
      ResourceType = "collection"
    }]
    AWSOwnedKey = true
  })
}

resource "aws_opensearchserverless_security_policy" "network" {
  name        = "main-network-policy"
  type        = "network"
  description = "Public access for main collection"
  policy = jsonencode([{
    Description = "Public access for collection and dashboards"
    Rules = [
      { Resource = ["collection/main-collection"], ResourceType = "collection" },
      { Resource = ["collection/main-collection"], ResourceType = "dashboard" }
    ]
    AllowFromPublic = true
  }])
}

resource "aws_opensearchserverless_collection" "main" {
  name        = "main-collection"
  type        = "SEARCH"
  description = "Shin main search collection"
  depends_on  = [aws_opensearchserverless_security_policy.encryption]
}

resource "aws_opensearchserverless_access_policy" "data_access" {
  name = "main-access-policy"
  type = "data"
  policy = jsonencode([{
    Rules = [{
      ResourceType = "collection"
      Resource     = ["collection/main-collection"]
      Permission   = ["aoss:CreateCollectionItems", "aoss:DeleteCollectionItems", "aoss:UpdateCollectionItems", "aoss:DescribeCollectionItems"]
      }, {
      ResourceType = "index"
      Resource     = ["index/main-collection/*"]
      Permission   = ["aoss:ReadDocument", "aoss:WriteDocument", "aoss:CreateIndex", "aoss:DeleteIndex", "aoss:UpdateIndex", "aoss:DescribeIndex"]
    }]
    Principal = ["arn:aws:iam::${data.aws_caller_identity.me.account_id}:root"]
  }])
}

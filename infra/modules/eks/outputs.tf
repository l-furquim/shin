output "cluster_name" {
  value = module.this.cluster_name
}

output "cluster_endpoint" {
  value = module.this.cluster_endpoint
}

output "cluster_certificate_authority_data" {
  value = module.this.cluster_certificate_authority_data
}

output "cluster_security_group_id" {
  value = module.this.cluster_security_group_id
}

output "node_security_group_id" {
  value = module.this.node_security_group_id
}

output "oidc_provider_arn" {
  value = module.this.oidc_provider_arn
}

output "oidc_provider_url" {
  value = module.this.cluster_oidc_issuer_url
}

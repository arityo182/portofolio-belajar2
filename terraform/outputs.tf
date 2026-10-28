output "alb_dns_name" {
  value = module.alb.dns_name
}

output "rds_endpoint" {
  value     = module.rds.address
  sensitive = false
}

output "redis_endpoint" {
  value = module.elasticache.address
}

output "ecs_cluster_name" {
  value = module.ecs.cluster_name
}

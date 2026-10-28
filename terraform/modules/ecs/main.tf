variable "name" { type = string }
variable "environment" { type = string }
variable "aws_region" { type = string }
variable "vpc_id" { type = string }
variable "private_subnet_ids" { type = list(string) }
variable "security_group_id" { type = string }
variable "db_host" { type = string }
variable "container_images" { type = map(string) }
variable "task_environment" {
  type = list(object({ name = string, value = string }))
}
variable "task_secrets" {
  type = list(object({ name = string, valueFrom = string }))
}
variable "target_group_arns" { type = map(string) }
variable "log_retention_days" { type = number }
variable "tags" { type = map(string) }

locals {
  services = {
    auth-service = {
      port = 8081, cpu = 512, memory = 1024, target_group = "auth"
    }
    loan-service = {
      port = 8082, cpu = 1024, memory = 2048, target_group = "core"
    }
    scoring-service = {
      port = 8083, cpu = 1024, memory = 2048, target_group = "scoring"
    }
    payment-service = {
      port = 8084, cpu = 512, memory = 1024, target_group = "payment"
    }
    frontend = {
      port = 3000, cpu = 512, memory = 1024, target_group = "frontend"
    }
  }

  service_environment = {
    auth-service = [
      { name = "SERVER_PORT", value = "8081" },
      { name = "DB_URL", value = "jdbc:postgresql://${var.db_host}:5432/koperasi?currentSchema=auth_schema" },
      { name = "DB_USERNAME", value = "auth_user" },
      { name = "DB_SCHEMA", value = "auth_schema" },
    ]
    loan-service = [
      { name = "SERVER_PORT", value = "8082" },
      { name = "DB_URL", value = "jdbc:postgresql://${var.db_host}:5432/koperasi?currentSchema=loan_schema" },
      { name = "DB_USERNAME", value = "loan_user" },
      { name = "DB_SCHEMA", value = "loan_schema" },
      { name = "REPORT_DB_URL", value = "jdbc:postgresql://${var.db_host}:5432/koperasi?currentSchema=public" },
      { name = "REPORT_DB_USERNAME", value = "report_user" },
    ]
    payment-service = [
      { name = "SERVER_PORT", value = "8084" },
      { name = "DB_URL", value = "jdbc:postgresql://${var.db_host}:5432/koperasi?currentSchema=payment_schema" },
      { name = "DB_USERNAME", value = "payment_user" },
      { name = "DB_SCHEMA", value = "payment_schema" },
    ]
    scoring-service = [
      { name = "PORT", value = "8083" },
      { name = "DB_HOST", value = var.db_host },
      { name = "DB_PORT", value = "5432" },
      { name = "DB_NAME", value = "koperasi" },
      { name = "DB_USER", value = "scoring_user" },
      { name = "DB_SCHEMA", value = "scoring_schema" },
      { name = "KAFKA_ENABLED", value = "true" },
    ]
    frontend = [
      { name = "PORT", value = "3000" },
      { name = "HOSTNAME", value = "0.0.0.0" },
      { name = "NEXT_TELEMETRY_DISABLED", value = "1" },
    ]
  }
}

resource "aws_ecs_cluster" "this" {
  name = "${var.name}-${var.environment}"
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
  tags = var.tags
}

resource "aws_iam_role" "execution" {
  name = "${var.name}-ecs-execution"
  assume_role_policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [{ Effect = "Allow", Principal = { Service = "ecs-tasks.amazonaws.com" }, Action = "sts:AssumeRole" }]
  })
  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "execution" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "task" {
  name = "${var.name}-ecs-task"
  assume_role_policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [{ Effect = "Allow", Principal = { Service = "ecs-tasks.amazonaws.com" }, Action = "sts:AssumeRole" }]
  })
  tags = var.tags
}

resource "aws_cloudwatch_log_group" "service" {
  for_each          = local.services
  name              = "/ecs/${var.name}/${each.key}"
  retention_in_days = var.log_retention_days
  tags              = var.tags
}

resource "aws_service_discovery_private_dns_namespace" "this" {
  name        = "${var.name}.local"
  description = "Service discovery for ${var.name}"
  vpc         = var.vpc_id
  tags        = var.tags
}

resource "aws_service_discovery_service" "this" {
  for_each = local.services
  name     = each.key
  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.this.id
    dns_records {
      ttl  = 60
      type = "A"
    }
    routing_policy = "MULTIVALUE"
  }
  health_check_custom_config {
    failure_threshold = 1
  }
  tags = var.tags
}

resource "aws_ecs_task_definition" "this" {
  for_each                 = local.services
  family                   = "${var.name}-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = tostring(each.value.cpu)
  memory                   = tostring(each.value.memory)
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name         = each.key
      image        = var.container_images[each.key]
      essential    = true
      portMappings = [{ containerPort = each.value.port, hostPort = each.value.port, protocol = "tcp" }]
      environment  = concat(var.task_environment, local.service_environment[each.key])
      secrets      = var.task_secrets
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.service[each.key].name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
  tags = var.tags
}

resource "aws_ecs_service" "this" {
  for_each                          = local.services
  name                              = each.key
  cluster                           = aws_ecs_cluster.this.id
  task_definition                   = aws_ecs_task_definition.this[each.key].arn
  desired_count                     = 2
  launch_type                       = "FARGATE"
  platform_version                  = "LATEST"
  health_check_grace_period_seconds = 120

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.security_group_id]
    assign_public_ip = false
  }

  dynamic "load_balancer" {
    for_each = each.value.target_group == "" ? [] : [each.value.target_group]
    content {
      target_group_arn = var.target_group_arns[load_balancer.value]
      container_name   = each.key
      container_port   = each.value.port
    }
  }

  service_registries {
    registry_arn = aws_service_discovery_service.this[each.key].arn
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  tags = var.tags
}

output "cluster_name" { value = aws_ecs_cluster.this.name }

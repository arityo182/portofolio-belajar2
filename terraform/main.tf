terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.60"
    }
  }

  # Configure remote state before using in a real environment.
  # backend "s3" {
  #   bucket         = "koperasi-terraform-state"
  #   key            = "prod/terraform.tfstate"
  #   region         = "ap-southeast-1"
  #   dynamodb_table = "koperasi-terraform-locks"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.tags
  }
}

locals {
  tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

module "networking" {
  source = "./modules/networking"

  name                 = var.project_name
  vpc_cidr             = var.vpc_cidr
  availability_zones   = var.availability_zones
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  tags                 = local.tags
}

resource "aws_security_group" "alb" {
  name        = "${var.project_name}-alb"
  description = "Public ALB ingress"
  vpc_id      = module.networking.vpc_id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

resource "aws_security_group" "ecs" {
  name        = "${var.project_name}-ecs"
  description = "ECS tasks"
  vpc_id      = module.networking.vpc_id

  ingress {
    from_port       = 3000
    to_port         = 8086
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

resource "aws_security_group" "rds" {
  name        = "${var.project_name}-rds"
  description = "PostgreSQL from ECS"
  vpc_id      = module.networking.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }

  tags = local.tags
}

resource "aws_security_group" "redis" {
  name        = "${var.project_name}-redis"
  description = "Redis from ECS"
  vpc_id      = module.networking.vpc_id

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }

  tags = local.tags
}

resource "aws_secretsmanager_secret" "db_password" {
  name = "${var.project_name}/${var.environment}/db-password"
  tags = local.tags
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = var.db_password
}

resource "aws_secretsmanager_secret" "jwt_secret" {
  name = "${var.project_name}/${var.environment}/jwt-secret"
  tags = local.tags
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = var.jwt_secret
}

resource "aws_secretsmanager_secret" "redis_auth" {
  name = "${var.project_name}/${var.environment}/redis-auth-token"
  tags = local.tags
}

resource "aws_secretsmanager_secret_version" "redis_auth" {
  secret_id     = aws_secretsmanager_secret.redis_auth.id
  secret_string = var.redis_auth_token
}

module "rds" {
  source = "./modules/rds"

  name                  = var.project_name
  vpc_id                = module.networking.vpc_id
  private_subnet_ids    = module.networking.private_subnet_ids
  security_group_id     = aws_security_group.rds.id
  db_name               = var.db_name
  db_username           = var.db_username
  db_password           = var.db_password
  instance_class        = var.db_instance_class
  backup_retention_days = var.db_backup_retention_days
  tags                  = local.tags
}

module "elasticache" {
  source = "./modules/elasticache"

  name               = var.project_name
  private_subnet_ids = module.networking.private_subnet_ids
  security_group_id  = aws_security_group.redis.id
  node_type          = var.redis_node_type
  auth_token         = var.redis_auth_token
  tags               = local.tags
}

module "alb" {
  source = "./modules/alb"

  name              = var.project_name
  vpc_id            = module.networking.vpc_id
  public_subnet_ids = module.networking.public_subnet_ids
  security_group_id = aws_security_group.alb.id
  certificate_arn   = var.certificate_arn
  tags              = local.tags
}

locals {
  container_images = {
    auth-service         = "${var.container_registry}/koperasi-auth-service:${var.image_tag}"
    loan-service         = "${var.container_registry}/koperasi-loan-service:${var.image_tag}"
    scoring-service      = "${var.container_registry}/koperasi-scoring-service:${var.image_tag}"
    payment-service      = "${var.container_registry}/koperasi-payment-service:${var.image_tag}"
    frontend             = "${var.container_registry}/koperasi-frontend:${var.image_tag}"
  }

  task_environment = [
    { name = "SPRING_PROFILES_ACTIVE", value = "aws" },
    { name = "DB_HOST", value = module.rds.address },
    { name = "DB_PORT", value = "5432" },
    { name = "DB_NAME", value = var.db_name },
    { name = "REDIS_HOST", value = module.elasticache.address },
    { name = "REDIS_PORT", value = "6379" },
    { name = "JWT_ISSUER", value = "koperasi-auth" },
    { name = "CORS_ALLOWED_ORIGINS", value = "https://${var.domain_name}" },
    { name = "WEBSOCKET_ALLOWED_ORIGINS", value = "https://${var.domain_name}" },
    { name = "LOAN_SERVICE_URL", value = "http://loan-service.koperasi.local:8082" },
    { name = "SCORING_SERVICE_URL", value = "http://scoring-service.koperasi.local:8083" },
    { name = "PAYMENT_SERVICE_URL", value = "http://payment-service.koperasi.local:8084" },
    { name = "NEXT_PUBLIC_API_BASE_URL", value = "/api/v1" },
    { name = "NEXT_PUBLIC_WS_URL", value = "wss://${var.domain_name}/ws" },
  ]

  task_secrets = [
    { name = "DB_PASSWORD", valueFrom = aws_secretsmanager_secret.db_password.arn },
    { name = "JWT_SECRET", valueFrom = aws_secretsmanager_secret.jwt_secret.arn },
    { name = "REDIS_PASSWORD", valueFrom = aws_secretsmanager_secret.redis_auth.arn },
  ]
}

module "ecs" {
  source = "./modules/ecs"

  name               = var.project_name
  environment        = var.environment
  aws_region         = var.aws_region
  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
  security_group_id  = aws_security_group.ecs.id
  db_host            = module.rds.address
  container_images   = local.container_images
  task_environment   = local.task_environment
  task_secrets       = local.task_secrets
  target_group_arns  = module.alb.target_group_arns
  log_retention_days = var.log_retention_days
  tags               = local.tags
}

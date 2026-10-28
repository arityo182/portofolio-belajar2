variable "name" { type = string }
variable "vpc_id" { type = string }
variable "private_subnet_ids" { type = list(string) }
variable "security_group_id" { type = string }
variable "db_name" { type = string }
variable "db_username" { type = string }
variable "db_password" {
  type      = string
  sensitive = true
}
variable "instance_class" { type = string }
variable "backup_retention_days" { type = number }
variable "tags" { type = map(string) }

resource "aws_db_subnet_group" "this" {
  name       = "${var.name}-db"
  subnet_ids = var.private_subnet_ids
  tags       = var.tags
}

resource "aws_db_parameter_group" "this" {
  name   = "${var.name}-pg15"
  family = "postgres15"
  tags   = var.tags
}

resource "aws_db_instance" "this" {
  identifier                = "${var.name}-postgres"
  engine                    = "postgres"
  engine_version            = "15"
  instance_class            = var.instance_class
  allocated_storage         = 20
  max_allocated_storage     = 100
  storage_type              = "gp3"
  storage_encrypted         = true
  db_name                   = var.db_name
  username                  = var.db_username
  password                  = var.db_password
  db_subnet_group_name      = aws_db_subnet_group.this.name
  parameter_group_name      = aws_db_parameter_group.this.name
  vpc_security_group_ids    = [var.security_group_id]
  backup_retention_period   = var.backup_retention_days
  backup_window             = "18:00-19:00"
  maintenance_window        = "sun:19:00-sun:20:00"
  multi_az                  = false
  skip_final_snapshot       = false
  final_snapshot_identifier = "${var.name}-final"
  deletion_protection       = true
  apply_immediately         = false
  tags                      = var.tags
}

output "address" { value = aws_db_instance.this.address }
output "port" { value = aws_db_instance.this.port }

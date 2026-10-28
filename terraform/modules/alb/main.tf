variable "name" { type = string }
variable "vpc_id" { type = string }
variable "public_subnet_ids" { type = list(string) }
variable "security_group_id" { type = string }
variable "certificate_arn" { type = string }
variable "tags" { type = map(string) }

resource "aws_lb" "this" {
  name               = "${var.name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [var.security_group_id]
  subnets            = var.public_subnet_ids
  tags               = var.tags
}

resource "aws_lb_target_group" "frontend" {
  name        = "${var.name}-frontend"
  port        = 3000
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"
  health_check {
    path                = "/login"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 5
  }
  tags = var.tags
}

resource "aws_lb_target_group" "auth" {
  name        = "${var.name}-auth"
  port        = 8081
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"
  health_check {
    path                = "/actuator/health/liveness"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 5
  }
  tags = var.tags
}

resource "aws_lb_target_group" "core" {
  name        = "${var.name}-core"
  port        = 8082
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"
  health_check {
    path                = "/actuator/health/liveness"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 5
  }
  tags = var.tags
}

resource "aws_lb_target_group" "payment" {
  name        = "${var.name}-payment"
  port        = 8084
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"
  health_check {
    path                = "/actuator/health/liveness"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 5
  }
  tags = var.tags
}

resource "aws_lb_target_group" "scoring" {
  name        = "${var.name}-scoring"
  port        = 8083
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"
  health_check {
    path                = "/health"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 5
  }
  tags = var.tags
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"
  default_action {
    type = "redirect"
    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.this.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.frontend.arn
  }
}

resource "aws_lb_listener_rule" "internal_block" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 5
  action {
    type = "fixed-response"
    fixed_response {
      content_type = "application/json"
      status_code  = "404"
      message_body = "{\"success\":false,\"code\":\"NOT_FOUND\"}"
    }
  }
  condition {
    path_pattern { values = ["/api/v1/loans/internal/*"] }
  }
}

resource "aws_lb_listener_rule" "auth" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 10
  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.auth.arn
  }
  condition {
    path_pattern {
      values = ["/api/v1/auth/*", "/api/v1/admin/users/*", "/api/v1/borrowers/*"]
    }
  }
}

resource "aws_lb_listener_rule" "core" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 20
  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.core.arn
  }
  condition {
    path_pattern {
      values = ["/api/v1/loans/*", "/api/v1/notifications/*", "/api/v1/reports/*"]
    }
  }
}

resource "aws_lb_listener_rule" "payment" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 30
  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.payment.arn
  }
  condition {
    path_pattern { values = ["/api/v1/payments/*"] }
  }
}

resource "aws_lb_listener_rule" "scoring" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 40
  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.scoring.arn
  }
  condition {
    path_pattern { values = ["/api/v1/scoring/*"] }
  }
}

resource "aws_lb_listener_rule" "websocket" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 50
  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.core.arn
  }
  condition {
    path_pattern { values = ["/ws*"] }
  }
}

output "dns_name" { value = aws_lb.this.dns_name }
output "target_group_arns" {
  value = {
    frontend = aws_lb_target_group.frontend.arn
    auth     = aws_lb_target_group.auth.arn
    core     = aws_lb_target_group.core.arn
    payment  = aws_lb_target_group.payment.arn
    scoring  = aws_lb_target_group.scoring.arn
  }
}

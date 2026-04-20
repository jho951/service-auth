variable "aws_region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Project name used in tags."
  type        = string
  default     = "msa"
}

variable "environment" {
  description = "Environment name used in resource names."
  type        = string
  default     = "prod"
}

variable "service_name" {
  description = "Logical service name from service-contract."
  type        = string
  default     = "auth-service"
}

variable "service_runtime_name" {
  description = "Container name inside the ECS task."
  type        = string
  default     = "auth-service"
}

variable "service_role" {
  description = "Terraform role classification for this service."
  type        = string
  default     = "app-service"
}

variable "tags" {
  description = "Additional tags to apply to all taggable resources."
  type        = map(string)
  default     = {}
}

variable "vpc_cidr" {
  description = "CIDR block for the service VPC."
  type        = string
  default     = "10.20.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "Public subnet CIDRs for the ALB and NAT gateway."
  type        = list(string)
  default     = ["10.20.1.0/24", "10.20.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "Private subnet CIDRs for ECS tasks and optional RDS."
  type        = list(string)
  default     = ["10.20.11.0/24", "10.20.12.0/24"]
}

variable "app_port" {
  description = "Container port."
  type        = number
  default     = 8081
}

variable "app_ingress_cidrs" {
  description = "CIDRs allowed to reach the ALB production listener."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "test_listener_ingress_cidrs" {
  description = "CIDRs allowed to reach the CodeDeploy test listener."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "alb_internal" {
  description = "Whether the ALB is internal."
  type        = bool
  default     = false
}

variable "alb_listener_port" {
  description = "Production ALB listener port."
  type        = number
  default     = 80
}

variable "alb_test_listener_port" {
  description = "CodeDeploy test ALB listener port."
  type        = number
  default     = 9000
}

variable "health_check_path" {
  description = "ALB target group health check path."
  type        = string
  default     = "/actuator/health"
}

variable "health_check_matcher" {
  description = "ALB target group health check success matcher."
  type        = string
  default     = "200-399"
}

variable "health_check_interval" {
  description = "ALB health check interval in seconds."
  type        = number
  default     = 30
}

variable "health_check_timeout" {
  description = "ALB health check timeout in seconds."
  type        = number
  default     = 5
}

variable "healthy_threshold" {
  description = "Number of successful health checks before a target is healthy."
  type        = number
  default     = 2
}

variable "unhealthy_threshold" {
  description = "Number of failed health checks before a target is unhealthy."
  type        = number
  default     = 3
}

variable "health_check_grace_period_seconds" {
  description = "ECS service health check grace period."
  type        = number
  default     = 120
}

variable "target_deregistration_delay" {
  description = "ALB target group deregistration delay in seconds."
  type        = number
  default     = 30
}

variable "desired_count" {
  description = "Desired ECS task count."
  type        = number
  default     = 2
}

variable "task_cpu" {
  description = "Fargate task CPU units."
  type        = number
  default     = 512
}

variable "task_memory" {
  description = "Fargate task memory in MiB."
  type        = number
  default     = 1024
}

variable "enable_container_insights" {
  description = "Enable ECS cluster Container Insights."
  type        = bool
  default     = true
}

variable "enable_execute_command" {
  description = "Enable ECS Exec for the service."
  type        = bool
  default     = true
}

variable "create_ecr_repository" {
  description = "Create an ECR repository and use image_tag from it. Set false when container_image points to an external registry."
  type        = bool
  default     = true
}

variable "ecr_repository_name" {
  description = "Optional ECR repository name override."
  type        = string
  default     = ""
}

variable "image_tag" {
  description = "ECR image tag for the task definition."
  type        = string
  default     = "latest"
}

variable "container_image" {
  description = "Full container image URI. When empty, Terraform uses the created ECR repository plus image_tag."
  type        = string
  default     = ""
}

variable "container_command" {
  description = "Optional ECS container command override."
  type        = list(string)
  default     = []
}

variable "ecr_image_tag_mutability" {
  description = "ECR tag mutability. Prefer IMMUTABLE for release tags."
  type        = string
  default     = "MUTABLE"
}

variable "ecr_keep_image_count" {
  description = "Number of recent images retained by the ECR lifecycle policy."
  type        = number
  default     = 20
}

variable "log_retention_days" {
  description = "CloudWatch log retention in days."
  type        = number
  default     = 30
}

variable "codedeploy_deployment_config_name" {
  description = "CodeDeploy ECS deployment config."
  type        = string
  default     = "CodeDeployDefault.ECSCanary10Percent5Minutes"
}

variable "deployment_ready_wait_minutes" {
  description = "Minutes CodeDeploy waits before continuing when deployment is ready."
  type        = number
  default     = 0
}

variable "blue_termination_wait_minutes" {
  description = "Minutes to keep the old blue task set after a successful deployment."
  type        = number
  default     = 15
}

variable "enable_mysql" {
  description = "Whether to create a private RDS MySQL instance and inject its connection variables."
  type        = bool
  default     = false
}

variable "mysql_engine_version" {
  description = "RDS MySQL engine version."
  type        = string
  default     = "8.0"
}

variable "mysql_instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t4g.micro"
}

variable "mysql_allocated_storage" {
  description = "Initial RDS storage in GiB."
  type        = number
  default     = 20
}

variable "mysql_max_allocated_storage" {
  description = "Maximum autoscaled RDS storage in GiB."
  type        = number
  default     = 100
}

variable "mysql_database_name" {
  description = "Application database name."
  type        = string
  default     = ""
}

variable "mysql_username" {
  description = "Application database username."
  type        = string
  default     = ""
}

variable "mysql_password" {
  description = "Application database password."
  type        = string
  sensitive   = true
  default     = ""
}

variable "mysql_url_env_name" {
  description = "Environment variable name for the generated JDBC URL."
  type        = string
  default     = "MYSQL_URL"
}

variable "mysql_username_env_name" {
  description = "Environment variable name for the generated DB username."
  type        = string
  default     = "MYSQL_USER"
}

variable "mysql_password_env_name" {
  description = "Secret environment variable name for the generated DB password."
  type        = string
  default     = "MYSQL_PASSWORD"
}

variable "mysql_jdbc_query" {
  description = "Query string appended to the generated JDBC URL."
  type        = string
  default     = "?useSSL=true&serverTimezone=UTC"
}

variable "mysql_multi_az" {
  description = "Whether the RDS instance should be Multi-AZ."
  type        = bool
  default     = false
}

variable "mysql_backup_retention_days" {
  description = "RDS backup retention period in days."
  type        = number
  default     = 7
}

variable "mysql_deletion_protection" {
  description = "Enable RDS deletion protection."
  type        = bool
  default     = true
}

variable "mysql_skip_final_snapshot" {
  description = "Skip final snapshot on RDS destroy. Keep false for production."
  type        = bool
  default     = false
}

variable "mysql_apply_immediately" {
  description = "Apply RDS changes immediately instead of during the maintenance window."
  type        = bool
  default     = false
}

variable "app_env" {
  description = "Non-secret environment variables for the service."
  type        = map(string)
  default     = {}
}

variable "app_secret_env" {
  description = "Secret environment variables for the service."
  type        = map(string)
  sensitive   = true
  default     = {}
}

variable "secret_recovery_window_days" {
  description = "Secrets Manager recovery window in days."
  type        = number
  default     = 7
}

variable "ssh_ingress_cidrs" {
  description = "Deprecated compatibility variable. ECS services do not open SSH."
  type        = list(string)
  default     = []
}

variable "ec2_key_name" {
  description = "Deprecated compatibility variable. ECS services do not create EC2 instances."
  type        = string
  default     = ""
}

variable "container_extra_args" {
  description = "Deprecated compatibility variable from the EC2 bootstrap layout."
  type        = string
  default     = ""
}

variable "container_command_args" {
  description = "Deprecated compatibility variable from the EC2 bootstrap layout. Use container_command instead."
  type        = string
  default     = ""
}

# auth-service Terraform

This stack follows the MSA Terraform contract and provisions the auth-service ECS/Fargate Blue/Green baseline.

It creates:

- Optional dedicated VPC with public/private subnets when `create_vpc = true`
- Internal ALB with production and CodeDeploy test listeners when used in shared VPC mode
- Blue and green ALB target groups
- ECS cluster, task definition, and ECS service using `CODE_DEPLOY`
- CodeDeploy ECS application and deployment group
- ECR repository with lifecycle policy
- CloudWatch log group
- Secrets Manager secret for sensitive environment variables
- Optional private RDS MySQL when `enable_mysql = true`

## Recommended Topology

The platform default is a shared VPC with internal ALB plus Route53 private DNS.

- Client traffic enters through the gateway public ALB only.
- `auth-service` receives service-to-service traffic through `auth.internal.platform.local`.
- ALB ingress should be restricted by source security groups, not internet CIDRs.

Recommended variable pattern:

```hcl
create_vpc = false

existing_vpc_id             = "vpc-..."
existing_public_subnet_ids  = ["subnet-public-a", "subnet-public-c"]
existing_private_subnet_ids = ["subnet-app-a", "subnet-app-c"]
existing_vpc_cidr           = "10.0.0.0/16"

alb_internal                          = true
alb_ingress_source_security_group_ids = ["sg-gateway-ecs-tasks"]
private_dns_zone_id                   = "Z123456789PRIVATE"
private_dns_name                      = "auth.internal.platform.local"
```

Set downstream URLs such as `USER_SERVICE_BASE_URL` to private DNS endpoints, not public domains.

## Apply Infrastructure

```bash
cp infra/terraform/terraform.tfvars.example infra/terraform/terraform.tfvars
cd infra/terraform
terraform init
terraform plan
terraform apply
```

## Build And Push Image

```bash
AWS_REGION=ap-northeast-2
ECR_REPO="$(terraform output -raw ecr_repository_url)"

aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$(echo "$ECR_REPO" | cut -d/ -f1)"

docker build \
  -f ../../docker/Dockerfile \
  -t "$ECR_REPO:2026.04.17-001" \
  ../..

docker push "$ECR_REPO:2026.04.17-001"
```

Use immutable release tags in production. Avoid `latest` for rollback-sensitive deployments.

## Deploy / Rollback

Terraform provisions the Blue/Green infrastructure. Runtime releases should be executed by CodeDeploy with a new ECS task definition revision and AppSpec.

Minimal AppSpec shape:

```yaml
version: 0.0
Resources:
  - TargetService:
      Type: AWS::ECS::Service
      Properties:
        TaskDefinition: "<new-task-definition-arn>"
        LoadBalancerInfo:
          ContainerName: "auth-service"
          ContainerPort: 8081
```

CodeDeploy shifts traffic from the blue target group to the green target group using:

- production listener: `terraform output service_url`
- test listener: `terraform output test_url`
- app: `terraform output codedeploy_app_name`
- deployment group: `terraform output codedeploy_deployment_group_name`

Rollback is handled by CodeDeploy auto rollback on deployment failure or stopped alarm. Manual rollback is a deployment using the previous task definition ARN.

## GitHub Actions CD

`.github/workflows/cd.yml` builds the service image, pushes it to Amazon ECR, registers a new ECS task definition revision, and creates a CodeDeploy ECS deployment.

Required GitHub secret:

- `AWS_ROLE_ARN`: IAM role assumed by GitHub OIDC
- `GH_TOKEN`: GitHub Packages read token for private platform dependencies

Required GitHub vars when Terraform defaults are changed:

- `AWS_REGION`
- `ECR_REPOSITORY_NAME`
- `ECS_CLUSTER_NAME`
- `ECS_SERVICE_NAME`
- `ECS_CONTAINER_NAME`
- `ECS_CONTAINER_PORT`
- `CODEDEPLOY_APPLICATION_NAME`
- `CODEDEPLOY_DEPLOYMENT_GROUP_NAME`

The workflow defaults match this stack's default `prod-auth-service` resource names. If Terraform uses a remote backend, these values can be automated from outputs later. Until then, keep GitHub vars in sync with Terraform outputs.

The assumed role needs permission to push to the service ECR repository, describe the current ECS service and task definition, register a new ECS task definition revision, pass the existing task and execution roles, and create/read CodeDeploy deployments.

## Notes

- `terraform apply` changes infrastructure. It does not replace CodeDeploy as the release mechanism.
- Shared VPC mode uses `create_vpc = false` plus `existing_*`, `alb_internal`, and `private_dns_*`.
- Route53 private DNS is optional in Terraform, but this service should use it in production.
- Secrets are stored in Terraform state. Use an encrypted remote backend before production use.
- RDS deletion protection is enabled by default for database-owning services.

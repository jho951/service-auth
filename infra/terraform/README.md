# auth-server Terraform

This stack follows the MSA Terraform contract and provisions an ECS/Fargate Blue/Green deployment baseline.

It creates:

- VPC with public subnets, private subnets, Internet Gateway, and NAT Gateway
- Public ALB with production and CodeDeploy test listeners
- Blue and green ALB target groups
- ECS cluster, task definition, and ECS service using `CODE_DEPLOY`
- CodeDeploy ECS application and deployment group
- ECR repository with lifecycle policy
- CloudWatch log group
- Secrets Manager secret for sensitive environment variables
- Optional private RDS MySQL when `enable_mysql = true`

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

## Notes

- `terraform apply` changes infrastructure. It does not replace CodeDeploy as the release mechanism.
- Secrets are stored in Terraform state. Use an encrypted remote backend before production use.
- RDS deletion protection is enabled by default for database-owning services.

#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: deploy-stack-service.sh <dev|prod>" >&2
  exit 1
}

DEPLOY_ENVIRONMENT="${1:-}"
[[ "$DEPLOY_ENVIRONMENT" == "dev" || "$DEPLOY_ENVIRONMENT" == "prod" ]] || usage

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CONTRACT_DIR="${CONTRACT_DIR:-$REPO_ROOT/.contract}"
SERVICE_NAME="${SERVICE_NAME:-}"
SYNC_EC2_ENV_TEMPLATE_SCRIPT="$SCRIPT_DIR/sync-ec2-env-template.sh"
SYNC_EC2_SCHEMA_SCRIPT="$SCRIPT_DIR/sync-ec2-schema.sh"

if [[ -z "$SERVICE_NAME" && -f "$REPO_ROOT/contract.lock.yml" ]]; then
  SERVICE_NAME="$(awk '/^[[:space:]]*name:/ { print $2; exit }' "$REPO_ROOT/contract.lock.yml")"
fi
SERVICE_NAME="${SERVICE_NAME:-$(basename "$REPO_ROOT")}"

[[ -d "$CONTRACT_DIR" ]] || { echo "Contract dir not found: $CONTRACT_DIR" >&2; exit 1; }
[[ -f "$SYNC_EC2_ENV_TEMPLATE_SCRIPT" ]] || { echo "Sync script not found: $SYNC_EC2_ENV_TEMPLATE_SCRIPT" >&2; exit 1; }
[[ -f "$SYNC_EC2_SCHEMA_SCRIPT" ]] || { echo "Sync script not found: $SYNC_EC2_SCHEMA_SCRIPT" >&2; exit 1; }

bash "$SYNC_EC2_ENV_TEMPLATE_SCRIPT"
bash "$SYNC_EC2_SCHEMA_SCRIPT"

exec "$CONTRACT_DIR/scripts/deploy-service-via-bundle.sh" \
  --service "$SERVICE_NAME" \
  --environment "$DEPLOY_ENVIRONMENT"

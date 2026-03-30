#!/bin/bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_PROJECT_NAME="auth-service"

ACTION=${1:-up}
ENV=${2:-dev}
TARGET=${3:-all}
case "$TARGET" in
  all|app)
    ;;
  *)
    echo "Invalid target: $TARGET"
    echo "Usage: ./scripts/run.docker.sh [up|down] [dev|prod] [all|app]"
    exit 1
    ;;
esac

case "$ACTION" in
  up|down)
    ;;
  *)
    echo "Invalid action: $ACTION"
    echo "Usage: ./scripts/run.docker.sh [up|down] [dev|prod] [all|app]"
    exit 1
    ;;
esac

case "$ENV" in
  dev)
    COMPOSE_FILE="$PROJECT_ROOT/compose.auth-service.dev.yml"
    ;;
  prod)
    COMPOSE_FILE="$PROJECT_ROOT/compose.auth-service.prod.yml"
    ;;
  *)
    echo "Invalid env: $ENV"
    echo "Usage: ./scripts/run.docker.sh [up|down] [dev|prod] [all|app]"
    exit 1
    ;;
esac

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Compose file not found: $COMPOSE_FILE"
  exit 1
fi

echo "Environment: $ENV"
echo "Target: $TARGET"
echo "Action: $ACTION"
echo "Using Docker Compose file: $COMPOSE_FILE"

ensure_network() {
  local network_name="$1"
  if [[ -z "${network_name:-}" ]]; then
    return 0
  fi
  if ! docker network inspect "$network_name" >/dev/null 2>&1; then
    echo "Creating external network: $network_name"
    docker network create "$network_name" >/dev/null
  fi
}

if [[ "$ACTION" == "up" ]]; then
  ensure_network "msa-service-shared"
  ensure_network "redis-core"
  docker compose -p "$COMPOSE_PROJECT_NAME" -f "$COMPOSE_FILE" up --build -d
else
  docker compose -p "$COMPOSE_PROJECT_NAME" -f "$COMPOSE_FILE" down --remove-orphans -v
fi

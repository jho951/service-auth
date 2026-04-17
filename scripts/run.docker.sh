#!/bin/bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_PROJECT_NAME="auth-service"

ACTION=${1:-up}
ENV=${2:-dev}
TARGET=${3:-all}

if [[ $# -gt 3 ]]; then
  echo "Invalid option: ${4}"
  echo "Usage: ./scripts/run.docker.sh [up|down] [dev|prod] [all|app]"
  exit 1
fi

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
  dev|prod)
    COMPOSE_FILE="$PROJECT_ROOT/docker/$ENV/compose.yml"
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

prepare_env_file() {
  local env="$1"
  local source_env_file="$PROJECT_ROOT/.env.$env"

  if [[ -f "$source_env_file" ]]; then
    echo "$source_env_file"
    return 0
  fi

  echo "Env file not found: $source_env_file" >&2
  echo "Create it from .env.example before running Docker." >&2
  exit 1
}

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

COMPOSE_ENV_FILE="$(prepare_env_file "$ENV")"
echo "Using env file: $COMPOSE_ENV_FILE"

if [[ "$ACTION" == "up" ]]; then
  SHARED_NETWORK="${SHARED_SERVICE_NETWORK:-${BACKEND_SHARED_NETWORK:-${SERVICE_SHARED_NETWORK:-service-backbone-shared}}}"
  ensure_network "$SHARED_NETWORK"
  AUTH_ENV_FILE="$COMPOSE_ENV_FILE" SHARED_SERVICE_NETWORK="$SHARED_NETWORK" BACKEND_SHARED_NETWORK="$SHARED_NETWORK" SERVICE_SHARED_NETWORK="$SHARED_NETWORK" \
    docker compose -p "$COMPOSE_PROJECT_NAME" -f "$COMPOSE_FILE" up --build -d
else
  if [[ "$ENV" == "dev" ]]; then
    AUTH_ENV_FILE="$COMPOSE_ENV_FILE" docker compose -p "$COMPOSE_PROJECT_NAME" -f "$COMPOSE_FILE" down --remove-orphans -v
  else
    AUTH_ENV_FILE="$COMPOSE_ENV_FILE" docker compose -p "$COMPOSE_PROJECT_NAME" -f "$COMPOSE_FILE" down --remove-orphans
  fi
fi

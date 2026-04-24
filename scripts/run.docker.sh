#!/bin/bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_PROJECT_NAME="auth-service"
BASE_COMPOSE_FILE="$PROJECT_ROOT/docker/compose.yml"

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
    ENV_COMPOSE_FILE="$PROJECT_ROOT/docker/$ENV/compose.yml"
    BUILD_COMPOSE_FILE="$PROJECT_ROOT/docker/compose.build.yml"
    ;;
  *)
    echo "Invalid env: $ENV"
    echo "Usage: ./scripts/run.docker.sh [up|down] [dev|prod] [all|app]"
    exit 1
    ;;
esac

if [[ ! -f "$BASE_COMPOSE_FILE" ]]; then
  echo "Base Compose file not found: $BASE_COMPOSE_FILE"
  exit 1
fi

if [[ ! -f "$ENV_COMPOSE_FILE" ]]; then
  echo "Compose file not found: $ENV_COMPOSE_FILE"
  exit 1
fi

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

gradle_property() {
  local key="$1"
  local gradle_properties="${HOME}/.gradle/gradle.properties"
  [[ -f "$gradle_properties" ]] || return 0
  awk -F= -v key="$key" '$1 == key { print $2; exit }' "$gradle_properties"
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

if [[ -z "${GH_TOKEN:-}" ]]; then
  GH_TOKEN="$(gradle_property githubPackagesToken)"
  [[ -n "$GH_TOKEN" ]] || GH_TOKEN="$(gradle_property githubToken)"
  [[ -n "$GH_TOKEN" ]] || GH_TOKEN="$(gradle_property ghToken)"
  [[ -n "$GH_TOKEN" ]] || GH_TOKEN="$(gradle_property gh_token)"
  export GH_TOKEN
fi

if [[ -z "${GITHUB_ACTOR:-}" ]]; then
  GITHUB_ACTOR="$(gradle_property githubPackagesUsername)"
  [[ -n "$GITHUB_ACTOR" ]] || GITHUB_ACTOR="$(gradle_property githubUsername)"
  [[ -n "$GITHUB_ACTOR" ]] || GITHUB_ACTOR="jho951"
  export GITHUB_ACTOR
fi

if [[ -z "${GITHUB_TOKEN:-}" && -n "${GH_TOKEN:-}" ]]; then
  export GITHUB_TOKEN="$GH_TOKEN"
fi

if [[ "$ACTION" == "up" ]]; then
  SHARED_NETWORK="${SHARED_SERVICE_NETWORK:-${BACKEND_SHARED_NETWORK:-${SERVICE_SHARED_NETWORK:-service-backbone-shared}}}"
  ensure_network "$SHARED_NETWORK"
  if [[ "$ENV" == "prod" ]]; then
    AUTH_ENV_FILE="$COMPOSE_ENV_FILE" SHARED_SERVICE_NETWORK="$SHARED_NETWORK" BACKEND_SHARED_NETWORK="$SHARED_NETWORK" SERVICE_SHARED_NETWORK="$SHARED_NETWORK" \
      docker compose --env-file "$COMPOSE_ENV_FILE" -p "$COMPOSE_PROJECT_NAME" -f "$BASE_COMPOSE_FILE" -f "$ENV_COMPOSE_FILE" pull
    AUTH_ENV_FILE="$COMPOSE_ENV_FILE" SHARED_SERVICE_NETWORK="$SHARED_NETWORK" BACKEND_SHARED_NETWORK="$SHARED_NETWORK" SERVICE_SHARED_NETWORK="$SHARED_NETWORK" \
      docker compose --env-file "$COMPOSE_ENV_FILE" -p "$COMPOSE_PROJECT_NAME" -f "$BASE_COMPOSE_FILE" -f "$ENV_COMPOSE_FILE" up -d
  else
    AUTH_ENV_FILE="$COMPOSE_ENV_FILE" SHARED_SERVICE_NETWORK="$SHARED_NETWORK" BACKEND_SHARED_NETWORK="$SHARED_NETWORK" SERVICE_SHARED_NETWORK="$SHARED_NETWORK" \
      docker compose --env-file "$COMPOSE_ENV_FILE" -p "$COMPOSE_PROJECT_NAME" -f "$BASE_COMPOSE_FILE" -f "$ENV_COMPOSE_FILE" -f "$BUILD_COMPOSE_FILE" up --build -d
  fi
else
  if [[ "$ENV" == "dev" ]]; then
    AUTH_ENV_FILE="$COMPOSE_ENV_FILE" docker compose --env-file "$COMPOSE_ENV_FILE" -p "$COMPOSE_PROJECT_NAME" -f "$BASE_COMPOSE_FILE" -f "$ENV_COMPOSE_FILE" down --remove-orphans -v
  else
    AUTH_ENV_FILE="$COMPOSE_ENV_FILE" docker compose --env-file "$COMPOSE_ENV_FILE" -p "$COMPOSE_PROJECT_NAME" -f "$BASE_COMPOSE_FILE" -f "$ENV_COMPOSE_FILE" down --remove-orphans
  fi
fi

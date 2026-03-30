#!/bin/bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DOCKER_DIR="$PROJECT_ROOT/docker"

ACTION=${1:-up}
ENV=${2:-dev}
TARGET=${3:-all}
SOURCE_ENV_FILE="$PROJECT_ROOT/.env.$ENV"
MSA_SHARED_NETWORK="${MSA_SHARED_NETWORK:-msa-shared}"

APP_COMPOSE_FILES=(
  "$DOCKER_DIR/docker-compose.app.yml"
  "$DOCKER_DIR/services/mysql/$ENV/docker-compose.mysql.yml"
)

case "$TARGET" in
  all|app)
    COMPOSE_FILES=("${APP_COMPOSE_FILES[@]}")
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

[[ -n "${ES_COMPOSE:-}" ]] && COMPOSE_FILES+=("$ES_COMPOSE")

if [[ ! -f "$SOURCE_ENV_FILE" ]]; then
  echo "Source env file not found: $SOURCE_ENV_FILE"
  exit 1
fi

docker network inspect "$MSA_SHARED_NETWORK" >/dev/null 2>&1 || docker network create "$MSA_SHARED_NETWORK" >/dev/null

missing_files=()
for f in "${COMPOSE_FILES[@]}"; do
  if [[ -z "${f:-}" ]]; then
    missing_files+=("<empty-entry-in-COMPOSE_FILES>")
    continue
  fi
  [[ -f "$f" ]] || missing_files+=("$f")
done

if (( ${#missing_files[@]} > 0 )); then
  echo "Missing compose files:"
  for f in "${missing_files[@]}"; do
    echo "  $f"
  done
  echo "Check ENV='$ENV', TARGET='$TARGET', and optional ES_COMPOSE."
  exit 1
fi

echo "Environment: $ENV"
echo "Target: $TARGET"
echo "Action: $ACTION"
echo "Using source env file: $SOURCE_ENV_FILE"
echo "Using Docker Compose files:"
for f in "${COMPOSE_FILES[@]}"; do
  echo "  $f"
done

COMPOSE_ARGS=()
for f in "${COMPOSE_FILES[@]}"; do
  COMPOSE_ARGS+=("-f" "$f")
done

if [[ "$ACTION" == "up" ]]; then
  ENV_FILE_PATH="$SOURCE_ENV_FILE" MSA_SHARED_NETWORK="$MSA_SHARED_NETWORK" docker compose --env-file "$SOURCE_ENV_FILE" \
    "${COMPOSE_ARGS[@]}" up --build -d
else
  ENV_FILE_PATH="$SOURCE_ENV_FILE" MSA_SHARED_NETWORK="$MSA_SHARED_NETWORK" docker compose --env-file "$SOURCE_ENV_FILE" \
    "${COMPOSE_ARGS[@]}" down --remove-orphans -v
fi

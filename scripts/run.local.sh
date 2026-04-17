#!/bin/bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ENV=${1:-local}
DEFAULT_PROFILE="$ENV"

if [[ "$ENV" == "local" ]]; then
  DEFAULT_PROFILE="dev"
fi

ENV_FILE="$PROJECT_ROOT/.env.$ENV"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Source env file not found: $ENV_FILE"
  if [[ "$ENV" == "local" ]]; then
    echo "Create .env.local from docs/examples/env.local.example before running locally."
  fi
  exit 1
fi

echo "Environment: $ENV"
echo "Using source env file: $ENV_FILE"

set -a
source "$ENV_FILE"
set +a

ACTIVE_PROFILE="${SPRING_PROFILES_ACTIVE:-$DEFAULT_PROFILE}"

cd "$PROJECT_ROOT"
echo "Spring profile: $ACTIVE_PROFILE"
echo "Swagger UI: http://localhost:${SERVER_PORT:-8082}/swagger-ui.html"
./gradlew :app:bootRun --args="--spring.profiles.active=$ACTIVE_PROFILE"

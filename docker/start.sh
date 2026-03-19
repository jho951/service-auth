#!/bin/bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
GENERATED_ENV_DIR="$SCRIPT_DIR/.generated"

ENV=${1:-dev}
TARGET=${2:-all}
SOURCE_ENV_FILE="$PROJECT_ROOT/.env.$ENV"
GENERATED_ENV_FILE="$GENERATED_ENV_DIR/.env.$ENV"

APP_COMPOSE_FILES=(
  "$SCRIPT_DIR/docker-compose.app.yml"
  "$SCRIPT_DIR/services/mysql/$ENV/docker-compose.mysql.yml"
)

case "$TARGET" in
  all|app)
    COMPOSE_FILES=("${APP_COMPOSE_FILES[@]}")
    ;;
  *)
    echo "❌ Invalid target: $TARGET"
    echo "ℹ️  Usage: ./docker/start.sh [dev|prod] [all|app]"
    exit 1
    ;;
esac

[[ -n "${ES_COMPOSE:-}" ]] && COMPOSE_FILES+=("$ES_COMPOSE")

generate_env_file() {
  mkdir -p "$GENERATED_ENV_DIR"

  if [[ ! -f "$SOURCE_ENV_FILE" ]]; then
    echo "❌ Source ENV file not found: $SOURCE_ENV_FILE"
    exit 1
  fi

  cp "$SOURCE_ENV_FILE" "$GENERATED_ENV_FILE"

  if [[ ! -s "$GENERATED_ENV_FILE" ]]; then
    echo "❌ Generated ENV file is empty: $GENERATED_ENV_FILE"
    exit 1
  fi
}

generate_env_file

if [[ ! -f "$SOURCE_ENV_FILE" ]]; then
  echo "❌ Source ENV file not found: $SOURCE_ENV_FILE"
  exit 1
fi

# compose 파일 존재 확인 (빈 값 방지)
missing_files=()
for f in "${COMPOSE_FILES[@]}"; do
  if [[ -z "${f:-}" ]]; then
    missing_files+=("<empty-entry-in-COMPOSE_FILES>")
    continue
  fi
  [[ -f "$f" ]] || missing_files+=("$f")
done

if (( ${#missing_files[@]} > 0 )); then
  echo "❌ Missing compose files:"
  for f in "${missing_files[@]}"; do echo "  $f"; done
  echo "ℹ️  Check ENV='$ENV', directory names, and optional ES_COMPOSE."
  exit 1
fi

echo "✅ Environment: $ENV"
echo "✅ Target: $TARGET"
echo "✅ Using source ENV file: $SOURCE_ENV_FILE"
echo "✅ Generated ENV snapshot: $GENERATED_ENV_FILE"
echo "✅ Using Docker Compose files:"
for f in "${COMPOSE_FILES[@]}"; do echo "  $f"; done

# 실행
docker compose --env-file "$SOURCE_ENV_FILE" \
  $(for f in "${COMPOSE_FILES[@]}"; do echo -n "-f $f "; done) \
  up --build -d

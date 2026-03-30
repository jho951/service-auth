#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="${1:-$PROJECT_ROOT/.env.prod}"
MIGRATION_SQL="$PROJECT_ROOT/db/migrations/2026-03-27_drop_auth_social_accounts.sql"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Env file not found: $ENV_FILE"
  exit 1
fi
if [[ ! -f "$MIGRATION_SQL" ]]; then
  echo "Migration SQL not found: $MIGRATION_SQL"
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${MYSQL_HOST:?MYSQL_HOST is required}"
: "${MYSQL_PORT:?MYSQL_PORT is required}"
: "${MYSQL_DB:?MYSQL_DB is required}"
: "${MYSQL_USER:?MYSQL_USER is required}"
: "${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}"

MYSQL_BIN="$(command -v mysql || true)"
if [[ -z "$MYSQL_BIN" && -x "/opt/homebrew/opt/mysql-client/bin/mysql" ]]; then
  MYSQL_BIN="/opt/homebrew/opt/mysql-client/bin/mysql"
fi
if [[ -z "$MYSQL_BIN" ]]; then
  echo "mysql client is required but not found"
  exit 1
fi

echo "Applying migration to ${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DB}"
"$MYSQL_BIN" \
  --host="$MYSQL_HOST" \
  --port="$MYSQL_PORT" \
  --user="$MYSQL_USER" \
  --password="$MYSQL_PASSWORD" \
  "$MYSQL_DB" < "$MIGRATION_SQL"

echo "Migration applied: drop auth_social_accounts"

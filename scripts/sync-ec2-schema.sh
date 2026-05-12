#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SOURCE_SCHEMA="$REPO_ROOT/db/schema.sql"
TARGET_SCHEMA="$REPO_ROOT/deploy/ec2/db/schema.sql"

[[ -f "$SOURCE_SCHEMA" ]] || { echo "Source schema not found: $SOURCE_SCHEMA" >&2; exit 1; }
mkdir -p "$(dirname "$TARGET_SCHEMA")"
cp "$SOURCE_SCHEMA" "$TARGET_SCHEMA"
echo "Synced EC2 bundle schema: $TARGET_SCHEMA"

#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./run-codex-auth-hybrid-migration.sh /path/to/Auth-server
# Example:
#   ./run-codex-auth-hybrid-migration.sh ~/workspace/Auth-server
REPO_DIR="${1:-.}"
PROMPT_FILE="${PROMPT_FILE:-$(dirname "$0")/codex-auth-hybrid-migration-prompt.md}"

if ! command -v codex >/dev/null 2>&1; then
  echo "Error: codex CLI is not installed or not in PATH." >&2
  exit 1
fi

if [[ ! -d "$REPO_DIR" ]]; then
  echo "Error: repository directory not found: $REPO_DIR" >&2
  exit 1
fi

if [[ ! -f "$PROMPT_FILE" ]]; then
  echo "Error: prompt file not found: $PROMPT_FILE" >&2
  exit 1
fi

cd "$REPO_DIR"

echo "Repository: $(pwd)"
echo "Prompt file: $PROMPT_FILE"
echo "Launching Codex..."

auth_prompt="$(cat "$PROMPT_FILE")"

exec codex "$auth_prompt"

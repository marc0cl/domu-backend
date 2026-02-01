#!/usr/bin/env bash
set -euo pipefail

if [[ -f .env ]]; then
  # Export variables from .env without echoing secrets.
  set -a
  source .env
  set +a
else
  echo "Warning: .env not found; using existing environment variables." >&2
fi

exec ./gradlew run

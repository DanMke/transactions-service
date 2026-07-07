#!/usr/bin/env bash
set -euo pipefail

# Build the application image and start the full stack (app + Postgres).
# Uses the Docker Compose v2 CLI ("docker compose", with a space).
docker compose up --build "$@"

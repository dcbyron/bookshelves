#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DB_NAME="${BOOKSHELVES_E2E_DB_NAME:-bookshelves_e2e_vue}"
DB_USER="${BOOKSHELVES_DB_USERNAME:-bookshelves}"
DB_PASSWORD="${BOOKSHELVES_DB_PASSWORD:-bookshelves}"
DB_PORT="${BOOKSHELVES_E2E_DB_PORT:-55432}"
CONTAINER_NAME="${BOOKSHELVES_E2E_DB_CONTAINER:-bookshelves-e2e-vue-db}"

. "$ROOT_DIR/scripts/e2e_postgres.sh"
trap 'stop_e2e_postgres "$CONTAINER_NAME"' EXIT

start_e2e_postgres "$CONTAINER_NAME" "$DB_PORT" "$DB_NAME" "$DB_USER" "$DB_PASSWORD"

export BOOKSHELVES_DB_URL="${BOOKSHELVES_DB_URL:-jdbc:postgresql://localhost:$DB_PORT/$DB_NAME}"
export BOOKSHELVES_DB_USERNAME="$DB_USER"
export BOOKSHELVES_DB_PASSWORD="$DB_PASSWORD"
export BOOKSHELVES_ALLOWED_ORIGINS="${BOOKSHELVES_ALLOWED_ORIGINS:-http://localhost:5173}"
export PORT="${PORT:-18080}"

cd "$ROOT_DIR/backend"
mvn spring-boot:run

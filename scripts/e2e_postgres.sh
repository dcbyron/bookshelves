#!/usr/bin/env bash
set -euo pipefail

start_e2e_postgres() {
  local container_name="$1"
  local host_port="$2"
  local db_name="$3"
  local db_user="${4:-bookshelves}"
  local db_password="${5:-bookshelves}"

  docker rm -f "$container_name" >/dev/null 2>&1 || true
  docker run --rm --name "$container_name" \
    -e POSTGRES_DB="$db_name" \
    -e POSTGRES_USER="$db_user" \
    -e POSTGRES_PASSWORD="$db_password" \
    -p "${host_port}:5432" \
    -d postgres:17 >/dev/null

  for _ in $(seq 1 60); do
    if docker exec "$container_name" pg_isready -U "$db_user" -d "$db_name" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  echo "Timed out waiting for PostgreSQL container $container_name" >&2
  return 1
}

seed_e2e_schema() {
  local container_name="$1"
  local db_name="$2"
  local db_user="$3"
  local root_dir="$4"

  local seed_file
  for seed_file in \
    "$root_dir/backend/src/main/resources/db/migration/V1__baseline_schema.sql" \
    "$root_dir/backend/src/main/resources/db/migration/V2__seed_default_reference_data.sql" \
    "$root_dir/backend/src/main/resources/db/migration/V3__add_last_audited_to_books.sql" \
    "$root_dir/backend/src/main/resources/db/migration/V4__add_digital_to_books.sql" \
    "$root_dir/backend/src/main/resources/db/migration/V5__rename_publication_to_publisher_and_add_publication_city.sql" \
    "$root_dir/backend/src/main/resources/db/migration/V6__add_dustjacket_slipcase_video_to_books.sql" \
    "$root_dir/backend/src/main/resources/db/migration/V7__reserved_for_public_migration_sequence.sql" \
    "$root_dir/backend/src/main/resources/db/migration/V8__add_audited_to_shelves.sql" \
    "$root_dir/backend/src/main/resources/db/migration/V9__add_shelf_coordinate_audits.sql" \
    "$root_dir/backend/src/main/resources/db/migration/V10__add_book_imprint_and_first_published.sql"; do
    local seeded=false
    for _ in $(seq 1 30); do
      if docker exec -i "$container_name" psql -v ON_ERROR_STOP=1 -U "$db_user" -d "$db_name" \
        < "$seed_file"; then
        seeded=true
        break
      fi
      sleep 1
    done
    if [ "$seeded" != true ]; then
      echo "Timed out seeding $seed_file into $db_name" >&2
      return 1
    fi
  done
}

stop_e2e_postgres() {
  local container_name="$1"
  docker rm -f "$container_name" >/dev/null 2>&1 || true
}

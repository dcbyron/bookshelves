#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_EXPORT_BASE="${BOOKSHELVES_EXPORT_BASE:-$ROOT_DIR/../exports}"

DB_HOST="${BOOKSHELVES_DB_HOST:-localhost}"
DB_PORT="${BOOKSHELVES_DB_PORT:-5432}"
DB_NAME="${BOOKSHELVES_DB_NAME:-bookshelves}"
DB_USER="${BOOKSHELVES_DB_USERNAME:-bookshelves}"
DB_PASSWORD="${BOOKSHELVES_DB_PASSWORD:-bookshelves}"

EXPORT_BASE="$DEFAULT_EXPORT_BASE"
INCLUDE_PORTABLE=1
INCLUDE_DUMP=1
COMPRESS_PORTABLE=0

usage() {
  cat <<'EOF'
Usage: scripts/export_bookshelves.sh [options]

Create a timestamped export directory containing:
- portable JSONL table exports
- an optional PostgreSQL custom-format dump
- a manifest.json describing the export

Options:
  --out DIR           Write timestamped exports beneath DIR
  --portable-only     Export only JSONL files and manifest.json
  --dump-only         Export only the PostgreSQL custom dump and manifest.json
  --compress          Gzip the JSONL files after export
  --help              Show this help text

Environment:
  BOOKSHELVES_EXPORT_BASE
  BOOKSHELVES_DB_HOST
  BOOKSHELVES_DB_PORT
  BOOKSHELVES_DB_NAME
  BOOKSHELVES_DB_USERNAME
  BOOKSHELVES_DB_PASSWORD

Examples:
  scripts/export_bookshelves.sh
  scripts/export_bookshelves.sh --portable-only --compress
  scripts/export_bookshelves.sh --out /tmp/bookshelves-exports
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --out)
      EXPORT_BASE="${2:?--out requires a directory}"
      shift 2
      ;;
    --portable-only)
      INCLUDE_PORTABLE=1
      INCLUDE_DUMP=0
      shift
      ;;
    --dump-only)
      INCLUDE_PORTABLE=0
      INCLUDE_DUMP=1
      shift
      ;;
    --compress)
      COMPRESS_PORTABLE=1
      shift
      ;;
    --help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ "$INCLUDE_PORTABLE" -eq 0 && "$INCLUDE_DUMP" -eq 0 ]]; then
  echo "Nothing to export: both portable and dump outputs are disabled." >&2
  exit 1
fi

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1" >&2
    exit 1
  fi
}

require_command psql
require_command pg_dump
require_command python3
if [[ "$COMPRESS_PORTABLE" -eq 1 ]]; then
  require_command gzip
fi

export PGPASSWORD="$DB_PASSWORD"
PSQL=(psql -X -v ON_ERROR_STOP=1 -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME")
PG_DUMP=(pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME")

timestamp="$(date -u +"%Y-%m-%dT%H-%M-%SZ")"
export_dir="$EXPORT_BASE/$timestamp"
mkdir -p "$export_dir"

cleanup_needed=1
cleanup() {
  if [[ "$cleanup_needed" -eq 1 && -d "$export_dir" ]]; then
    rm -rf "$export_dir"
  fi
}
trap cleanup EXIT

query_value() {
  "${PSQL[@]}" -Atqc "$1"
}

export_jsonl() {
  local filename="$1"
  local sql="$2"
  local target="$export_dir/$filename"

  "${PSQL[@]}" -c "\\copy ($sql) TO '$target'"

  if [[ "$COMPRESS_PORTABLE" -eq 1 ]]; then
    gzip -n "$target"
    printf '%s.gz' "$target"
  else
    printf '%s' "$target"
  fi
}

schema_version="$(query_value "select coalesce(max(version), '0') from flyway_schema_history where success")"

count_book="$(query_value "select count(*) from book")"
count_collection="$(query_value "select count(*) from collection")"
count_shelf="$(query_value "select count(*) from shelf")"
count_shelf_coordinate_audit="$(query_value "select count(*) from shelf_coordinate_audit")"
count_book_collection="$(query_value "select count(*) from book_collection")"

if [[ "$INCLUDE_PORTABLE" -eq 1 ]]; then
  # Export all current book columns so the portable backup stays aligned with
  # schema additions like last_audited and digital without needing manual edits.
  book_file="$(export_jsonl "book.jsonl" "select row_to_json(t) from (select * from book order by id) t")"
  collection_file="$(export_jsonl "collection.jsonl" "select row_to_json(t) from (select id, name from collection order by id) t")"
  shelf_file="$(export_jsonl "shelf.jsonl" "select row_to_json(t) from (select id, name, audited from shelf order by id) t")"
  shelf_coordinate_audit_file="$(export_jsonl "shelf_coordinate_audit.jsonl" "select row_to_json(t) from (select shelf_id, shelf_coords, audited from shelf_coordinate_audit order by shelf_id, shelf_coords) t")"
  book_collection_file="$(export_jsonl "book_collection.jsonl" "select row_to_json(t) from (select book_id, collection_id from book_collection order by book_id, collection_id) t")"
else
  book_file=""
  collection_file=""
  shelf_file=""
  shelf_coordinate_audit_file=""
  book_collection_file=""
fi

if [[ "$INCLUDE_DUMP" -eq 1 ]]; then
  dump_file="$export_dir/bookshelves.dump"
  "${PG_DUMP[@]}" --format=custom --file="$dump_file"
else
  dump_file=""
fi

export EXPORT_DIR="$export_dir"
export EXPORT_TIMESTAMP="$timestamp"
export EXPORT_SCHEMA_VERSION="$schema_version"
export EXPORT_DB_HOST="$DB_HOST"
export EXPORT_DB_PORT="$DB_PORT"
export EXPORT_DB_NAME="$DB_NAME"
export EXPORT_DB_USER="$DB_USER"
export EXPORT_INCLUDE_PORTABLE="$INCLUDE_PORTABLE"
export EXPORT_INCLUDE_DUMP="$INCLUDE_DUMP"
export EXPORT_COMPRESS_PORTABLE="$COMPRESS_PORTABLE"
export EXPORT_COUNT_BOOK="$count_book"
export EXPORT_COUNT_COLLECTION="$count_collection"
export EXPORT_COUNT_SHELF="$count_shelf"
export EXPORT_COUNT_SHELF_COORDINATE_AUDIT="$count_shelf_coordinate_audit"
export EXPORT_COUNT_BOOK_COLLECTION="$count_book_collection"

python3 <<'PY'
import json
import os
from pathlib import Path

export_dir = Path(os.environ["EXPORT_DIR"])

files = []
for path in sorted(export_dir.iterdir()):
    if path.name == "manifest.json":
        continue
    if ".jsonl" in path.name:
        file_format = "jsonl"
    elif path.suffix == ".dump":
        file_format = "pg_dump_custom"
    else:
        file_format = path.suffix.lstrip(".") or "unknown"
    files.append(
        {
            "name": path.name,
            "format": file_format,
            "sizeBytes": path.stat().st_size,
        }
    )

manifest = {
    "application": "Bookshelves",
    "database": {
        "host": os.environ["EXPORT_DB_HOST"],
        "port": int(os.environ["EXPORT_DB_PORT"]),
        "name": os.environ["EXPORT_DB_NAME"],
        "username": os.environ["EXPORT_DB_USER"],
    },
    "exportedAt": os.environ["EXPORT_TIMESTAMP"],
    "schemaVersion": os.environ["EXPORT_SCHEMA_VERSION"],
    "portableFormat": {
        "type": "jsonl",
        "description": "One JSON object per line, one file per table.",
        "compressed": os.environ["EXPORT_COMPRESS_PORTABLE"] == "1",
    },
    "includes": {
        "portableJsonl": os.environ["EXPORT_INCLUDE_PORTABLE"] == "1",
        "postgresDump": os.environ["EXPORT_INCLUDE_DUMP"] == "1",
    },
    "rowCounts": {
        "book": int(os.environ["EXPORT_COUNT_BOOK"]),
        "collection": int(os.environ["EXPORT_COUNT_COLLECTION"]),
        "shelf": int(os.environ["EXPORT_COUNT_SHELF"]),
        "shelf_coordinate_audit": int(os.environ["EXPORT_COUNT_SHELF_COORDINATE_AUDIT"]),
        "book_collection": int(os.environ["EXPORT_COUNT_BOOK_COLLECTION"]),
    },
    "files": files,
}

(export_dir / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
PY

cleanup_needed=0
trap - EXIT

echo "Export written to: $export_dir"
if [[ "$INCLUDE_PORTABLE" -eq 1 ]]; then
  echo "Portable JSONL export files created."
fi
if [[ "$INCLUDE_DUMP" -eq 1 ]]; then
  echo "PostgreSQL custom dump created."
fi

# Backup

Bookshelves can create timestamped export directories with portable JSONL
files, a PostgreSQL custom-format dump, or both.

## From The UI

Open the Backup page, choose the output directory, select the artifact types,
and create the backup.

The backend process must be able to write to the selected output directory.

## From The Script

```sh
scripts/export_bookshelves.sh
```

Useful options:

```sh
scripts/export_bookshelves.sh --portable-only
scripts/export_bookshelves.sh --dump-only
scripts/export_bookshelves.sh --portable-only --compress
scripts/export_bookshelves.sh --out /tmp/bookshelves-exports
```

Each export directory includes `manifest.json`. Depending on selected options,
it can also include:

- `book.jsonl`
- `collection.jsonl`
- `shelf.jsonl`
- `shelf_coordinate_audit.jsonl`
- `book_collection.jsonl`
- `bookshelves.dump`

The JSONL files are intended for inspection and portability. The dump is the
best choice for faithful PostgreSQL restoration.

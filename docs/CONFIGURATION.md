# Configuration

Bookshelves uses backend runtime configuration for database access, CORS,
export script location, and display name.

## Backend Environment

| Variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | `8080` | HTTP port |
| `BOOKSHELVES_APP_NAME` | `Bookshelves` | Frontend display name and browser title |
| `BOOKSHELVES_DB_URL` | `jdbc:postgresql://localhost:5432/bookshelves` | PostgreSQL JDBC URL |
| `BOOKSHELVES_DB_USERNAME` | `bookshelves` | Database username |
| `BOOKSHELVES_DB_PASSWORD` | `bookshelves` | Database password |
| `BOOKSHELVES_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed frontend origin |
| `BOOKSHELVES_EXPORT_SCRIPT_PATH` | empty | Optional explicit export script path |
| `BOOKSHELVES_EXPORT_WORKING_DIRECTORY` | empty | Optional export process working directory |

The frontend loads public display settings from:

```http
GET /api/config
```

Example:

```json
{
  "appName": "Bookshelves"
}
```

## Frontend Environment

| Variable | Default | Purpose |
| --- | --- | --- |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Backend target for Vite dev proxy |
| `PLAYWRIGHT_BASE_URL` | `http://localhost:15173` | Frontend URL used by Playwright |

## Export Script Environment

The export script can run directly with:

```sh
scripts/export_bookshelves.sh
```

It reads:

| Variable | Default |
| --- | --- |
| `BOOKSHELVES_EXPORT_BASE` | `../exports` beside the repo |
| `BOOKSHELVES_DB_HOST` | `localhost` |
| `BOOKSHELVES_DB_PORT` | `5432` |
| `BOOKSHELVES_DB_NAME` | `bookshelves` |
| `BOOKSHELVES_DB_USERNAME` | `bookshelves` |
| `BOOKSHELVES_DB_PASSWORD` | `bookshelves` |

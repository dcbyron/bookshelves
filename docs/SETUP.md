# Setup

## Prerequisites

- Java 25
- Maven 3.9 or newer
- Node.js 20 or newer
- PostgreSQL 17

Docker is optional for local PostgreSQL and required for Testcontainers-backed
backend tests.

## Database

Create a local database and user:

```sh
createdb bookshelves
createuser bookshelves
```

Set a password for the user or provide a PostgreSQL connection URL with your
own credentials.

The backend defaults are:

```text
BOOKSHELVES_DB_URL=jdbc:postgresql://localhost:5432/bookshelves
BOOKSHELVES_DB_USERNAME=bookshelves
BOOKSHELVES_DB_PASSWORD=bookshelves
```

Flyway runs automatically on backend startup. The public seed migration creates
only two generic reference rows: `Uncategorized` shelf and `General`
collection.

## Backend

```sh
cd backend
mvn spring-boot:run
```

Health check:

```sh
curl -i http://localhost:8080/health
```

Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

## Frontend

```sh
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

The Vite dev server proxies `/api` to `http://localhost:8080` by default. To
use a different backend:

```sh
VITE_API_PROXY_TARGET=http://localhost:18080 npm run dev
```

## Compose

The compose file is intentionally small and uses public-safe names:

```sh
docker compose -f infra/compose.yaml up --build
```

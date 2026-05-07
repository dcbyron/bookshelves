# Bookshelves

Bookshelves is a self-hosted book catalog built with Spring Boot, PostgreSQL,
and Vue. It tracks books, shelves, collections, publishers, basic value
metadata, reports, and portable backups.

## Stack

- Backend: Java 25, Spring Boot, JDBC, Flyway, PostgreSQL
- Frontend: Vue 3, Vite, TypeScript, Naive UI
- Tests: JUnit/Testcontainers, Vitest, Playwright

## Quick Start

Start PostgreSQL with the public development defaults:

```sh
docker volume create bookshelves-postgres-data

docker run --rm --name bookshelves-db \
  -e POSTGRES_DB=bookshelves \
  -e POSTGRES_USER=bookshelves \
  -e POSTGRES_PASSWORD=bookshelves \
  -p 5432:5432 \
  -v bookshelves-postgres-data:/var/lib/postgresql/data \
  postgres:17
```

Then run the backend in a second terminal:

```sh
cd backend
mvn spring-boot:run
```

Run the frontend in a third terminal:

```sh
cd frontend
npm install
npm run dev
```

The frontend runs at `http://localhost:5173` and proxies API requests to
`http://localhost:8080`.

Default database settings are public-safe development defaults:

```text
database: bookshelves
username: bookshelves
password: bookshelves
```

See [docs/SETUP.md](docs/SETUP.md) for a full local setup.

## Configuration

Runtime branding is configured by the backend and loaded by the frontend from
`GET /api/config`.

```sh
BOOKSHELVES_APP_NAME="My Library"
```

See [docs/CONFIGURATION.md](docs/CONFIGURATION.md) for environment variables.

## Tests

```sh
cd backend
mvn test
```

```sh
cd frontend
npm test
npm run build
npm run test:e2e
```

Playwright starts the Spring Boot backend and Vite frontend through
`scripts/start_spring_e2e.sh`.

## Documentation

- [Setup](docs/SETUP.md)
- [Configuration](docs/CONFIGURATION.md)
- [Development](docs/DEVELOPMENT.md)
- [API](docs/API.md)
- [Backup](docs/BACKUP.md)

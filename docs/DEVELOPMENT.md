# Development

## Repository Layout

```text
backend/   Spring Boot API, Flyway migrations, JUnit tests
frontend/  Vue app, Vitest tests, Playwright tests
infra/     Docker Compose for the Spring/Vue/PostgreSQL stack
scripts/   Local e2e and export helpers
docs/      Public project documentation
```

The repository intentionally excludes alternate-stack code, generated docs,
exports, logs, private seeds, and private catalog data.

## Backend Checks

```sh
cd backend
mvn test
```

The backend tests use Testcontainers for PostgreSQL integration coverage.

## Frontend Checks

```sh
cd frontend
npm test
npm run build
```

End-to-end tests start PostgreSQL, Spring Boot, and Vite:

```sh
cd frontend
npm run test:e2e
```

## Migrations

Flyway migrations live in `backend/src/main/resources/db/migration`.

`V2__seed_default_reference_data.sql` is intentionally tiny and generic. Do not
add real catalog data, private shelf names, or collection-specific reference
data to migrations.

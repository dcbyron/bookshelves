# API

The backend serves JSON over HTTP. Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

The OpenAPI document is available from Springdoc at:

```text
http://localhost:8080/v3/api-docs
```

## Core Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/health` | Health check |
| `GET` | `/api/config` | Public frontend runtime configuration |
| `GET` | `/api/books` | Search and page books |
| `POST` | `/api/books` | Create a book |
| `GET` | `/api/books/{id}` | Read a book |
| `PUT` | `/api/books/{id}` | Update a book |
| `DELETE` | `/api/books/{id}` | Delete a book |
| `GET` | `/api/collections` | List collections |
| `POST` | `/api/collections` | Create a collection |
| `GET` | `/api/shelves` | List shelves |
| `POST` | `/api/shelves` | Create a shelf |
| `GET` | `/api/reports/missing-location` | Books without shelf placement |
| `POST` | `/api/exports` | Create a backup/export |

Use the generated OpenAPI document for the complete request and response
schema.

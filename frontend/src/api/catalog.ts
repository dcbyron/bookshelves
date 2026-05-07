import type {
  AuthorSummary,
  AppConfig,
  Book,
  BookNavigation,
  BookPage,
  BookQueryParams,
  BookRequest,
  BookSummary,
  Collection,
  CollectionValueReportRow,
  ExportRequest,
  ExportResponse,
  GroupedBooks,
  MissingLocationReport,
  PublisherRenameRequest,
  PublisherRenameResponse,
  PublisherSummary,
  ValueAnalyticsParams,
  ValueDetails,
  ValueSummary,
  Shelf,
  ShelfCoordinateAudit
} from "../types/catalog";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, {
    headers: {
      "Content-Type": "application/json"
    },
    ...init
  });

  if (!response.ok) {
    const contentType = response.headers.get("content-type") ?? "";
    if (contentType.includes("application/json")) {
      const payload = await response.json() as { detail?: string; title?: string; errors?: string[] };
      const message = payload.errors?.length
        ? `${payload.detail ?? payload.title ?? "Request failed"}: ${payload.errors.join(", ")}`
        : (payload.detail ?? payload.title ?? `Request failed with status ${response.status}`);
      throw new Error(message);
    }
    const message = await response.text();
    throw new Error(message || `Request failed with status ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export async function fetchBooks(params: BookQueryParams) {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      searchParams.set(key, Array.isArray(value) ? value.join(",") : String(value));
    }
  });
  const suffix = searchParams.size > 0 ? `?${searchParams.toString()}` : "";
  return request<BookPage>(`/books${suffix}`);
}

export async function fetchAppConfig() {
  return request<AppConfig>("/config");
}

export async function fetchBook(id: string) {
  return request<Book>(`/books/${id}`);
}

export async function fetchBookNavigation(id: string, params: BookQueryParams) {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      searchParams.set(key, Array.isArray(value) ? value.join(",") : String(value));
    }
  });
  const suffix = searchParams.size > 0 ? `?${searchParams.toString()}` : "";
  return request<BookNavigation>(`/books/${id}/navigation${suffix}`);
}

export async function createBook(payload: BookRequest) {
  return request<Book>("/books", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function updateBook(id: string, payload: BookRequest) {
  return request<Book>(`/books/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export async function deleteBook(id: string) {
  await request<void>(`/books/${id}`, { method: "DELETE" });
}

export async function fetchCollections() {
  return request<Collection[]>("/collections");
}

export async function createCollection(payload: Collection) {
  return request<Collection>("/collections", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function updateCollection(id: string, payload: Collection) {
  return request<Collection>(`/collections/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export async function deleteCollection(id: string) {
  await request<void>(`/collections/${id}`, { method: "DELETE" });
}

export async function fetchShelves() {
  return request<Shelf[]>("/shelves");
}

export async function fetchShelfCoordinateAudits() {
  return request<ShelfCoordinateAudit[]>("/shelf-coordinate-audits");
}

export async function createShelf(payload: Shelf) {
  return request<Shelf>("/shelves", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function updateShelf(id: string, payload: Shelf) {
  return request<Shelf>(`/shelves/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export async function updateShelfCoordinateAudit(shelfId: string, shelfCoords: string, audited: boolean) {
  return request<ShelfCoordinateAudit>(
    `/shelf-coordinate-audits/${encodeURIComponent(shelfId)}/${encodeURIComponent(shelfCoords)}`,
    {
      method: "PUT",
      body: JSON.stringify({ audited })
    }
  );
}

export async function deleteShelf(id: string) {
  await request<void>(`/shelves/${id}`, { method: "DELETE" });
}

export async function createExport(payload: ExportRequest) {
  return request<ExportResponse>("/exports", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function fetchAuthors() {
  return request<AuthorSummary[]>("/browse/authors");
}

export async function fetchBooksByAuthor(authorKey: string) {
  return request<BookSummary[]>(`/browse/authors/${encodeURIComponent(authorKey)}/books`);
}

export async function fetchRecentlyAdded(limit = 50) {
  return request<BookSummary[]>(`/browse/recently-added?limit=${limit}`);
}

export async function fetchRecentlyUpdated(limit = 50) {
  return request<BookSummary[]>(`/browse/recently-updated?limit=${limit}`);
}

export async function fetchShelfReport() {
  return request<GroupedBooks[]>("/reports/shelves");
}

export async function fetchCollectionReport() {
  return request<GroupedBooks[]>("/reports/collections");
}

export async function fetchMissingLocationReport() {
  return request<MissingLocationReport>("/reports/missing-location");
}

export async function fetchValueByCollectionPaidReport() {
  return request<CollectionValueReportRow[]>("/reports/value-by-collection-paid");
}

export async function fetchValueByCollectionReplacementReport() {
  return request<CollectionValueReportRow[]>("/reports/value-by-collection-replacement");
}

export async function fetchPublishers() {
  return request<PublisherSummary[]>("/publishers");
}

export async function renamePublisher(payload: PublisherRenameRequest) {
  return request<PublisherRenameResponse>("/publishers/rename", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

function analyticsQuery(params: ValueAnalyticsParams) {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "" && value !== false) {
      searchParams.set(key, String(value));
    }
  });
  return searchParams.size > 0 ? `?${searchParams.toString()}` : "";
}

export async function fetchValueSummary(params: ValueAnalyticsParams) {
  return request<ValueSummary>(`/analytics/value-summary${analyticsQuery(params)}`);
}

export async function fetchValueDetails(params: ValueAnalyticsParams) {
  return request<ValueDetails>(`/analytics/value-details${analyticsQuery(params)}`);
}

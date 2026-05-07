export interface BookSummary {
  id: string;
  title: string;
  author: string | null;
  shelfCoords?: string | null;
  year: number | null;
  shelfId: string | null;
  dateAdded: string | null;
  lastAudited?: string | null;
  updatedAt?: string | null;
}

export interface AppConfig {
  appName: string;
}

export interface BookPage {
  items: BookSummary[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface BookNavigation {
  previousBookId: string | null;
  nextBookId: string | null;
  position: number | null;
  totalItems: number;
}

export interface BookQueryParams {
  q?: string;
  collectionIds?: string[];
  collectionMode?: string;
  noCollectionOnly?: boolean;
  noBestEstimateOnly?: boolean;
  shelfId?: string;
  year?: number;
  hasShelf?: boolean;
  specialOnly?: boolean;
  digitalOnly?: boolean;
  hardbackOnly?: boolean;
  dustjacketOnly?: boolean;
  slipcaseOnly?: boolean;
  videoOnly?: boolean;
  dateAddedMode?: string;
  dateAddedOn?: string;
  dateAddedYear?: number;
  lastAuditedMode?: string;
  lastAuditedOn?: string;
  sort?: string;
  direction?: string;
  page?: number;
  size?: number;
}

export interface Book {
  id: string;
  title: string;
  author: string | null;
  publicationCity: string | null;
  publisher: string | null;
  imprint: string | null;
  year: number | null;
  firstPublished: number | null;
  edition: string | null;
  pages: number | null;
  frontpages: string | null;
  dateAdded: string | null;
  lastAudited: string | null;
  shelfId: string | null;
  authorSecondary: string | null;
  isbn: string | null;
  special: boolean;
  digital: boolean;
  hardback: boolean | null;
  dustjacket: boolean;
  slipcase: boolean;
  video: boolean;
  vols: number | null;
  series: string | null;
  shelfCoords: string | null;
  note: string | null;
  pricePaid: number | null;
  priceOnItem: number | null;
  priceToReplace: number | null;
  priceToReplaceChecked: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  collectionIds: string[];
}

export interface BookRequest {
  title: string;
  author: string | null;
  publicationCity: string | null;
  publisher: string | null;
  imprint: string | null;
  year: number | null;
  firstPublished: number | null;
  edition: string | null;
  pages: number | null;
  frontpages: string | null;
  dateAdded: string | null;
  lastAudited: string | null;
  shelfId: string | null;
  authorSecondary: string | null;
  isbn: string | null;
  special: boolean;
  digital: boolean;
  hardback: boolean | null;
  dustjacket: boolean;
  slipcase: boolean;
  video: boolean;
  vols: number | null;
  series: string | null;
  shelfCoords: string | null;
  note: string | null;
  pricePaid: number | null;
  priceOnItem: number | null;
  priceToReplace: number | null;
  priceToReplaceChecked: string | null;
  collectionIds: string[];
}

export interface Collection {
  id: string;
  name: string;
  usageCount?: number;
}

export interface Shelf {
  id: string;
  name: string;
  audited?: boolean;
  usageCount?: number;
}

export interface ShelfCoordinateAudit {
  shelfId: string;
  shelfName: string;
  shelfCoords: string;
  audited: boolean;
}

export interface PublisherSummary {
  publisher: string;
  bookCount: number;
}

export interface PublisherRenameRequest {
  sourcePublisher: string;
  targetPublisher: string;
}

export interface PublisherRenameResponse {
  publisher: string;
  updatedCount: number;
}

export interface ExportRequest {
  outputDirectory?: string | null;
  portableJsonl: boolean;
  postgresDump: boolean;
  compress: boolean;
}

export interface ExportFile {
  name: string;
  format: string;
  sizeBytes: number;
}

export interface ExportResponse {
  exportDirectory: string;
  manifestPath: string;
  exportedAt: string;
  schemaVersion: string;
  includes: Record<string, boolean>;
  rowCounts: Record<string, number>;
  files: ExportFile[];
}

export interface AuthorSummary {
  key: string;
  name: string;
  bookCount: number;
}

export interface GroupedBooks {
  id: string | null;
  name: string;
  bookCount: number;
  books: BookSummary[];
}

export interface CollectionValueReportRow {
  id: string;
  name: string;
  bookCount: number;
  totalValue: number;
}

export interface MissingLocationReport {
  totalItems: number;
  books: BookSummary[];
}

export interface ValueSummary {
  basis: string;
  totalValue: number;
  includedCount: number;
  missingValueCount: number;
  totalScopedCount: number;
  coveragePercent: number;
  missingReplacementCheckedCount: number;
  oldestReplacementChecked: string | null;
}

export interface ValueDetail {
  id: string;
  title: string;
  author: string | null;
  year: number | null;
  shelfId: string | null;
  dateAdded: string | null;
  updatedAt?: string | null;
  valueAmount: number | null;
  priceToReplaceChecked: string | null;
  special: boolean;
  digital: boolean;
  hardback: boolean | null;
  dustjacket: boolean;
  slipcase: boolean;
  video: boolean;
}

export interface ValueDetails {
  basis: string;
  totalItems: number;
  items: ValueDetail[];
}

export interface ValueAnalyticsParams {
  basis?: string;
  collectionId?: string;
  shelfId?: string;
  dateAddedMode?: string;
  dateAddedOn?: string;
  dateAddedYear?: number;
  specialOnly?: boolean;
  digitalOnly?: boolean;
  hardbackOnly?: boolean;
  dustjacketOnly?: boolean;
  slipcaseOnly?: boolean;
  videoOnly?: boolean;
}

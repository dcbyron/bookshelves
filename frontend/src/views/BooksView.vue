<script setup lang="ts">
import { computed, h, reactive, watch } from "vue";
import { useQuery } from "@tanstack/vue-query";
import { RouterLink, useRoute } from "vue-router";
import type { LocationQuery, LocationQueryRaw } from "vue-router";
import {
  NAlert,
  NButton,
  NCard,
  NCheckbox,
  NDataTable,
  NEmpty,
  NInput,
  NPagination,
  NSelect,
  NSpace,
  NSpin,
  NStatistic,
  NTag
} from "naive-ui";
import type { SelectOption } from "naive-ui";

import { fetchBooks } from "../api/catalog";
import { useReferenceData } from "../composables/useReferenceData";
import type { BookSummary } from "../types/catalog";
import { formatDate } from "../utils/format";

const filters = reactive({
  q: "",
  collectionIds: [] as string[],
  collectionMode: "or",
  noCollectionOnly: false,
  noBestEstimateOnly: false,
  shelfId: "",
  year: "",
  hasShelf: "",
  specialOnly: false,
  digitalOnly: false,
  hardbackOnly: false,
  dustjacketOnly: false,
  slipcaseOnly: false,
  videoOnly: false,
  dateAddedMode: "any",
  dateAddedOn: "",
  dateAddedYear: "",
  lastAuditedMode: "any",
  lastAuditedOn: "",
  sort: "title",
  direction: "asc",
  page: 1,
  size: 25
});

const route = useRoute();

function applyQueryToFilters(query: LocationQuery) {
  filters.q = typeof query.q === "string" ? query.q : "";
  const parsedCollectionIds =
    typeof query.collectionIds === "string"
      ? query.collectionIds.split(",").map((value) => value.trim()).filter(Boolean)
      : [];
  const noCollectionOnly = query.noCollectionOnly === "true";
  filters.collectionIds = noCollectionOnly ? [] : parsedCollectionIds;
  filters.collectionMode = typeof query.collectionMode === "string" && query.collectionMode === "and" ? "and" : "or";
  filters.noCollectionOnly = noCollectionOnly;
  filters.noBestEstimateOnly = query.noBestEstimateOnly === "true";
  filters.shelfId = typeof query.shelfId === "string" ? query.shelfId : "";
  filters.year = typeof query.year === "string" ? query.year : "";
  filters.hasShelf = typeof query.hasShelf === "string" ? query.hasShelf : "";
  filters.specialOnly = query.specialOnly === "true";
  filters.digitalOnly = query.digitalOnly === "true";
  filters.hardbackOnly = query.hardbackOnly === "true";
  filters.dustjacketOnly = query.dustjacketOnly === "true";
  filters.slipcaseOnly = query.slipcaseOnly === "true";
  filters.videoOnly = query.videoOnly === "true";
  filters.dateAddedMode = typeof query.dateAddedMode === "string" ? query.dateAddedMode : "any";
  filters.dateAddedOn = typeof query.dateAddedOn === "string" ? query.dateAddedOn : "";
  filters.dateAddedYear = typeof query.dateAddedYear === "string" ? query.dateAddedYear : "";
  filters.lastAuditedMode = typeof query.lastAuditedMode === "string" ? query.lastAuditedMode : "any";
  filters.lastAuditedOn = typeof query.lastAuditedOn === "string" ? query.lastAuditedOn : "";
  filters.sort = typeof query.sort === "string" ? query.sort : "title";
  filters.direction = typeof query.direction === "string" ? query.direction : "asc";
  filters.page = typeof query.page === "string" && Number(query.page) > 0 ? Number(query.page) : 1;
  filters.size = typeof query.size === "string" && Number(query.size) > 0 ? Number(query.size) : 25;
}

watch(
  () => route.query,
  (query) => {
    applyQueryToFilters(query);
  },
  { immediate: true }
);

const queryParams = computed(() => ({
  q: filters.q.trim() || undefined,
  collectionIds: filters.collectionIds.length ? filters.collectionIds : undefined,
  collectionMode: filters.collectionIds.length > 1 ? filters.collectionMode : undefined,
  noCollectionOnly: filters.noCollectionOnly || undefined,
  noBestEstimateOnly: filters.noBestEstimateOnly || undefined,
  shelfId: filters.shelfId.trim() || undefined,
  year: filters.year ? Number(filters.year) : undefined,
  hasShelf: filters.hasShelf === "" ? undefined : filters.hasShelf === "true",
  specialOnly: filters.specialOnly || undefined,
  digitalOnly: filters.digitalOnly || undefined,
  hardbackOnly: filters.hardbackOnly || undefined,
  dustjacketOnly: filters.dustjacketOnly || undefined,
  slipcaseOnly: filters.slipcaseOnly || undefined,
  videoOnly: filters.videoOnly || undefined,
  dateAddedMode:
    filters.dateAddedMode === "year"
      ? filters.dateAddedYear.trim()
        ? "year"
        : undefined
      : filters.dateAddedMode !== "any" && filters.dateAddedOn.trim()
        ? filters.dateAddedMode
        : undefined,
  dateAddedOn:
    filters.dateAddedMode !== "any" && filters.dateAddedMode !== "year" && filters.dateAddedOn.trim()
      ? filters.dateAddedOn.trim()
      : undefined,
  dateAddedYear:
    filters.dateAddedMode === "year" && filters.dateAddedYear.trim()
      ? Number(filters.dateAddedYear.trim())
      : undefined,
  lastAuditedMode:
    filters.lastAuditedMode === "never"
      ? "never"
      : filters.lastAuditedMode !== "any" && filters.lastAuditedOn.trim()
        ? filters.lastAuditedMode
        : undefined,
  lastAuditedOn:
    filters.lastAuditedMode !== "any" && filters.lastAuditedMode !== "never" && filters.lastAuditedOn.trim()
      ? filters.lastAuditedOn.trim()
      : undefined,
  sort: filters.sort,
  direction: filters.direction,
  page: filters.page - 1,
  size: filters.size
}));

const booksQuery = useQuery({
  queryKey: computed(() => ["books", queryParams.value]),
  queryFn: () => fetchBooks(queryParams.value)
});

const { collectionsQuery, shelvesQuery } = useReferenceData();

const collectionOptions = computed(() =>
  (collectionsQuery.data.value ?? []).map((collection) => ({
    label: collection.name,
    value: collection.id
  }))
);

const shelfOptions = computed(() =>
  (shelvesQuery.data.value ?? []).map((shelf) => ({
    label: shelf.id,
    value: shelf.id
  }))
);

const shelfPresenceOptions: SelectOption[] = [
  { label: "Any shelf state", value: "" },
  { label: "Has shelf", value: "true" },
  { label: "No shelf", value: "false" }
];

const auditedFilterOptions: SelectOption[] = [
  { label: "Any audit date", value: "any" },
  { label: "Never audited", value: "never" },
  { label: "Audited before", value: "before" },
  { label: "Audited after", value: "after" }
];

const addedFilterOptions: SelectOption[] = [
  { label: "Any added date", value: "any" },
  { label: "Added in year", value: "year" },
  { label: "Added before", value: "before" },
  { label: "Added after", value: "after" }
];

const dateAddedInputLabel = computed(() => (filters.dateAddedMode === "year" ? "Added Year" : "Added Date"));
const dateAddedInputPlaceholder = computed(() => (filters.dateAddedMode === "year" ? "YYYY" : "YYYY-MM-DD"));

const sortOptions: SelectOption[] = [
  { label: "Title", value: "title" },
  { label: "Author", value: "author" },
  { label: "Year", value: "year" },
  { label: "Date added", value: "dateAdded" },
  { label: "Updated", value: "updated" }
];

const directionOptions: SelectOption[] = [
  { label: "Ascending", value: "asc" },
  { label: "Descending", value: "desc" }
];

const pageSizeOptions: SelectOption[] = [
  { label: "25", value: 25 },
  { label: "50", value: 50 },
  { label: "100", value: 100 }
];

const shelfNameById = computed(
  () => new Map((shelvesQuery.data.value ?? []).map((shelf) => [shelf.id, shelf.name]))
);

const filteredCount = computed(() => booksQuery.data.value?.items.length ?? 0);
const totalBooks = computed(() => booksQuery.data.value?.totalItems ?? 0);
const totalPages = computed(() => booksQuery.data.value?.totalPages ?? 0);
const pageStart = computed(() => (totalBooks.value === 0 ? 0 : (filters.page - 1) * filters.size + 1));
const pageEnd = computed(() => Math.min(filters.page * filters.size, totalBooks.value));
const activeFilterCount = computed(
  () =>
    [
      filters.q,
      filters.collectionIds.length > 0,
      filters.noCollectionOnly,
      filters.noBestEstimateOnly,
      filters.shelfId,
      filters.year,
      filters.hasShelf,
      filters.specialOnly,
      filters.digitalOnly,
      filters.hardbackOnly,
      filters.dustjacketOnly,
      filters.slipcaseOnly,
      filters.videoOnly,
      (filters.dateAddedMode === "year" && filters.dateAddedYear) ||
        (filters.dateAddedMode !== "any" && filters.dateAddedMode !== "year" && filters.dateAddedOn),
      filters.lastAuditedMode === "never" || (filters.lastAuditedMode !== "any" && filters.lastAuditedOn)
    ].filter(Boolean).length
);

function clearFilters() {
  filters.q = "";
  filters.collectionIds = [];
  filters.collectionMode = "or";
  filters.noCollectionOnly = false;
  filters.noBestEstimateOnly = false;
  filters.shelfId = "";
  filters.year = "";
  filters.hasShelf = "";
  filters.specialOnly = false;
  filters.digitalOnly = false;
  filters.hardbackOnly = false;
  filters.dustjacketOnly = false;
  filters.slipcaseOnly = false;
  filters.videoOnly = false;
  filters.dateAddedMode = "any";
  filters.dateAddedOn = "";
  filters.dateAddedYear = "";
  filters.lastAuditedMode = "any";
  filters.lastAuditedOn = "";
  filters.page = 1;
}

function resetToFirstPage() {
  filters.page = 1;
}

function updateDateAddedFilterInput(value: string) {
  if (filters.dateAddedMode === "year") {
    filters.dateAddedYear = value;
  } else {
    filters.dateAddedOn = value;
  }
  resetToFirstPage();
}

function updateDateAddedMode(value: string) {
  filters.dateAddedMode = value;
  if (value === "year") {
    filters.dateAddedOn = "";
  } else {
    filters.dateAddedYear = "";
  }
  resetToFirstPage();
}

function updateShelfFilter(value: string | null) {
  filters.shelfId = value ?? "";
  resetToFirstPage();
}

function shortId(id: string) {
  return id.slice(0, 8);
}

function booksRouteQuery(): LocationQueryRaw {
  return {
    source: "books",
    ...(filters.q.trim() ? { q: filters.q.trim() } : {}),
    ...(filters.collectionIds.length ? { collectionIds: filters.collectionIds.join(",") } : {}),
    ...(filters.collectionIds.length > 1 ? { collectionMode: filters.collectionMode } : {}),
    ...(filters.noCollectionOnly ? { noCollectionOnly: "true" } : {}),
    ...(filters.noBestEstimateOnly ? { noBestEstimateOnly: "true" } : {}),
    ...(filters.shelfId.trim() ? { shelfId: filters.shelfId.trim() } : {}),
    ...(filters.year.trim() ? { year: filters.year.trim() } : {}),
    ...(filters.hasShelf ? { hasShelf: filters.hasShelf } : {}),
    ...(filters.specialOnly ? { specialOnly: "true" } : {}),
    ...(filters.digitalOnly ? { digitalOnly: "true" } : {}),
    ...(filters.hardbackOnly ? { hardbackOnly: "true" } : {}),
    ...(filters.dustjacketOnly ? { dustjacketOnly: "true" } : {}),
    ...(filters.slipcaseOnly ? { slipcaseOnly: "true" } : {}),
    ...(filters.videoOnly ? { videoOnly: "true" } : {}),
    ...(filters.dateAddedMode !== "any" ? { dateAddedMode: filters.dateAddedMode } : {}),
    ...(filters.dateAddedMode === "year" && filters.dateAddedYear.trim()
      ? { dateAddedYear: filters.dateAddedYear.trim() }
      : {}),
    ...(filters.dateAddedMode !== "any" && filters.dateAddedMode !== "year" && filters.dateAddedOn.trim()
      ? { dateAddedOn: filters.dateAddedOn.trim() }
      : {}),
    ...(filters.lastAuditedMode !== "any" ? { lastAuditedMode: filters.lastAuditedMode } : {}),
    ...(filters.lastAuditedMode !== "any" && filters.lastAuditedMode !== "never" && filters.lastAuditedOn.trim()
      ? { lastAuditedOn: filters.lastAuditedOn.trim() }
      : {}),
    sort: filters.sort,
    direction: filters.direction,
    page: String(filters.page),
    size: String(filters.size)
  };
}

function toggleCollection(collectionId: string) {
  filters.noCollectionOnly = false;
  filters.collectionIds = filters.collectionIds.includes(collectionId)
    ? filters.collectionIds.filter((id) => id !== collectionId)
    : [...filters.collectionIds, collectionId];
  resetToFirstPage();
}

function toggleNoCollection() {
  filters.noCollectionOnly = !filters.noCollectionOnly;
  if (filters.noCollectionOnly) {
    filters.collectionIds = [];
  }
  resetToFirstPage();
}

function setCollectionMode(mode: "or" | "and") {
  filters.collectionMode = mode;
  resetToFirstPage();
}

const columns = computed(() => [
  {
    title: "Id",
    key: "id",
    width: 112,
    ellipsis: {
      tooltip: true
    },
    render: (row: BookSummary) =>
      h("code", { class: "muted-copy", title: row.id }, shortId(row.id))
  },
  {
    title: "Title",
    key: "title",
    minWidth: 380,
    render: (row: BookSummary) =>
      h(
        RouterLink,
        { to: { path: `/books/${row.id}`, query: booksRouteQuery() }, class: "table-link" },
        { default: () => row.title }
      )
  },
  {
    title: "Author",
    key: "author",
    width: 210,
    render: (row: BookSummary) => row.author ?? "—"
  },
  {
    title: "Year",
    key: "year",
    width: 72,
    render: (row: BookSummary) => row.year ?? "—"
  },
  {
    title: "Shelf",
    key: "shelfId",
    width: 176,
    render: (row: BookSummary) => {
      const shelfId = row.shelfId ?? "—";
      const shelfName = shelfNameById.value.get(row.shelfId ?? "") ?? row.shelfId ?? "—";
      return h("span", { title: shelfName }, shelfId);
    }
  },
  {
    title: "Added",
    key: "dateAdded",
    width: 116,
    render: (row: BookSummary) => formatDate(row.dateAdded)
  },
  {
    title: "Updated",
    key: "updatedAt",
    width: 116,
    render: (row: BookSummary) => formatDate(row.updatedAt)
  }
]);
</script>

<template>
  <section class="stack">
    <header class="page-header">
      <div>
        <p class="page-subtitle">
          Search the catalog, narrow it with a few focused filters, and jump straight into edits.
        </p>
      </div>
      <NSpace>
        <NButton secondary @click="booksQuery.refetch()">Refresh</NButton>
        <RouterLink to="/books/new">
          <NButton type="primary">New book</NButton>
        </RouterLink>
      </NSpace>
    </header>

    <div class="books-summary-grid">
      <NCard class="books-focus-card">
        <div class="books-meta-copy">
          <span class="muted-label">Current Focus</span>
          <div class="pill-list books-focus-body">
            <NTag v-if="filters.noCollectionOnly" type="warning" round>No Collection</NTag>
            <NTag v-if="filters.noBestEstimateOnly" type="error" round>No Best Estimate</NTag>
            <template v-else>
              <NTag v-for="collectionId in filters.collectionIds" :key="collectionId" type="success" round>
                {{ collectionOptions.find((option) => option.value === collectionId)?.label ?? collectionId }}
              </NTag>
              <NTag v-if="filters.collectionIds.length > 1" type="default" round>
                Collections: {{ filters.collectionMode.toUpperCase() }}
              </NTag>
            </template>
            <NTag v-if="filters.q" round>{{ filters.q }}</NTag>
            <NTag v-if="filters.shelfId" type="info" round>
              {{ shelfOptions.find((option) => option.value === filters.shelfId)?.label ?? filters.shelfId }}
            </NTag>
            <NTag v-if="filters.year" type="warning" round>{{ filters.year }}</NTag>
            <NTag v-if="filters.hasShelf === 'true'" type="info" round>Has shelf</NTag>
            <NTag v-if="filters.hasShelf === 'false'" type="warning" round>No shelf</NTag>
            <NTag v-if="filters.specialOnly" type="success" round>Special</NTag>
            <NTag v-if="filters.digitalOnly" type="info" round>Digital</NTag>
            <NTag v-if="filters.hardbackOnly" type="warning" round>Hardback</NTag>
            <NTag v-if="filters.dustjacketOnly" type="warning" round>Dustjacket</NTag>
            <NTag v-if="filters.slipcaseOnly" type="default" round>Slipcase</NTag>
            <NTag v-if="filters.videoOnly" type="error" round>Video</NTag>
            <NTag v-if="filters.dateAddedMode === 'year' && filters.dateAddedYear" type="info" round>
              Added in {{ filters.dateAddedYear }}
            </NTag>
            <NTag v-else-if="filters.dateAddedMode !== 'any' && filters.dateAddedOn" type="info" round>
              Added {{ filters.dateAddedMode }} {{ filters.dateAddedOn }}
            </NTag>
            <NTag v-if="filters.lastAuditedMode === 'never'" type="info" round>
              Never audited
            </NTag>
            <NTag v-else-if="filters.lastAuditedMode !== 'any' && filters.lastAuditedOn" type="info" round>
              Audited {{ filters.lastAuditedMode }} {{ filters.lastAuditedOn }}
            </NTag>
            <NButton
              v-if="activeFilterCount > 0"
              size="small"
              secondary
              class="books-focus-clear"
              @click="clearFilters"
            >
              Clear filters
            </NButton>
            <span v-if="activeFilterCount === 0" class="muted-copy">All books</span>
          </div>
        </div>
      </NCard>
    </div>

    <NCard>
      <div class="filters filter-cluster-grid">
        <div class="filter-cluster filter-cluster-search-year">
          <div class="field">
            <label for="q">Search</label>
            <NInput id="q" v-model:value="filters.q" clearable placeholder="Title, author, or series" @update:value="resetToFirstPage" />
          </div>
          <div class="field">
            <label for="year">Year</label>
            <NInput id="year" v-model:value="filters.year" clearable placeholder="e.g. 2024" @update:value="resetToFirstPage" />
          </div>
        </div>
        <div class="filter-cluster filter-cluster-two">
          <div class="field">
            <label for="hasShelf">Shelf State</label>
            <NSelect
              id="hasShelf"
              v-model:value="filters.hasShelf"
              :options="shelfPresenceOptions"
              @update:value="resetToFirstPage"
            />
          </div>
          <div class="field">
            <label for="shelf">Shelf</label>
            <NSelect
              id="shelf"
              :value="filters.shelfId || null"
              :options="shelfOptions"
              clearable
              placeholder="All shelves"
              @update:value="updateShelfFilter"
            />
          </div>
        </div>
        <div class="filter-cluster filter-cluster-two">
          <div class="field">
            <label for="dateAddedMode">Date Added</label>
            <NSelect
              id="dateAddedMode"
              :value="filters.dateAddedMode"
              @update:value="updateDateAddedMode"
              :options="addedFilterOptions"
            />
          </div>
          <div class="field">
            <label for="dateAddedOn">{{ dateAddedInputLabel }}</label>
            <NInput
              id="dateAddedOn"
              :value="filters.dateAddedMode === 'year' ? filters.dateAddedYear : filters.dateAddedOn"
              @update:value="updateDateAddedFilterInput"
              clearable
              :placeholder="dateAddedInputPlaceholder"
              :disabled="filters.dateAddedMode === 'any'"
            />
          </div>
        </div>
        <div class="filter-cluster filter-cluster-two">
          <div class="field">
            <label for="lastAuditedMode">Last Audited</label>
            <NSelect
              id="lastAuditedMode"
              v-model:value="filters.lastAuditedMode"
              :options="auditedFilterOptions"
              @update:value="resetToFirstPage"
            />
          </div>
          <div class="field">
            <label for="lastAuditedOn">Audit Date</label>
            <NInput
              id="lastAuditedOn"
              v-model:value="filters.lastAuditedOn"
              clearable
              placeholder="YYYY-MM-DD"
              :disabled="filters.lastAuditedMode === 'any' || filters.lastAuditedMode === 'never'"
              @update:value="resetToFirstPage"
            />
          </div>
        </div>
      </div>
      <div class="filter-cluster top-gap">
        <div class="field">
          <label>Collections</label>
          <div class="pill-list collection-button-surface">
            <button
              class="btn compact-pill-button"
              :class="filters.noCollectionOnly ? 'btn-primary' : 'btn-secondary'"
              type="button"
              @click="toggleNoCollection"
            >
              <NTag size="small" :bordered="false" :type="filters.noCollectionOnly ? 'warning' : 'default'" round>
                No Collection
              </NTag>
            </button>
            <span class="collection-row-break" aria-hidden="true"></span>
            <button
              v-for="collection in collectionsQuery.data.value ?? []"
              :key="collection.id"
              class="btn compact-pill-button"
              :class="filters.collectionIds.includes(collection.id) ? 'btn-primary' : 'btn-secondary'"
              type="button"
              @click="toggleCollection(collection.id)"
            >
              <NTag size="small" :bordered="false" :type="filters.collectionIds.includes(collection.id) ? 'success' : 'default'" round>
                {{ collection.name }}
              </NTag>
            </button>
          </div>
        </div>
        <div class="field">
          <label>Collection Match</label>
          <div class="inline-actions">
            <NButton
              size="small"
              class="compact-match-button"
              :type="filters.collectionMode === 'or' ? 'primary' : 'default'"
              :disabled="filters.noCollectionOnly"
              @click="setCollectionMode('or')"
            >
              OR
            </NButton>
            <NButton
              size="small"
              class="compact-match-button"
              :type="filters.collectionMode === 'and' ? 'primary' : 'default'"
              :disabled="filters.noCollectionOnly"
              @click="setCollectionMode('and')"
            >
              AND
            </NButton>
          </div>
        </div>
      </div>
      <div class="filter-cluster top-gap">
        <div class="inline-actions">
          <NCheckbox v-model:checked="filters.specialOnly" @update:checked="resetToFirstPage">Special</NCheckbox>
          <NCheckbox v-model:checked="filters.digitalOnly" @update:checked="resetToFirstPage">Digital</NCheckbox>
          <NCheckbox v-model:checked="filters.hardbackOnly" @update:checked="resetToFirstPage">Hardback</NCheckbox>
          <NCheckbox v-model:checked="filters.dustjacketOnly" @update:checked="resetToFirstPage">Dustjacket</NCheckbox>
          <NCheckbox v-model:checked="filters.slipcaseOnly" @update:checked="resetToFirstPage">Slipcase</NCheckbox>
          <NCheckbox v-model:checked="filters.videoOnly" @update:checked="resetToFirstPage">Video</NCheckbox>
          <NCheckbox v-model:checked="filters.noBestEstimateOnly" @update:checked="resetToFirstPage">
            No Best Estimate
          </NCheckbox>
        </div>
      </div>
    </NCard>

    <NAlert v-if="booksQuery.isError.value" type="error" title="Could not load books">
      {{ booksQuery.error.value instanceof Error ? booksQuery.error.value.message : "Unknown error" }}
    </NAlert>

    <NCard>
      <div v-if="totalBooks > 0" class="results-toolbar">
        <span class="muted-copy results-toolbar-summary">
          Showing {{ pageStart }}-{{ pageEnd }} of {{ totalBooks }} books
        </span>
        <div class="filter-cluster filter-cluster-three results-toolbar-controls">
          <div class="field">
            <label for="sort">Sort By</label>
            <NSelect id="sort" v-model:value="filters.sort" :options="sortOptions" @update:value="resetToFirstPage" />
          </div>
          <div class="field">
            <label for="direction">Direction</label>
            <NSelect id="direction" v-model:value="filters.direction" :options="directionOptions" @update:value="resetToFirstPage" />
          </div>
          <div class="field">
            <label for="pageSize">Page Size</label>
            <NSelect id="pageSize" v-model:value="filters.size" :options="pageSizeOptions" @update:value="resetToFirstPage" />
          </div>
        </div>
      </div>
      <NSpin :show="booksQuery.isLoading.value">
        <NDataTable
          v-if="booksQuery.data.value?.items.length"
          :columns="columns"
          :data="booksQuery.data.value.items"
          :bordered="false"
          size="large"
        />
        <NEmpty
          v-else-if="activeFilterCount > 0"
          description="No books match the current filters."
          class="empty-state"
        >
          <template #extra>
            <NButton tertiary @click="clearFilters">Clear filters</NButton>
          </template>
        </NEmpty>
        <NEmpty
          v-else
          description="No books have been added to the catalog yet."
          class="empty-state"
        >
          <template #extra>
            <RouterLink to="/books/new">
              <NButton type="primary">Add the first book</NButton>
            </RouterLink>
          </template>
        </NEmpty>
      </NSpin>
      <div v-if="totalBooks > 0" class="inline-actions top-gap books-footer">
        <NPagination
          v-model:page="filters.page"
          :page-count="Math.max(totalPages, 1)"
        />
      </div>
    </NCard>
  </section>
</template>

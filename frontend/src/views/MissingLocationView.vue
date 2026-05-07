<script setup lang="ts">
import { computed, reactive, watch } from "vue";
import { useMutation, useQuery } from "@tanstack/vue-query";
import { NAlert, NButton, NCard, NEmpty, NSelect, NSpin, NSpace, useMessage } from "naive-ui";

import { fetchBook, fetchMissingLocationReport, fetchShelves, updateBook } from "../api/catalog";
import type { Book, BookRequest, BookSummary } from "../types/catalog";

const message = useMessage();
const selectedShelves = reactive<Record<string, string>>({});
const selectedCoords = reactive<Record<string, string>>({});
const assignedShelves = reactive<Record<string, string>>({});
const rowErrors = reactive<Record<string, string>>({});
const assigningRows = reactive<Record<string, boolean>>({});

const reportQuery = useQuery({
  queryKey: ["missing-location"],
  queryFn: fetchMissingLocationReport
});

const shelvesQuery = useQuery({
  queryKey: ["shelves"],
  queryFn: fetchShelves
});

const shelfOptions = computed(() =>
  (shelvesQuery.data.value ?? []).map((shelf) => ({
    label: `${shelf.name} (${shelf.id})`,
    value: shelf.id
  }))
);

const shelfNameById = computed(() => new Map((shelvesQuery.data.value ?? []).map((shelf) => [shelf.id, shelf.name])));

function compareByAuthorThenTitle(left: BookSummary, right: BookSummary) {
  const authorComparison = (left.author ?? "").localeCompare(right.author ?? "", undefined, { sensitivity: "base" });
  if (authorComparison !== 0) {
    return authorComparison;
  }
  return left.title.localeCompare(right.title, undefined, { sensitivity: "base" });
}

function missingLocationBooks(books: BookSummary[]) {
  return [...books].sort(compareByAuthorThenTitle);
}

function toBookRequest(book: Book): BookRequest {
  return {
    title: book.title,
    author: book.author,
    publicationCity: book.publicationCity,
    publisher: book.publisher,
    imprint: book.imprint,
    year: book.year,
    firstPublished: book.firstPublished,
    edition: book.edition,
    pages: book.pages,
    frontpages: book.frontpages,
    dateAdded: book.dateAdded,
    lastAudited: book.lastAudited,
    shelfId: book.shelfId,
    authorSecondary: book.authorSecondary,
    isbn: book.isbn,
    special: book.special,
    digital: book.digital,
    hardback: book.hardback ?? false,
    dustjacket: book.dustjacket,
    slipcase: book.slipcase,
    video: book.video,
    vols: book.vols,
    series: book.series,
    shelfCoords: book.shelfCoords,
    note: book.note,
    pricePaid: book.pricePaid,
    priceOnItem: book.priceOnItem,
    priceToReplace: book.priceToReplace,
    priceToReplaceChecked: book.priceToReplaceChecked,
    collectionIds: [...book.collectionIds]
  };
}

function effectiveShelfId(bookId: string) {
  return assignedShelves[bookId] ?? selectedShelves[bookId] ?? "";
}

function clearState() {
  Object.keys(selectedShelves).forEach((key) => delete selectedShelves[key]);
  Object.keys(selectedCoords).forEach((key) => delete selectedCoords[key]);
  Object.keys(assignedShelves).forEach((key) => delete assignedShelves[key]);
  Object.keys(rowErrors).forEach((key) => delete rowErrors[key]);
  Object.keys(assigningRows).forEach((key) => delete assigningRows[key]);
}

watch(() => reportQuery.data.value, () => {
  clearState();
});

const assignShelfMutation = useMutation({
  mutationFn: async ({ bookId, shelfId, shelfCoords }: { bookId: string; shelfId: string; shelfCoords?: string }) => {
    const book = await fetchBook(bookId);
    return updateBook(bookId, {
      ...toBookRequest(book),
      shelfId,
      ...(shelfCoords ? { shelfCoords } : {})
    });
  },
  onSuccess: (book) => {
    if (book.shelfId) {
      assignedShelves[book.id] = book.shelfId;
      selectedShelves[book.id] = book.shelfId;
      delete rowErrors[book.id];
      message.success(`Assigned "${book.title}" to ${shelfNameById.value.get(book.shelfId) ?? book.shelfId}.`);
    }
  },
  onError: (error, variables) => {
    rowErrors[variables.bookId] = error instanceof Error ? error.message : "Could not assign this book to a shelf.";
  },
  onSettled: (_, __, variables) => {
    delete assigningRows[variables.bookId];
  }
});

function applyShelf(book: BookSummary) {
  const shelfId = selectedShelves[book.id];
  const shelfCoords = selectedCoords[book.id]?.trim();
  if (!shelfId) {
    rowErrors[book.id] = "Choose a shelf before applying.";
    return;
  }
  delete rowErrors[book.id];
  assigningRows[book.id] = true;
  assignShelfMutation.mutate({ bookId: book.id, shelfId, shelfCoords: shelfCoords || undefined });
}
</script>

<template>
  <section class="stack report-page">
    <header class="page-header report-controls">
      <div>
        <p class="page-subtitle">
          Assign shelves to books that still lack a location and work through the queue one row at a time.
        </p>
      </div>
      <NSpace>
        <NButton secondary @click="reportQuery.refetch()">Refresh</NButton>
      </NSpace>
    </header>

    <NAlert v-if="shelvesQuery.isError.value" type="error" title="Could not load shelves">
      {{ shelvesQuery.error.value instanceof Error ? shelvesQuery.error.value.message : "Unknown error" }}
    </NAlert>

    <NSpin :show="reportQuery.isPending.value">
      <section class="card report-group">
        <div class="report-group-header">
          <h4>Books Without a Shelf</h4>
          <span v-if="reportQuery.data.value" class="muted-copy">{{ reportQuery.data.value.totalItems }} books</span>
        </div>
        <div v-if="reportQuery.data.value && reportQuery.data.value.books.length > 0" class="report-book-list">
          <div
            v-for="book in missingLocationBooks(reportQuery.data.value.books)"
            :key="book.id"
            class="report-book-row report-book-row-assignable"
          >
            <strong>{{ book.author ?? "—" }}</strong>
            <span>{{ book.title }}</span>
            <span>{{ book.year ?? "—" }}</span>
            <div class="report-assignment-control">
              <NSelect
                :value="effectiveShelfId(book.id) || null"
                :options="shelfOptions"
                placeholder="Choose shelf"
                :disabled="Boolean(assignedShelves[book.id]) || Boolean(assigningRows[book.id])"
                @update:value="(value) => { selectedShelves[book.id] = value ?? ''; delete rowErrors[book.id]; }"
              />
            </div>
            <input
              :value="selectedCoords[book.id] ?? ''"
              class="report-coords-input"
              type="text"
              inputmode="text"
              maxlength="2"
              placeholder="Coords"
              :disabled="!selectedShelves[book.id] || Boolean(assignedShelves[book.id]) || Boolean(assigningRows[book.id])"
              @input="selectedCoords[book.id] = (($event.target as HTMLInputElement).value ?? '').slice(0, 2)"
            />
            <div class="report-apply-cell">
              <NButton
                class="report-apply-button"
                type="primary"
                size="small"
                :disabled="Boolean(assignedShelves[book.id]) || !selectedShelves[book.id] || Boolean(assigningRows[book.id])"
                @click="applyShelf(book)"
              >
                {{ assigningRows[book.id] ? "Applying..." : "Apply" }}
              </NButton>
              <span v-if="rowErrors[book.id]" class="field-error">{{ rowErrors[book.id] }}</span>
              <span v-else-if="assignedShelves[book.id]" class="muted-copy">
                Assigned to {{ shelfNameById.get(assignedShelves[book.id]) ?? assignedShelves[book.id] }}
              </span>
            </div>
          </div>
        </div>
        <NEmpty v-else description="All books currently have a shelf assignment." />
      </section>
    </NSpin>
  </section>
</template>

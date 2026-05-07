<script setup lang="ts">
import { computed, ref } from "vue";
import { useQuery } from "@tanstack/vue-query";
import { NButton, NCard, NEmpty, NSpin, NSpace } from "naive-ui";

import {
  fetchCollectionReport,
  fetchRecentlyAdded,
  fetchRecentlyUpdated,
  fetchShelfReport,
  fetchValueByCollectionPaidReport,
  fetchValueByCollectionReplacementReport
} from "../api/catalog";
import type { BookSummary } from "../types/catalog";
import { formatCurrency, formatDate } from "../utils/format";

type ReportType =
  | "shelves"
  | "collections"
  | "recently-added"
  | "recently-updated"
  | "value-by-collection-paid"
  | "value-by-collection-replacement";

const reportType = ref<ReportType>("shelves");

const reportTitle = computed(() => {
  switch (reportType.value) {
    case "collections":
      return "Collections Report";
    case "recently-added":
      return "Recently Added Report";
    case "recently-updated":
      return "Recently Updated Report";
    case "value-by-collection-paid":
      return "Value by Collection (Paid)";
    case "value-by-collection-replacement":
      return "Value by Collection (Replacement)";
    default:
      return "Shelves Report";
  }
});

const reportQuery = useQuery({
  queryKey: computed(() => ["reports", reportType.value]),
  queryFn: async () => {
    switch (reportType.value) {
      case "collections":
        return { kind: "grouped" as const, data: await fetchCollectionReport() };
      case "recently-added":
        return { kind: "recent" as const, data: await fetchRecentlyAdded() };
      case "recently-updated":
        return { kind: "recent" as const, data: await fetchRecentlyUpdated() };
      case "value-by-collection-paid":
        return { kind: "collection-value" as const, data: await fetchValueByCollectionPaidReport() };
      case "value-by-collection-replacement":
        return { kind: "collection-value" as const, data: await fetchValueByCollectionReplacementReport() };
      default:
        return { kind: "grouped" as const, data: await fetchShelfReport() };
    }
  }
});

function compareByAuthorThenTitle(left: BookSummary, right: BookSummary) {
  const authorComparison = (left.author ?? "").localeCompare(right.author ?? "", undefined, { sensitivity: "base" });
  if (authorComparison !== 0) {
    return authorComparison;
  }
  return left.title.localeCompare(right.title, undefined, { sensitivity: "base" });
}

function parseShelfCoords(coords: string | null | undefined) {
  const match = (coords ?? "").trim().match(/^([a-z]+)(\d+)$/i);
  return {
    column: match ? match[1].toLowerCase() : "{",
    tier: match ? Number.parseInt(match[2], 10) : Number.MAX_SAFE_INTEGER,
    raw: (coords ?? "{").toLowerCase()
  };
}

function compareByShelfCoordsAuthorThenTitle(left: BookSummary, right: BookSummary) {
  const leftCoords = parseShelfCoords(left.shelfCoords);
  const rightCoords = parseShelfCoords(right.shelfCoords);
  const columnComparison = leftCoords.column.localeCompare(rightCoords.column, undefined, { sensitivity: "base" });
  if (columnComparison !== 0) {
    return columnComparison;
  }
  if (leftCoords.tier !== rightCoords.tier) {
    return leftCoords.tier - rightCoords.tier;
  }
  const rawComparison = leftCoords.raw.localeCompare(rightCoords.raw, undefined, { sensitivity: "base" });
  if (rawComparison !== 0) {
    return rawComparison;
  }
  return compareByAuthorThenTitle(left, right);
}

function booksForGroup(books: BookSummary[]) {
  return reportType.value === "shelves"
    ? [...books].sort(compareByShelfCoordsAuthorThenTitle)
    : reportType.value === "collections"
      ? [...books].sort(compareByAuthorThenTitle)
    : books;
}

function printReport() {
  window.print();
}

function downloadJson() {
  if (!reportQuery.data.value) {
    return;
  }
  const payload = JSON.stringify(reportQuery.data.value.data, null, 2);
  const blob = new Blob([payload], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${reportType.value}-report.json`;
  link.click();
  URL.revokeObjectURL(url);
}

function perBookValue(totalValue: number, bookCount: number) {
  return bookCount > 0 ? totalValue / bookCount : 0;
}
</script>

<template>
  <section class="stack report-page">
    <header class="page-header report-controls">
      <div>
        <p class="page-subtitle">
          Print clean paper-ready reports or export the underlying report data as JSON.
        </p>
      </div>
      <NSpace>
        <NButton secondary @click="reportQuery.refetch()">Refresh</NButton>
        <NButton secondary @click="downloadJson">Download JSON</NButton>
        <NButton type="primary" @click="printReport">Print</NButton>
      </NSpace>
    </header>

    <NCard class="report-controls">
      <NSpace>
        <NButton :type="reportType === 'shelves' ? 'primary' : 'default'" @click="reportType = 'shelves'">Shelves</NButton>
        <NButton :type="reportType === 'collections' ? 'primary' : 'default'" @click="reportType = 'collections'">Collections</NButton>
        <NButton :type="reportType === 'value-by-collection-paid' ? 'primary' : 'default'" @click="reportType = 'value-by-collection-paid'">
          Value by Collection (Paid)
        </NButton>
        <NButton
          :type="reportType === 'value-by-collection-replacement' ? 'primary' : 'default'"
          @click="reportType = 'value-by-collection-replacement'"
        >
          Value by Collection (Replacement)
        </NButton>
        <NButton :type="reportType === 'recently-added' ? 'primary' : 'default'" @click="reportType = 'recently-added'">Recently Added</NButton>
        <NButton :type="reportType === 'recently-updated' ? 'primary' : 'default'" @click="reportType = 'recently-updated'">Recently Updated</NButton>
      </NSpace>
    </NCard>

    <NSpin :show="reportQuery.isPending.value">
      <div class="report-print-root">
        <header class="report-header">
          <p class="eyebrow">Bookshelves</p>
          <h3 class="report-title">{{ reportTitle }}</h3>
          <p class="page-subtitle">Generated on {{ new Date().toISOString().slice(0, 10) }}</p>
        </header>

        <template v-if="reportQuery.data.value?.kind === 'grouped'">
          <div v-if="reportQuery.data.value.data.length > 0" class="stack">
            <section
              v-for="group in reportQuery.data.value.data"
              :key="group.id ?? group.name"
              class="card report-group"
            >
              <div class="report-group-header">
                <h4>{{ reportType === "shelves" && group.id ? `${group.name} (${group.id})` : group.name }}</h4>
                <span class="muted-copy">{{ group.bookCount }} books</span>
              </div>
              <div v-if="group.books.length > 0" class="report-book-list">
                <div v-if="reportType === 'shelves'" class="report-book-row report-book-row-header report-book-row-shelf">
                  <strong>Author</strong>
                  <strong>Title</strong>
                  <strong>Coordinates</strong>
                  <strong>Year</strong>
                  <strong>Added</strong>
                </div>
                <div
                  v-for="book in booksForGroup(group.books)"
                  :key="book.id"
                  class="report-book-row"
                  :class="{
                    'report-book-row-author-first': reportType === 'collections',
                    'report-book-row-shelf': reportType === 'shelves'
                  }"
                >
                  <strong v-if="reportType === 'shelves' || reportType === 'collections'">{{ book.author ?? "—" }}</strong>
                  <strong v-else>{{ book.title }}</strong>
                  <span v-if="reportType === 'shelves' || reportType === 'collections'">{{ book.title }}</span>
                  <span v-else>{{ book.author ?? "—" }}</span>
                  <span v-if="reportType === 'shelves'">{{ book.shelfCoords ?? "—" }}</span>
                  <span>{{ book.year ?? "—" }}</span>
                  <span v-if="reportType !== 'shelves' && reportType !== 'collections'">{{ book.shelfId ?? "—" }}</span>
                  <span>{{ formatDate(book.dateAdded) }}</span>
                </div>
              </div>
              <p v-else class="muted-copy">No books in this group.</p>
            </section>
          </div>
          <NEmpty v-else description="No report data is available." />
        </template>

        <template v-else-if="reportQuery.data.value?.kind === 'recent'">
          <section class="card report-group">
            <div class="report-group-header">
              <h4>{{ reportType === "recently-added" ? "Most Recently Added Books" : "Most Recently Updated Books" }}</h4>
            </div>
            <table v-if="reportQuery.data.value.data.length > 0" class="table books-table recent-report-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Author</th>
                  <th>Shelf</th>
                  <th>{{ reportType === "recently-added" ? "Added" : "Updated" }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="book in reportQuery.data.value.data" :key="book.id">
                  <td>{{ book.title }}</td>
                  <td>{{ book.author ?? "—" }}</td>
                  <td>{{ book.shelfId ?? "—" }}</td>
                  <td>{{ reportType === "recently-added" ? formatDate(book.dateAdded) : formatDate(book.updatedAt) }}</td>
                </tr>
              </tbody>
            </table>
            <p v-else class="muted-copy">No report data is available.</p>
          </section>
        </template>

        <template v-else-if="reportQuery.data.value?.kind === 'collection-value'">
          <section class="card report-group">
            <div class="report-group-header">
              <h4>{{ reportTitle }}</h4>
            </div>
            <table v-if="reportQuery.data.value.data.length > 0" class="table books-table recent-report-table">
              <thead>
                <tr>
                  <th>Collection</th>
                  <th>Books</th>
                  <th>Value</th>
                  <th>Per Book</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in reportQuery.data.value.data" :key="row.id">
                  <td>{{ row.name }}</td>
                  <td>{{ row.bookCount }}</td>
                  <td>{{ formatCurrency(row.totalValue) }}</td>
                  <td>{{ formatCurrency(perBookValue(row.totalValue, row.bookCount)) }}</td>
                </tr>
              </tbody>
            </table>
            <p v-else class="muted-copy">No report data is available.</p>
          </section>
        </template>
      </div>
    </NSpin>
  </section>
</template>

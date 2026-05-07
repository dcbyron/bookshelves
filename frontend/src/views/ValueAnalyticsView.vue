<script setup lang="ts">
import { computed, h, reactive } from "vue";
import { useQuery } from "@tanstack/vue-query";
import { RouterLink } from "vue-router";
import {
  NButton,
  NCard,
  NCheckbox,
  NDataTable,
  NEmpty,
  NSelect,
  NSpin,
  NStatistic,
  NSpace
} from "naive-ui";

import { fetchCollections, fetchShelves, fetchValueDetails, fetchValueSummary } from "../api/catalog";
import type { ValueAnalyticsParams, ValueDetail } from "../types/catalog";
import { formatCurrency, formatDate } from "../utils/format";

const filters = reactive({
  basis: "paid",
  collectionId: "",
  shelfId: "",
  dateAddedMode: "any",
  dateAddedOn: "",
  dateAddedYear: "",
  specialOnly: false,
  digitalOnly: false,
  hardbackOnly: false,
  dustjacketOnly: false,
  slipcaseOnly: false,
  videoOnly: false
});

const analyticsParams = computed<ValueAnalyticsParams>(() => ({
  basis: filters.basis,
  collectionId: filters.collectionId || undefined,
  shelfId: filters.shelfId || undefined,
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
  specialOnly: filters.specialOnly,
  digitalOnly: filters.digitalOnly,
  hardbackOnly: filters.hardbackOnly,
  dustjacketOnly: filters.dustjacketOnly,
  slipcaseOnly: filters.slipcaseOnly,
  videoOnly: filters.videoOnly
}));

const collectionsQuery = useQuery({
  queryKey: ["collections"],
  queryFn: fetchCollections
});

const shelvesQuery = useQuery({
  queryKey: ["shelves"],
  queryFn: fetchShelves
});

const summaryQuery = useQuery({
  queryKey: computed(() => ["analytics", "summary", analyticsParams.value]),
  queryFn: () => fetchValueSummary(analyticsParams.value)
});

const detailsQuery = useQuery({
  queryKey: computed(() => ["analytics", "details", analyticsParams.value]),
  queryFn: () => fetchValueDetails(analyticsParams.value)
});

const basisOptions = [
  { label: "Best Estimate", value: "bestEstimate" },
  { label: "Price Paid", value: "paid" },
  { label: "Printed Price", value: "printed" },
  { label: "Replacement Price", value: "replacement" }
];

const collectionOptions = computed(() => [
  { label: "All collections", value: "" },
  ...((collectionsQuery.data.value ?? []).map((collection) => ({
    label: `${collection.name} (${collection.id})`,
    value: collection.id
  })))
]);

const shelfOptions = computed(() => [
  { label: "All shelves", value: "" },
  ...((shelvesQuery.data.value ?? []).map((shelf) => ({
    label: `${shelf.name} (${shelf.id})`,
    value: shelf.id
  })))
]);

const activeScopeLabel = computed(() => {
  const labels: string[] = [];
  if (filters.collectionId) {
    const collection = (collectionsQuery.data.value ?? []).find((item) => item.id === filters.collectionId);
    labels.push(collection ? `Collection: ${collection.name}` : `Collection: ${filters.collectionId}`);
  }
  if (filters.shelfId) {
    const shelf = (shelvesQuery.data.value ?? []).find((item) => item.id === filters.shelfId);
    labels.push(shelf ? `Shelf: ${shelf.name}` : `Shelf: ${filters.shelfId}`);
  }
  if (filters.dateAddedMode === "year" && filters.dateAddedYear) {
    labels.push(`Added in ${filters.dateAddedYear}`);
  } else if (filters.dateAddedMode !== "any" && filters.dateAddedOn) {
    labels.push(`Added ${filters.dateAddedMode} ${filters.dateAddedOn}`);
  }
  if (filters.specialOnly) {
    labels.push("Special");
  }
  if (filters.digitalOnly) {
    labels.push("Digital");
  }
  if (filters.hardbackOnly) {
    labels.push("Hardback");
  }
  if (filters.dustjacketOnly) {
    labels.push("Dustjacket");
  }
  if (filters.slipcaseOnly) {
    labels.push("Slipcase");
  }
  if (filters.videoOnly) {
    labels.push("Video");
  }
  return labels.length > 0 ? labels.join(" • ") : "Whole library";
});

const detailColumns = [
  {
    title: "Author",
    key: "author",
    minWidth: 180,
    render: (row: ValueDetail) => row.author ?? "—"
  },
  {
    title: "Title",
    key: "title",
    minWidth: 320,
    render: (row: ValueDetail) =>
      h(RouterLink, { to: `/books/${row.id}`, class: "table-link" }, { default: () => row.title })
  },
  {
    title: "Value",
    key: "valueAmount",
    width: 130,
    render: (row: ValueDetail) => formatCurrency(row.valueAmount)
  },
  {
    title: "Shelf",
    key: "shelfId",
    width: 120,
    render: (row: ValueDetail) => row.shelfId ?? "—"
  },
  {
    title: "Replacement Checked",
    key: "priceToReplaceChecked",
    width: 140,
    render: (row: ValueDetail) => formatDate(row.priceToReplaceChecked)
  }
];

function clearFilters() {
  filters.basis = "paid";
  filters.collectionId = "";
  filters.shelfId = "";
  filters.dateAddedMode = "any";
  filters.dateAddedOn = "";
  filters.dateAddedYear = "";
  filters.specialOnly = false;
  filters.digitalOnly = false;
  filters.hardbackOnly = false;
  filters.dustjacketOnly = false;
  filters.slipcaseOnly = false;
  filters.videoOnly = false;
}

const dateAddedInputLabel = computed(() => (filters.dateAddedMode === "year" ? "Added Year" : "Added Date"));
const dateAddedInputPlaceholder = computed(() => (filters.dateAddedMode === "year" ? "YYYY" : "YYYY-MM-DD"));

function updateCollectionFilter(value: string | null) {
  filters.collectionId = value ?? "";
}

function updateShelfFilter(value: string | null) {
  filters.shelfId = value ?? "";
}

function updateDateAddedMode(value: string) {
  filters.dateAddedMode = value;
  if (value === "year") {
    filters.dateAddedOn = "";
  } else {
    filters.dateAddedYear = "";
  }
}

function updateDateAddedInput(value: string) {
  if (filters.dateAddedMode === "year") {
    filters.dateAddedYear = value;
  } else {
    filters.dateAddedOn = value;
  }
}
</script>

<template>
  <section class="stack">
    <header class="page-header">
      <div>
        <p class="page-subtitle">
          Measure the library's value by best estimate, purchase price, printed price, or replacement estimate, then narrow the scope to the books that matter.
        </p>
      </div>
      <NSpace>
        <NButton secondary @click="summaryQuery.refetch(); detailsQuery.refetch()">Refresh</NButton>
        <NButton secondary @click="clearFilters">Reset</NButton>
      </NSpace>
    </header>

    <NCard title="Valuation Scope">
      <div class="filters">
        <div class="field">
          <label for="analytics-basis">Value Basis</label>
          <NSelect id="analytics-basis" v-model:value="filters.basis" :options="basisOptions" />
        </div>
        <div class="field">
          <label for="analytics-collection">Collection</label>
          <NSelect
            id="analytics-collection"
            :value="filters.collectionId || null"
            :options="collectionOptions"
            clearable
            @update:value="updateCollectionFilter"
          />
        </div>
        <div class="field">
          <label for="analytics-shelf">Shelf</label>
          <NSelect
            id="analytics-shelf"
            :value="filters.shelfId || null"
            :options="shelfOptions"
            clearable
            @update:value="updateShelfFilter"
          />
        </div>
        <div class="field">
          <label for="analytics-dateAddedMode">Date Added</label>
          <NSelect
            id="analytics-dateAddedMode"
            :value="filters.dateAddedMode"
            @update:value="updateDateAddedMode"
            :options="[
              { label: 'Any added date', value: 'any' },
              { label: 'Added in year', value: 'year' },
              { label: 'Added before', value: 'before' },
              { label: 'Added after', value: 'after' }
            ]"
          />
        </div>
        <div class="field">
          <label for="analytics-dateAddedOn">{{ dateAddedInputLabel }}</label>
          <input
            id="analytics-dateAddedOn"
            :value="filters.dateAddedMode === 'year' ? filters.dateAddedYear : filters.dateAddedOn"
            @input="updateDateAddedInput(($event.target as HTMLInputElement).value)"
            class="n-input__input-el"
            :placeholder="dateAddedInputPlaceholder"
            :disabled="filters.dateAddedMode === 'any'"
          />
        </div>
      </div>
      <div class="inline-actions top-gap">
        <label class="inline-actions">
          <NCheckbox v-model:checked="filters.specialOnly" />
          <span>Special</span>
        </label>
        <label class="inline-actions">
          <NCheckbox v-model:checked="filters.digitalOnly" />
          <span>Digital</span>
        </label>
        <label class="inline-actions">
          <NCheckbox v-model:checked="filters.hardbackOnly" />
          <span>Hardback</span>
        </label>
        <label class="inline-actions">
          <NCheckbox v-model:checked="filters.dustjacketOnly" />
          <span>Dustjacket</span>
        </label>
        <label class="inline-actions">
          <NCheckbox v-model:checked="filters.slipcaseOnly" />
          <span>Slipcase</span>
        </label>
        <label class="inline-actions">
          <NCheckbox v-model:checked="filters.videoOnly" />
          <span>Video</span>
        </label>
      </div>
      <p class="page-subtitle top-gap">Current scope: {{ activeScopeLabel }}</p>
    </NCard>

    <div class="dashboard-grid">
      <NCard>
        <NStatistic label="Total Value" :value="formatCurrency(summaryQuery.data.value?.totalValue)" />
      </NCard>
      <NCard>
        <NStatistic label="Books Included" :value="summaryQuery.data.value?.includedCount ?? 0" />
      </NCard>
      <NCard>
        <NStatistic label="Coverage" :value="summaryQuery.data.value ? `${summaryQuery.data.value.coveragePercent.toFixed(2)}%` : '0.00%'"/>
      </NCard>
      <NCard>
        <NStatistic label="Missing Value" :value="summaryQuery.data.value?.missingValueCount ?? 0" />
      </NCard>
      <NCard>
        <NStatistic label="Scoped Books" :value="summaryQuery.data.value?.totalScopedCount ?? 0" />
      </NCard>
      <NCard>
        <NStatistic label="Replacement Review Gaps" :value="summaryQuery.data.value?.missingReplacementCheckedCount ?? 0" />
      </NCard>
    </div>

    <div class="dashboard-grid dashboard-grid-two">
      <NCard title="Interpretation">
        <p class="page-subtitle compact-copy">
          Total value excludes books missing the chosen money field. Coverage shows how much of the current scope is represented in the sum.
        </p>
        <ul class="compact-list">
          <li>Included books: {{ summaryQuery.data.value?.includedCount ?? 0 }}</li>
          <li>Missing selected value: {{ summaryQuery.data.value?.missingValueCount ?? 0 }}</li>
          <li>Oldest replacement review: {{ formatDate(summaryQuery.data.value?.oldestReplacementChecked) }}</li>
        </ul>
      </NCard>
      <NCard title="Drill Down">
        <p class="page-subtitle compact-copy">
          Review the books behind the current total, including rows whose selected value is blank.
        </p>
        <ul class="compact-list">
          <li>Total rows in scope: {{ detailsQuery.data.value?.totalItems ?? 0 }}</li>
          <li>Selected basis: {{ filters.basis }}</li>
          <li>Books with blank value remain visible so coverage gaps are explicit.</li>
        </ul>
      </NCard>
    </div>

    <NCard title="Scoped Books">
      <NSpin :show="summaryQuery.isPending.value || detailsQuery.isPending.value">
        <NDataTable
          v-if="(detailsQuery.data.value?.items.length ?? 0) > 0"
          :columns="detailColumns"
          :data="detailsQuery.data.value?.items ?? []"
        />
        <NEmpty v-else description="No books match the current analytics scope." />
      </NSpin>
    </NCard>
  </section>
</template>

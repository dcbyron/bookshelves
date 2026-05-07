<script setup lang="ts">
import { computed, h, ref, watch } from "vue";
import { useQuery } from "@tanstack/vue-query";
import { RouterLink } from "vue-router";
import { NAlert, NButton, NCard, NDataTable, NEmpty, NSpace, NSpin, NStatistic, NTag } from "naive-ui";

import { fetchAuthors, fetchBooksByAuthor } from "../api/catalog";
import type { AuthorSummary, BookSummary } from "../types/catalog";
import { formatDate } from "../utils/format";

const authorsQuery = useQuery({
  queryKey: ["browse", "authors"],
  queryFn: fetchAuthors
});

const selectedAuthorKey = ref<string | null>(null);
const authorSort = ref<"name" | "count">("name");
const authorBooksSort = ref<"title" | "year" | "shelf" | "added">("title");

const sortedAuthors = computed(() => {
  const authors = [...(authorsQuery.data.value ?? [])];
  return authors.sort((left, right) => {
    if (authorSort.value === "count") {
      const countDifference = right.bookCount - left.bookCount;
      if (countDifference !== 0) {
        return countDifference;
      }
    }
    return left.name.localeCompare(right.name, undefined, { sensitivity: "base" });
  });
});

watch(
  () => sortedAuthors.value,
  (authors) => {
    if (!authors?.length) {
      selectedAuthorKey.value = null;
      return;
    }
    if (!selectedAuthorKey.value || !authors.some((author) => author.key === selectedAuthorKey.value)) {
      selectedAuthorKey.value = authors[0].key;
    }
  },
  { immediate: true }
);

const selectedAuthor = computed(() =>
  sortedAuthors.value.find((author) => author.key === selectedAuthorKey.value) ?? null
);

const authorBooksQuery = useQuery({
  queryKey: computed(() => ["browse", "author-books", selectedAuthorKey.value]),
  queryFn: () => fetchBooksByAuthor(selectedAuthorKey.value ?? ""),
  enabled: computed(() => Boolean(selectedAuthorKey.value))
});

const authorBooksLoading = computed(() =>
  Boolean(selectedAuthorKey.value) && authorBooksQuery.isFetching.value
);

const authorBooksEmptyDescription = computed(() =>
  selectedAuthor.value ? "No books are available for this author." : "Select an author to browse their books."
);

const sortedAuthorBooks = computed(() => {
  const books = [...(authorBooksQuery.data.value ?? [])];
  return books.sort((left, right) => {
    switch (authorBooksSort.value) {
      case "year": {
        const leftYear = left.year ?? Number.MAX_SAFE_INTEGER;
        const rightYear = right.year ?? Number.MAX_SAFE_INTEGER;
        if (leftYear !== rightYear) {
          return leftYear - rightYear;
        }
        break;
      }
      case "shelf": {
        const leftShelf = left.shelfId ?? "\uffff";
        const rightShelf = right.shelfId ?? "\uffff";
        const shelfComparison = leftShelf.localeCompare(rightShelf, undefined, { sensitivity: "base" });
        if (shelfComparison !== 0) {
          return shelfComparison;
        }
        break;
      }
      case "added": {
        const leftDate = left.dateAdded ?? "9999-12-31";
        const rightDate = right.dateAdded ?? "9999-12-31";
        if (leftDate !== rightDate) {
          return leftDate.localeCompare(rightDate);
        }
        break;
      }
      default: {
        const titleComparison = left.title.localeCompare(right.title, undefined, { sensitivity: "base" });
        if (titleComparison !== 0) {
          return titleComparison;
        }
      }
    }

    return left.title.localeCompare(right.title, undefined, { sensitivity: "base" });
  });
});

const authorColumns = [
  { title: "Author", key: "name" },
  { title: "Count", key: "bookCount", width: 90 }
];

const bookColumns = [
  {
    title: "Title",
    key: "title",
    minWidth: 320,
    render: (row: BookSummary) =>
      h(RouterLink, { to: `/books/${row.id}`, class: "table-link" }, { default: () => row.title })
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
    width: 180,
    render: (row: BookSummary) => row.shelfId ?? "—"
  },
  {
    title: "Added",
    key: "dateAdded",
    width: 116,
    render: (row: BookSummary) => formatDate(row.dateAdded)
  }
];
</script>

<template>
  <section class="stack">
    <header class="page-header">
      <div>
        <p class="page-subtitle">
          Browse the catalog author by author and jump directly into a writer's books.
        </p>
      </div>
      <NButton secondary @click="authorsQuery.refetch()">Refresh</NButton>
    </header>

    <div class="dashboard-grid dashboard-grid-two">
      <NCard>
        <NStatistic label="Author Buckets" :value="authorsQuery.data.value?.length ?? 0" />
      </NCard>
      <NCard>
        <NStatistic label="Books for Selected Author" :value="authorBooksQuery.data.value?.length ?? 0" />
      </NCard>
    </div>

    <div class="browse-layout">
      <NCard>
        <template #header>
          <NSpace align="center" justify="space-between">
            <span>Authors</span>
            <select id="author-sort" v-model="authorSort" class="compact-select">
              <option value="name">Sort by author</option>
              <option value="count">Sort by count</option>
            </select>
          </NSpace>
        </template>
        <NSpin :show="authorsQuery.isPending.value">
          <NDataTable
            v-if="sortedAuthors.length > 0"
            :columns="authorColumns"
            :data="sortedAuthors"
            :row-props="(row: AuthorSummary) => ({
              class: row.key === selectedAuthorKey ? 'browse-selected-row' : '',
              onClick: () => {
                selectedAuthorKey = row.key;
              }
            })"
          />
          <NEmpty v-else description="No authors are available yet." />
        </NSpin>
      </NCard>

      <NCard>
        <template #header>
          <NSpace align="center" justify="space-between">
            <NSpace align="center">
              <span>{{ selectedAuthor?.name ?? "Author Books" }}</span>
              <NTag v-if="selectedAuthor" round>{{ selectedAuthor.bookCount }} books</NTag>
            </NSpace>
            <select id="author-books-sort" v-model="authorBooksSort" class="compact-select">
              <option value="title">Sort by title</option>
              <option value="year">Sort by year</option>
              <option value="shelf">Sort by shelf</option>
              <option value="added">Sort by added</option>
            </select>
          </NSpace>
        </template>
        <NSpin :show="authorBooksLoading">
          <NAlert v-if="authorBooksQuery.isError.value" type="error" title="Could not load author books">
            {{ authorBooksQuery.error.value instanceof Error ? authorBooksQuery.error.value.message : "Unknown error" }}
          </NAlert>
          <NDataTable
            v-else-if="sortedAuthorBooks.length > 0"
            :columns="bookColumns"
            :data="sortedAuthorBooks"
          />
          <NEmpty v-else :description="authorBooksEmptyDescription" />
        </NSpin>
      </NCard>
    </div>
  </section>
</template>

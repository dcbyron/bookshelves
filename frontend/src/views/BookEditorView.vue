<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch, watchEffect } from "vue";
import { useMutation, useQuery, useQueryClient } from "@tanstack/vue-query";
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from "vue-router";
import type { LocationQueryRaw } from "vue-router";
import { NAlert, NButton, NCard, NInput, NSpace, NTag, useMessage } from "naive-ui";

import { createBook, deleteBook, fetchBook, fetchBookNavigation, updateBook } from "../api/catalog";
import BookForm from "../components/BookForm.vue";
import { useConfirmation } from "../composables/useConfirmation";
import { useReferenceData } from "../composables/useReferenceData";
import type { Book, BookQueryParams, BookRequest } from "../types/catalog";
import { formatDateTime } from "../utils/format";

const route = useRoute();
const router = useRouter();
const queryClient = useQueryClient();
const message = useMessage();
const { confirm } = useConfirmation();

const bookId = computed(() => (typeof route.params.id === "string" ? route.params.id : null));
const isEditing = computed(() => Boolean(bookId.value));
const isFormDirty = ref(false);
const allowNavigation = ref(false);

const booksContextQuery = computed<LocationQueryRaw | null>(() => {
  if (route.query.source !== "books") {
    return null;
  }
  const query: LocationQueryRaw = {};
  const keys = [
    "q",
    "collectionIds",
    "collectionMode",
    "noCollectionOnly",
    "shelfId",
    "year",
    "hasShelf",
    "specialOnly",
    "digitalOnly",
    "hardbackOnly",
    "dustjacketOnly",
    "slipcaseOnly",
    "videoOnly",
    "dateAddedMode",
    "dateAddedOn",
    "dateAddedYear",
    "lastAuditedMode",
    "lastAuditedOn",
    "sort",
    "direction",
    "page",
    "size"
  ] as const;
  keys.forEach((key) => {
    const value = route.query[key];
    if (typeof value === "string" && value.length > 0) {
      query[key] = value;
    }
  });
  return query;
});

const navigationParams = computed<BookQueryParams | null>(() => {
  if (!booksContextQuery.value) {
    return null;
  }
  return {
    q: typeof booksContextQuery.value.q === "string" ? booksContextQuery.value.q : undefined,
    collectionIds:
      typeof booksContextQuery.value.collectionIds === "string"
        ? booksContextQuery.value.collectionIds.split(",").map((value) => value.trim()).filter(Boolean)
        : undefined,
    collectionMode:
      typeof booksContextQuery.value.collectionMode === "string" ? booksContextQuery.value.collectionMode : undefined,
    noCollectionOnly: booksContextQuery.value.noCollectionOnly === "true" ? true : undefined,
    shelfId: typeof booksContextQuery.value.shelfId === "string" ? booksContextQuery.value.shelfId : undefined,
    year: typeof booksContextQuery.value.year === "string" ? Number(booksContextQuery.value.year) : undefined,
    hasShelf:
      typeof booksContextQuery.value.hasShelf === "string"
        ? booksContextQuery.value.hasShelf === "true"
        : undefined,
    specialOnly: booksContextQuery.value.specialOnly === "true" ? true : undefined,
    digitalOnly: booksContextQuery.value.digitalOnly === "true" ? true : undefined,
    hardbackOnly: booksContextQuery.value.hardbackOnly === "true" ? true : undefined,
    dustjacketOnly: booksContextQuery.value.dustjacketOnly === "true" ? true : undefined,
    slipcaseOnly: booksContextQuery.value.slipcaseOnly === "true" ? true : undefined,
    videoOnly: booksContextQuery.value.videoOnly === "true" ? true : undefined,
    dateAddedMode:
      typeof booksContextQuery.value.dateAddedMode === "string" ? booksContextQuery.value.dateAddedMode : undefined,
    dateAddedOn: typeof booksContextQuery.value.dateAddedOn === "string" ? booksContextQuery.value.dateAddedOn : undefined,
    dateAddedYear:
      typeof booksContextQuery.value.dateAddedYear === "string" ? Number(booksContextQuery.value.dateAddedYear) : undefined,
    lastAuditedMode:
      typeof booksContextQuery.value.lastAuditedMode === "string" ? booksContextQuery.value.lastAuditedMode : undefined,
    lastAuditedOn:
      typeof booksContextQuery.value.lastAuditedOn === "string" ? booksContextQuery.value.lastAuditedOn : undefined,
    sort: typeof booksContextQuery.value.sort === "string" ? booksContextQuery.value.sort : "title",
    direction: typeof booksContextQuery.value.direction === "string" ? booksContextQuery.value.direction : "asc"
  };
});

const backToResultsTarget = computed(() =>
  booksContextQuery.value
    ? { path: "/books", query: booksContextQuery.value }
    : { path: "/books" }
);

const emptyBook = (): BookRequest => ({
  title: "",
  author: null,
  publicationCity: null,
  publisher: null,
  imprint: null,
  year: null,
  firstPublished: null,
  edition: null,
  pages: null,
  frontpages: null,
  dateAdded: null,
  lastAudited: null,
  shelfId: null,
  authorSecondary: null,
  isbn: null,
  special: false,
  digital: false,
  hardback: false,
  dustjacket: false,
  slipcase: false,
  video: false,
  vols: 1,
  series: null,
  shelfCoords: null,
  note: null,
  pricePaid: null,
  priceOnItem: null,
  priceToReplace: null,
  priceToReplaceChecked: null,
  collectionIds: []
});

const form = reactive<BookRequest>(emptyBook());

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

const bookQuery = useQuery({
  queryKey: computed(() => ["book", bookId.value]),
  queryFn: () => fetchBook(bookId.value!),
  enabled: computed(() => Boolean(bookId.value))
});

const navigationQuery = useQuery({
  queryKey: computed(() => ["book-navigation", bookId.value, navigationParams.value]),
  queryFn: () => fetchBookNavigation(bookId.value!, navigationParams.value!),
  enabled: computed(() => Boolean(bookId.value && navigationParams.value))
});

const navigationSummary = computed(() => navigationQuery.data.value);

watchEffect(() => {
  if (bookQuery.data.value) {
    Object.assign(form, toBookRequest(bookQuery.data.value));
  } else if (!isEditing.value) {
    Object.assign(form, emptyBook());
  }
});

const saveMutation = useMutation({
  mutationFn: (payload: BookRequest) => {
    if (bookId.value) {
      return updateBook(bookId.value, payload);
    }
    return createBook(payload);
  },
  onSuccess: async (savedBook) => {
    isFormDirty.value = false;
    allowNavigation.value = true;
    await queryClient.invalidateQueries({ queryKey: ["books"] });
    await queryClient.invalidateQueries({ queryKey: ["book", savedBook.id] });
    await queryClient.invalidateQueries({ queryKey: ["book-navigation"] });
    message.success(bookId.value ? "Book updated" : "Book created");
    await router.push({ path: `/books/${savedBook.id}`, query: route.query });
    allowNavigation.value = false;
  }
});

const deleteMutation = useMutation({
  mutationFn: () => deleteBook(bookId.value!),
  onSuccess: async () => {
    isFormDirty.value = false;
    allowNavigation.value = true;
    await queryClient.invalidateQueries({ queryKey: ["books"] });
    message.success("Book deleted");
    await router.push(backToResultsTarget.value);
    allowNavigation.value = false;
  }
});

watch(bookId, () => {
  isFormDirty.value = false;
  saveMutation.reset();
  deleteMutation.reset();
});

const { collectionsQuery, shelvesQuery } = useReferenceData();

function handleSubmit(payload: BookRequest) {
  saveMutation.mutate(payload);
}

async function navigateToNeighbor(targetBookId: string | null) {
  if (!targetBookId) {
    return;
  }
  if (shouldWarnAboutUnsavedChanges()) {
    const confirmed = await confirmLeave();
    if (!confirmed) {
      return;
    }
    allowNavigation.value = true;
  }
  try {
    await router.push({ path: `/books/${targetBookId}`, query: route.query });
  } finally {
    allowNavigation.value = false;
  }
}

async function handleDelete() {
  if (!bookId.value) {
    return;
  }
  const confirmed = await confirm({
    title: "Delete book?",
    message: "Delete this book from the catalog? This action cannot be undone.",
    positiveText: "Delete book"
  });
  if (confirmed) {
    deleteMutation.mutate();
  }
}

function shouldWarnAboutUnsavedChanges() {
  return isFormDirty.value && !saveMutation.isPending.value && !deleteMutation.isPending.value && !allowNavigation.value;
}

function confirmLeave() {
  return confirm({
    title: "Discard unsaved changes?",
    message: "You have unsaved changes. Leave this page and discard them?",
    positiveText: "Discard changes"
  });
}

function handleBeforeUnload(event: BeforeUnloadEvent) {
  if (shouldWarnAboutUnsavedChanges()) {
    event.preventDefault();
    event.returnValue = "";
  }
}

onBeforeRouteLeave(async () => {
  if (!shouldWarnAboutUnsavedChanges()) {
    return true;
  }
  const confirmed = await confirmLeave();
  if (confirmed) {
    allowNavigation.value = true;
  }
  return confirmed;
});

onBeforeRouteUpdate(async () => {
  if (!shouldWarnAboutUnsavedChanges()) {
    return true;
  }
  const confirmed = await confirmLeave();
  if (confirmed) {
    allowNavigation.value = true;
  }
  return confirmed;
});

onMounted(() => {
  window.addEventListener("beforeunload", handleBeforeUnload);
});

onBeforeUnmount(() => {
  window.removeEventListener("beforeunload", handleBeforeUnload);
});
</script>

<template>
  <section class="stack">
    <header class="page-header">
      <div>
        <p class="page-subtitle">
          {{ isEditing ? "Update the catalog record and its collection assignments." : "Enter a new book into the catalog." }}
        </p>
        <NSpace v-if="bookQuery.data.value" class="top-gap" size="small">
          <NTag round>
            Id {{ bookQuery.data.value.id }}
          </NTag>
          <NTag type="info" round>
            Created {{ formatDateTime(bookQuery.data.value.createdAt) }}
          </NTag>
          <NTag type="success" round>
            Updated {{ formatDateTime(bookQuery.data.value.updatedAt) }}
          </NTag>
        </NSpace>
      </div>
      <div class="inline-actions">
        <RouterLink class="btn btn-secondary" :to="backToResultsTarget">Back to books</RouterLink>
        <NTag v-if="navigationSummary?.position" round>
          {{ navigationSummary.position }} of {{ navigationSummary.totalItems }}
        </NTag>
        <NButton
          v-if="navigationParams"
          secondary
          :disabled="!navigationSummary?.previousBookId"
          @click="navigateToNeighbor(navigationSummary?.previousBookId ?? null)"
        >
          Previous
        </NButton>
        <NButton
          v-if="navigationParams"
          secondary
          :disabled="!navigationSummary?.nextBookId"
          @click="navigateToNeighbor(navigationSummary?.nextBookId ?? null)"
        >
          Next
        </NButton>
        <NButton v-if="isEditing" type="error" @click="handleDelete">
          Delete
        </NButton>
      </div>
    </header>

    <NAlert v-if="bookQuery.isError.value" type="error" title="Could not load this book">
      {{ bookQuery.error.value instanceof Error ? bookQuery.error.value.message : "Unknown error" }}
    </NAlert>

    <NAlert v-if="saveMutation.isSuccess.value" type="success" title="Book saved">
      The record was saved successfully.
    </NAlert>

    <NAlert v-if="saveMutation.isError.value" type="error" title="Could not save the book">
      {{ saveMutation.error.value instanceof Error ? saveMutation.error.value.message : "Unknown error" }}
    </NAlert>

    <NAlert v-if="deleteMutation.isError.value" type="error" title="Could not delete the book">
      {{ deleteMutation.error.value instanceof Error ? deleteMutation.error.value.message : "Unknown error" }}
    </NAlert>

    <NCard v-if="bookQuery.isLoading.value" class="empty-state">Loading book...</NCard>

    <NCard v-else-if="bookQuery.data.value" title="Record Identity">
      <div class="field readonly-field">
        <label for="bookId">Book Id</label>
        <input id="bookId" class="readonly-input" :value="bookQuery.data.value.id" readonly />
      </div>
    </NCard>

    <BookForm
      v-if="!bookQuery.isLoading.value"
      :initial-value="form"
      :collections="collectionsQuery.data.value ?? []"
      :shelves="shelvesQuery.data.value ?? []"
      :submitting="saveMutation.isPending.value"
      @dirty-change="isFormDirty = $event"
      @submit="handleSubmit"
    />
  </section>
</template>

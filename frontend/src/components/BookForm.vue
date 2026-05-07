<script setup lang="ts">
import { computed, watch } from "vue";
import { useForm } from "vee-validate";
import { toTypedSchema } from "@vee-validate/zod";
import { z } from "zod";
import { NButton, NCard, NCheckbox, NInput, NInputNumber, NSelect, NTag } from "naive-ui";
import type { SelectOption } from "naive-ui";

import type { BookRequest, Collection, Shelf } from "../types/catalog";

const props = defineProps<{
  initialValue: BookRequest;
  collections: Collection[];
  shelves: Shelf[];
  submitting?: boolean;
}>();

const emit = defineEmits<{
  submit: [value: BookRequest];
  dirtyChange: [value: boolean];
}>();

const nullableString = z.preprocess((value) => {
  if (value === null || value === undefined) {
    return null;
  }
  if (typeof value === "string") {
    const trimmed = value.trim();
    return trimmed === "" ? null : trimmed;
  }
  return value;
}, z.string().nullable());

const nullableIsbn = z.preprocess((value) => {
  if (value === null || value === undefined) {
    return null;
  }
  if (typeof value === "string") {
    const trimmed = value.trim();
    return trimmed === "" ? null : trimmed;
  }
  return value;
}, z.string().max(13, "ISBN must be at most 13 characters").nullable());

const nullableInt = z.preprocess((value) => {
  if (value === "" || value === null || value === undefined) {
    return null;
  }
  const numberValue = Number(value);
  return Number.isNaN(numberValue) ? value : numberValue;
}, z.number().int().nonnegative().nullable());

const nullableMoney = z.preprocess((value) => {
  if (value === "" || value === null || value === undefined) {
    return null;
  }
  if (typeof value === "string") {
    const trimmed = value.trim();
    if (!/^\d+(\.\d{1,2})?$/.test(trimmed)) {
      return value;
    }
    return Number(trimmed);
  }
  const numberValue = Number(value);
  return Number.isNaN(numberValue) ? value : numberValue;
}, z.number().nonnegative().nullable());

const nullableDate = z.preprocess((value) => {
  if (value === "" || value === undefined) {
    return null;
  }
  return typeof value === "string" ? value.trim() : value;
}, z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Use YYYY-MM-DD").nullable());

const bookSchema = toTypedSchema(z.object({
  title: z.string().trim().min(1, "Title is required").max(500),
  author: nullableString,
  publicationCity: nullableString,
  publisher: nullableString,
  imprint: nullableString,
  year: nullableInt,
  firstPublished: nullableInt,
  edition: nullableString,
  pages: nullableInt,
  frontpages: nullableString,
  dateAdded: nullableDate,
  lastAudited: nullableDate,
  shelfId: nullableString,
  authorSecondary: nullableString,
  isbn: nullableIsbn,
  special: z.boolean(),
  digital: z.boolean(),
  hardback: z.boolean().nullable(),
  dustjacket: z.boolean(),
  slipcase: z.boolean(),
  video: z.boolean(),
  vols: nullableInt,
  series: nullableString,
  shelfCoords: nullableString,
  note: nullableString,
  pricePaid: nullableMoney,
  priceOnItem: nullableMoney,
  priceToReplace: nullableMoney,
  priceToReplaceChecked: nullableDate,
  collectionIds: z.array(z.string()).default([])
}));

function toDateInputValue(value: string | null | undefined) {
  return value ? value.slice(0, 10) : null;
}

function toMoneyInputValue(value: number | null | undefined) {
  return value === null || value === undefined ? null : value.toFixed(2);
}

function normalizeInitialValue(value: BookRequest): BookRequest {
  return {
    title: value.title ?? "",
    author: value.author ?? null,
    publicationCity: value.publicationCity ?? null,
    publisher: value.publisher ?? null,
    imprint: value.imprint ?? null,
    year: value.year ?? null,
    firstPublished: value.firstPublished ?? null,
    edition: value.edition ?? null,
    pages: value.pages ?? null,
    frontpages: value.frontpages ?? null,
    dateAdded: toDateInputValue(value.dateAdded),
    lastAudited: toDateInputValue(value.lastAudited),
    shelfId: value.shelfId ?? null,
    authorSecondary: value.authorSecondary ?? null,
    isbn: value.isbn ?? null,
    special: value.special ?? false,
    digital: value.digital ?? false,
    hardback: value.hardback ?? false,
    dustjacket: value.dustjacket ?? false,
    slipcase: value.slipcase ?? false,
    video: value.video ?? false,
    vols: value.vols ?? 1,
    series: value.series ?? null,
    shelfCoords: value.shelfCoords ?? null,
    note: value.note ?? null,
    pricePaid: toMoneyInputValue(value.pricePaid) as unknown as number | null,
    priceOnItem: toMoneyInputValue(value.priceOnItem) as unknown as number | null,
    priceToReplace: toMoneyInputValue(value.priceToReplace) as unknown as number | null,
    priceToReplaceChecked: toDateInputValue(value.priceToReplaceChecked),
    collectionIds: value.collectionIds ?? []
  };
}

const {
  errors,
  values,
  meta,
  defineField,
  handleSubmit,
  resetForm,
  setFieldValue
} = useForm<BookRequest>({
  validationSchema: bookSchema,
  initialValues: normalizeInitialValue(props.initialValue)
});

watch(
  () => props.initialValue,
  (value) => {
    resetForm({
      values: normalizeInitialValue(value)
    });
  },
  { deep: true }
);

watch(
  () => meta.value.dirty,
  (value) => emit("dirtyChange", value),
  { immediate: true }
);

const [title] = defineField("title");
const [author] = defineField("author");
const [publicationCity] = defineField("publicationCity");
const [publisher] = defineField("publisher");
const [imprint] = defineField("imprint");
const [year] = defineField("year");
const [firstPublished] = defineField("firstPublished");
const [edition] = defineField("edition");
const [pages] = defineField("pages");
const [frontpages] = defineField("frontpages");
const [dateAdded] = defineField("dateAdded");
const [lastAudited] = defineField("lastAudited");
const [shelfId] = defineField("shelfId");
const [authorSecondary] = defineField("authorSecondary");
const [isbn] = defineField("isbn");
const [special] = defineField("special");
const [digital] = defineField("digital");
const [hardback] = defineField("hardback");
const [dustjacket] = defineField("dustjacket");
const [slipcase] = defineField("slipcase");
const [video] = defineField("video");
const [vols] = defineField("vols");
const [series] = defineField("series");
const [shelfCoords] = defineField("shelfCoords");
const [note] = defineField("note");
const [pricePaid] = defineField("pricePaid");
const [priceOnItem] = defineField("priceOnItem");
const [priceToReplace] = defineField("priceToReplace");
const [priceToReplaceChecked] = defineField("priceToReplaceChecked");

const activeCollectionIds = computed(() => values.collectionIds ?? []);
const shelfOptions = computed<SelectOption[]>(() => [
  { label: "Unassigned", value: "" },
  ...props.shelves.map((shelf) => ({
    label: `${shelf.name} (${shelf.id})`,
    value: shelf.id
  }))
]);
const selectedShelfId = computed<string | null>({
  get: () => shelfId.value ?? null,
  set: (value) => {
    shelfId.value = value ?? null;
  }
});
const hardbackChecked = computed<boolean>({
  get: () => Boolean(hardback.value),
  set: (value) => {
    hardback.value = value;
  }
});

function moneyFieldAdapter(field: { value: unknown }) {
  return computed<string>({
    get: () => {
      if (field.value === null || field.value === undefined) {
        return "";
      }
      if (typeof field.value === "number") {
        return field.value.toFixed(2);
      }
      return String(field.value);
    },
    set: (value) => {
      field.value = value as unknown;
    }
  });
}

const pricePaidText = moneyFieldAdapter(pricePaid);
const priceOnItemText = moneyFieldAdapter(priceOnItem);
const priceToReplaceText = moneyFieldAdapter(priceToReplace);

function toggleCollection(id: string) {
  const next = activeCollectionIds.value.includes(id)
    ? activeCollectionIds.value.filter((value) => value !== id)
    : [...activeCollectionIds.value, id];
  setFieldValue("collectionIds", next);
}

const onSubmit = handleSubmit((submittedValues) => {
  emit("submit", submittedValues);
});
</script>

<template>
  <form class="stack" @submit.prevent="onSubmit">
    <NCard title="Bibliographic Details">
      <div class="grid grid-two">
        <div class="field">
          <label for="title">Title</label>
          <NInput id="title" v-model:value="title" placeholder="Book title" />
          <span v-if="errors.title" class="field-error">{{ errors.title }}</span>
        </div>
        <div class="field">
          <label for="author">Author</label>
          <NInput id="author" v-model:value="author" placeholder="Primary author" />
        </div>
        <div class="field">
          <label for="authorSecondary">Secondary Author</label>
          <NInput id="authorSecondary" v-model:value="authorSecondary" placeholder="Secondary author" />
        </div>
        <div class="field">
          <label for="isbn">ISBN</label>
          <NInput id="isbn" v-model:value="isbn" placeholder="978..." />
          <span v-if="errors.isbn" class="field-error">{{ errors.isbn }}</span>
        </div>
        <div class="field">
          <label for="publicationCity">Publication City</label>
          <NInput id="publicationCity" v-model:value="publicationCity" placeholder="New York, London, etc." />
        </div>
        <div class="field">
          <label for="publisher">Publisher</label>
          <NInput id="publisher" v-model:value="publisher" placeholder="Publisher name" />
        </div>
        <div class="field">
          <label for="imprint">Imprint</label>
          <NInput id="imprint" v-model:value="imprint" placeholder="Imprint name" />
        </div>
        <div class="field">
          <label for="edition">Edition</label>
          <NInput id="edition" v-model:value="edition" placeholder="1st, Revised, etc." />
        </div>
        <div class="field">
          <label for="year">Year</label>
          <NInputNumber id="year" v-model:value="year" :show-button="false" clearable />
          <span v-if="errors.year" class="field-error">{{ errors.year }}</span>
        </div>
        <div class="field">
          <label for="firstPublished">First Published</label>
          <NInputNumber id="firstPublished" v-model:value="firstPublished" :show-button="false" clearable />
          <span v-if="errors.firstPublished" class="field-error">{{ errors.firstPublished }}</span>
        </div>
        <div class="field">
          <label for="frontpages">Frontpages</label>
          <NInput id="frontpages" v-model:value="frontpages" placeholder="xii, xviii, etc." />
        </div>
        <div class="field">
          <label for="pages">Pages</label>
          <NInputNumber id="pages" v-model:value="pages" :show-button="false" clearable />
          <span v-if="errors.pages" class="field-error">{{ errors.pages }}</span>
        </div>
        <div class="field">
          <label for="vols">Volumes</label>
          <NInputNumber id="vols" v-model:value="vols" :show-button="false" clearable />
          <span v-if="errors.vols" class="field-error">{{ errors.vols }}</span>
        </div>
        <div class="field">
          <label for="series">Series</label>
          <NInput id="series" v-model:value="series" placeholder="Series name" />
        </div>
        <div class="field">
          <label for="dateAdded">Date Added</label>
          <NInput id="dateAdded" v-model:value="dateAdded" placeholder="YYYY-MM-DD" />
          <span v-if="errors.dateAdded" class="field-error">{{ errors.dateAdded }}</span>
        </div>
        <div class="field">
          <label for="lastAudited">Last Audited</label>
          <NInput id="lastAudited" v-model:value="lastAudited" placeholder="YYYY-MM-DD" />
          <span v-if="errors.lastAudited" class="field-error">{{ errors.lastAudited }}</span>
        </div>
      </div>
    </NCard>

    <NCard title="Catalog Details">
      <div class="grid grid-two">
        <div class="field">
          <label for="shelfId">Shelf</label>
          <NSelect id="shelfId" v-model:value="selectedShelfId" :options="shelfOptions" clearable placeholder="Assign a shelf" />
        </div>
        <div class="field">
          <label for="shelfCoords">Shelf Coordinates</label>
          <NInput id="shelfCoords" v-model:value="shelfCoords" placeholder="A-1, top row, etc." />
        </div>
        <div class="field">
          <label for="pricePaid">Price Paid</label>
          <NInput id="pricePaid" v-model:value="pricePaidText" inputmode="decimal" placeholder="0.00" />
          <span v-if="errors.pricePaid" class="field-error">{{ errors.pricePaid }}</span>
        </div>
        <div class="field">
          <label for="priceOnItem">Price On Item</label>
          <NInput id="priceOnItem" v-model:value="priceOnItemText" inputmode="decimal" placeholder="0.00" />
          <span v-if="errors.priceOnItem" class="field-error">{{ errors.priceOnItem }}</span>
        </div>
        <div class="field">
          <label for="priceToReplace">Replacement Price</label>
          <NInput id="priceToReplace" v-model:value="priceToReplaceText" inputmode="decimal" placeholder="0.00" />
          <span v-if="errors.priceToReplace" class="field-error">{{ errors.priceToReplace }}</span>
        </div>
        <div class="field">
          <label for="priceToReplaceChecked">Replacement Checked</label>
          <NInput id="priceToReplaceChecked" v-model:value="priceToReplaceChecked" placeholder="YYYY-MM-DD" />
          <span v-if="errors.priceToReplaceChecked" class="field-error">{{ errors.priceToReplaceChecked }}</span>
        </div>
      </div>

      <div class="grid grid-two top-gap">
        <div class="field">
          <NCheckbox id="special" v-model:checked="special">Special</NCheckbox>
        </div>
        <div class="field">
          <NCheckbox id="digital" v-model:checked="digital">Digital</NCheckbox>
        </div>
        <div class="field">
          <NCheckbox id="hardback" v-model:checked="hardbackChecked">Hardback</NCheckbox>
        </div>
        <div class="field">
          <NCheckbox id="dustjacket" v-model:checked="dustjacket">Dustjacket</NCheckbox>
        </div>
        <div class="field">
          <NCheckbox id="slipcase" v-model:checked="slipcase">Slipcase</NCheckbox>
        </div>
        <div class="field">
          <NCheckbox id="video" v-model:checked="video">Video</NCheckbox>
        </div>
      </div>
    </NCard>

    <NCard title="Notes and Collections">
      <div class="stack">
      <div class="field">
        <label for="note">Notes</label>
        <NInput
          id="note"
          v-model:value="note"
          type="textarea"
          placeholder="Catalog notes, provenance, condition, and other details"
        />
      </div>

      <div class="field">
        <label>Collections</label>
        <div class="pill-list collection-button-surface">
          <button
            v-for="collection in collections"
            :key="collection.id"
            class="btn compact-pill-button"
            :class="activeCollectionIds.includes(collection.id) ? 'btn-primary' : 'btn-secondary'"
            type="button"
            @click="toggleCollection(collection.id)"
          >
            <NTag size="small" :bordered="false" :type="activeCollectionIds.includes(collection.id) ? 'success' : 'default'" round>
              {{ collection.name }}
            </NTag>
          </button>
        </div>
      </div>
      </div>
    </NCard>

    <div class="inline-actions">
      <NButton attr-type="submit" type="primary" :loading="submitting">
        Save book
      </NButton>
    </div>
  </form>
</template>

<script setup lang="ts">
import { computed, h, reactive, ref } from "vue";
import { useMutation, useQuery, useQueryClient } from "@tanstack/vue-query";
import {
  NAlert,
  NButton,
  NCard,
  NDataTable,
  NEmpty,
  NInput,
  NSpace,
  NSpin,
  useMessage
} from "naive-ui";

import { createCollection, deleteCollection, fetchCollections } from "../api/catalog";
import { useConfirmation } from "../composables/useConfirmation";

const queryClient = useQueryClient();
const message = useMessage();
const { confirm } = useConfirmation();
const form = reactive({
  id: "",
  name: ""
});
const sortMode = ref<"name" | "count">("name");

function trimmedForm() {
  return {
    id: form.id.trim(),
    name: form.name.trim()
  };
}

function canCreateCollection() {
  return form.id.trim().length > 0 && form.name.trim().length > 0;
}

const collectionsQuery = useQuery({
  queryKey: ["collections"],
  queryFn: fetchCollections
});

const sortedCollections = computed(() => {
  const items = [...(collectionsQuery.data.value ?? [])];
  if (sortMode.value === "count") {
    return items.sort((left, right) => {
      const countDifference = (right.usageCount ?? 0) - (left.usageCount ?? 0);
      if (countDifference !== 0) {
        return countDifference;
      }
      return left.name.localeCompare(right.name, undefined, { sensitivity: "base" });
    });
  }
  return items.sort((left, right) => left.name.localeCompare(right.name, undefined, { sensitivity: "base" }));
});

const createMutation = useMutation({
  mutationFn: () => createCollection(trimmedForm()),
  onSuccess: async () => {
    form.id = "";
    form.name = "";
    message.success("Collection added");
    await queryClient.invalidateQueries({ queryKey: ["collections"] });
  }
});

const createDisabled = () => !canCreateCollection() || createMutation.isPending.value;

const deleteMutation = useMutation({
  mutationFn: (id: string) => deleteCollection(id),
  onSuccess: async () => {
    message.success("Collection deleted");
    await queryClient.invalidateQueries({ queryKey: ["collections"] });
  }
});

async function handleDeleteCollection(row: { id: string; name: string; usageCount?: number }) {
  const usageCount = row.usageCount ?? 0;
  const confirmed = await confirm({
    title: "Delete collection?",
    ...(usageCount > 0
      ? {
          message: `Delete the collection "${row.name}" (${row.id})? It is still assigned to ${usageCount} book(s), so deletion will be blocked until those assignments are removed.`,
          positiveText: "Attempt delete"
        }
      : {
          message: `Delete the collection "${row.name}" (${row.id})?`,
          positiveText: "Delete collection"
        })
  });
  if (confirmed) {
    deleteMutation.mutate(row.id);
  }
}

const columns = [
  { title: "Name", key: "name", width: 320 },
  { title: "Id", key: "id", width: 220 },
  { title: "Books", key: "usageCount", width: 100 },
  {
    title: "",
    key: "actions",
    width: 120,
    render: (row: { id: string }) =>
      h(
        NButton,
        {
          type: "error",
          secondary: true,
          onClick: () => handleDeleteCollection(row as { id: string; name: string; usageCount?: number })
        },
        { default: () => "Delete" }
      )
  }
];
</script>

<template>
  <section class="stack">
    <header class="page-header">
      <div>
        <p class="page-subtitle">Manage the named collection buckets used across the catalog.</p>
      </div>
    </header>

    <NAlert v-if="collectionsQuery.isError.value" type="error" title="Could not load collections">
      {{ collectionsQuery.error.value instanceof Error ? collectionsQuery.error.value.message : "Unknown error" }}
    </NAlert>

    <NAlert v-if="createMutation.isError.value" type="error" title="Could not add collection">
      {{ createMutation.error.value instanceof Error ? createMutation.error.value.message : "Unknown error" }}
    </NAlert>

    <NAlert v-if="deleteMutation.isError.value" type="error" title="Could not delete collection">
      {{ deleteMutation.error.value instanceof Error ? deleteMutation.error.value.message : "Unknown error" }}
    </NAlert>

    <div class="dashboard-grid dashboard-grid-two">
      <NCard title="Add Collection">
        <div class="stack">
          <div class="field">
            <label for="collection-id">Collection id</label>
            <NInput id="collection-id" v-model:value="form.id" placeholder="general" />
          </div>
          <div class="field">
            <label for="collection-name">Collection name</label>
            <NInput id="collection-name" v-model:value="form.name" placeholder="General" />
          </div>
          <NSpace justify="end">
            <NButton
              type="primary"
              :loading="createMutation.isPending.value"
              :disabled="createDisabled()"
              @click="createMutation.mutate()"
            >
              Add collection
            </NButton>
          </NSpace>
        </div>
      </NCard>

      <NCard title="Collection Notes">
        <p class="page-subtitle compact-copy">
          Keep ids short and stable. The Books column shows how many catalog records still reference each collection.
        </p>
      </NCard>
    </div>

    <NCard>
      <template #header>Existing Collections</template>
      <template #header-extra>
        <select id="collection-sort" v-model="sortMode" class="compact-select">
          <option value="name">Sort by name</option>
          <option value="count">Sort by count</option>
        </select>
      </template>
      <NSpin :show="collectionsQuery.isLoading.value">
        <NDataTable
          v-if="sortedCollections.length"
          :columns="columns"
          :data="sortedCollections"
          :bordered="false"
        />
        <NEmpty v-else description="No collections yet." class="empty-state" />
      </NSpin>
    </NCard>
  </section>
</template>

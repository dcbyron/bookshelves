<script setup lang="ts">
import { computed, ref } from "vue";
import { useMutation, useQuery, useQueryClient } from "@tanstack/vue-query";
import { NAlert, NButton, NCard, NEmpty, NInput, NSpin, useMessage } from "naive-ui";

import { fetchPublishers, renamePublisher } from "../api/catalog";
import type { PublisherRenameResponse, PublisherSummary } from "../types/catalog";

const queryClient = useQueryClient();
const message = useMessage();

const sortMode = ref<"name" | "count">("name");
const editingPublisher = ref<string | null>(null);
const draftPublisher = ref("");

const publishersQuery = useQuery({
  queryKey: ["publishers"],
  queryFn: fetchPublishers
});

const sortedPublishers = computed(() => {
  const items: PublisherSummary[] = [...(publishersQuery.data.value ?? [])];
  if (sortMode.value === "count") {
    return items.sort((left, right) => {
      const countDifference = right.bookCount - left.bookCount;
      if (countDifference !== 0) {
        return countDifference;
      }
      return left.publisher.localeCompare(right.publisher, undefined, { sensitivity: "base" });
    });
  }
  return items.sort((left, right) => left.publisher.localeCompare(right.publisher, undefined, { sensitivity: "base" }));
});

function beginRename(publisher: string) {
  editingPublisher.value = publisher;
  draftPublisher.value = publisher;
}

function cancelRename() {
  editingPublisher.value = null;
  draftPublisher.value = "";
}

const renameMutation = useMutation<PublisherRenameResponse, Error, { sourcePublisher: string; targetPublisher: string }>({
  mutationFn: ({ sourcePublisher, targetPublisher }: { sourcePublisher: string; targetPublisher: string }) =>
    renamePublisher({ sourcePublisher, targetPublisher }),
  onSuccess: async (response) => {
    message.success(`Updated ${response.updatedCount} book(s) to ${response.publisher}`);
    cancelRename();
    await queryClient.invalidateQueries({ queryKey: ["publishers"] });
  }
});

function saveRename(sourcePublisher: string) {
  renameMutation.mutate({
    sourcePublisher,
    targetPublisher: draftPublisher.value.trim()
  });
}

function saveDisabled(sourcePublisher: string) {
  const trimmed = draftPublisher.value.trim();
  return renameMutation.isPending.value || trimmed.length === 0 || trimmed === sourcePublisher;
}
</script>

<template>
  <section class="stack">
    <header class="page-header">
      <div>
        <p class="page-subtitle">
          Normalize publisher names across existing books by bulk-renaming one exact stored value at a time.
        </p>
      </div>
    </header>

    <NAlert v-if="publishersQuery.isError.value" type="error" title="Could not load publishers">
      {{ publishersQuery.error.value instanceof Error ? publishersQuery.error.value.message : "Unknown error" }}
    </NAlert>

    <NAlert v-if="renameMutation.isError.value" type="error" title="Could not rename publisher">
      {{ renameMutation.error.value instanceof Error ? renameMutation.error.value.message : "Unknown error" }}
    </NAlert>

    <div class="dashboard-grid dashboard-grid-two">
      <NCard title="Publisher Notes">
        <p class="page-subtitle compact-copy">
          Use this maintenance screen to consolidate variant publisher spellings into one canonical string.
        </p>
      </NCard>
      <NCard title="Workflow">
        <ul class="compact-list">
          <li>Sort alphabetically to scan for near-duplicate names.</li>
          <li>Sort by count to normalize the heaviest variants first.</li>
          <li>Rename one row to update every matching book in one step.</li>
        </ul>
      </NCard>
    </div>

    <NCard>
      <template #header>Existing Publishers</template>
      <template #header-extra>
        <select id="publisher-sort" v-model="sortMode" class="compact-select">
          <option value="name">Sort by publisher</option>
          <option value="count">Sort by count</option>
        </select>
      </template>

      <NSpin :show="publishersQuery.isLoading.value">
        <table v-if="sortedPublishers.length" class="table publisher-table">
          <thead>
            <tr>
              <th>Publisher</th>
              <th>Books</th>
              <th />
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in sortedPublishers" :key="row.publisher">
              <td>
                <NInput
                  v-if="editingPublisher === row.publisher"
                  :id="`publisher-rename-${row.publisher}`"
                  v-model:value="draftPublisher"
                  placeholder="Canonical publisher name"
                />
                <template v-else>{{ row.publisher }}</template>
              </td>
              <td>{{ row.bookCount }}</td>
              <td>
                <div class="inline-actions">
                  <template v-if="editingPublisher === row.publisher">
                    <NButton
                      type="primary"
                      size="small"
                      :disabled="saveDisabled(row.publisher)"
                      :loading="renameMutation.isPending.value"
                      @click="saveRename(row.publisher)"
                    >
                      Save
                    </NButton>
                    <NButton secondary size="small" @click="cancelRename">Cancel</NButton>
                  </template>
                  <template v-else>
                    <NButton secondary size="small" @click="beginRename(row.publisher)">Rename</NButton>
                  </template>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <NEmpty v-else description="No publishers yet." class="empty-state" />
      </NSpin>
    </NCard>
  </section>
</template>

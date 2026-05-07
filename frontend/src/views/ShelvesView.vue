<script setup lang="ts">
import { h, reactive } from "vue";
import { useMutation, useQuery, useQueryClient } from "@tanstack/vue-query";
import {
  NAlert,
  NButton,
  NCard,
  NCheckbox,
  NDataTable,
  NEmpty,
  NInput,
  NSpace,
  NSpin,
  useMessage
} from "naive-ui";

import {
  createShelf,
  deleteShelf,
  fetchShelfCoordinateAudits,
  fetchShelves,
  updateShelf,
  updateShelfCoordinateAudit
} from "../api/catalog";
import { useConfirmation } from "../composables/useConfirmation";
import type { Shelf, ShelfCoordinateAudit } from "../types/catalog";

const queryClient = useQueryClient();
const message = useMessage();
const { confirm } = useConfirmation();
const form = reactive({
  id: "",
  name: "",
  audited: false
});

function trimmedForm() {
  return {
    id: form.id.trim(),
    name: form.name.trim(),
    audited: form.audited
  };
}

function canCreateShelf() {
  return form.id.trim().length > 0 && form.name.trim().length > 0;
}

const shelvesQuery = useQuery({
  queryKey: ["shelves"],
  queryFn: fetchShelves
});

const coordinateAuditsQuery = useQuery({
  queryKey: ["shelf-coordinate-audits"],
  queryFn: fetchShelfCoordinateAudits
});

const createMutation = useMutation({
  mutationFn: () => createShelf(trimmedForm()),
  onSuccess: async () => {
    form.id = "";
    form.name = "";
    form.audited = false;
    message.success("Shelf added");
    await queryClient.invalidateQueries({ queryKey: ["shelves"] });
  }
});

const createDisabled = () => !canCreateShelf() || createMutation.isPending.value;

const deleteMutation = useMutation({
  mutationFn: (id: string) => deleteShelf(id),
  onSuccess: async () => {
    message.success("Shelf deleted");
    await queryClient.invalidateQueries({ queryKey: ["shelves"] });
  }
});

const updateAuditMutation = useMutation({
  mutationFn: ({ row, audited }: { row: Shelf; audited: boolean }) =>
    updateShelf(row.id, { id: row.id, name: row.name, audited }),
  onMutate: async ({ row, audited }) => {
    await queryClient.cancelQueries({ queryKey: ["shelves"] });
    const previousShelves = queryClient.getQueryData<Shelf[]>(["shelves"]);
    queryClient.setQueryData<Shelf[]>(["shelves"], (current = []) =>
      current.map((shelf) => (shelf.id === row.id ? { ...shelf, audited } : shelf))
    );
    return { previousShelves };
  },
  onError: (_error, _variables, context) => {
    if (context?.previousShelves) {
      queryClient.setQueryData(["shelves"], context.previousShelves);
    }
  },
  onSettled: async () => {
    await queryClient.invalidateQueries({ queryKey: ["shelves"] });
  }
});

const updateCoordinateAuditMutation = useMutation({
  mutationFn: ({ row, audited }: { row: ShelfCoordinateAudit; audited: boolean }) =>
    updateShelfCoordinateAudit(row.shelfId, row.shelfCoords, audited),
  onMutate: async ({ row, audited }) => {
    await queryClient.cancelQueries({ queryKey: ["shelf-coordinate-audits"] });
    const previousAudits = queryClient.getQueryData<ShelfCoordinateAudit[]>(["shelf-coordinate-audits"]);
    queryClient.setQueryData<ShelfCoordinateAudit[]>(["shelf-coordinate-audits"], (current = []) =>
      current.map((audit) =>
        audit.shelfId === row.shelfId && audit.shelfCoords === row.shelfCoords
          ? { ...audit, audited }
          : audit
      )
    );
    return { previousAudits };
  },
  onError: (_error, _variables, context) => {
    if (context?.previousAudits) {
      queryClient.setQueryData(["shelf-coordinate-audits"], context.previousAudits);
    }
  },
  onSettled: async () => {
    await queryClient.invalidateQueries({ queryKey: ["shelf-coordinate-audits"] });
  }
});

async function handleDeleteShelf(row: { id: string; name: string; usageCount?: number }) {
  const usageCount = row.usageCount ?? 0;
  const confirmed = await confirm({
    title: "Delete shelf?",
    ...(usageCount > 0
      ? {
          message: `Delete the shelf "${row.name}" (${row.id})? It is still assigned to ${usageCount} book(s), so deletion will be blocked until those books are moved.`,
          positiveText: "Attempt delete"
        }
      : {
          message: `Delete the shelf "${row.name}" (${row.id})?`,
          positiveText: "Delete shelf"
        })
  });
  if (confirmed) {
    deleteMutation.mutate(row.id);
  }
}

const columns = [
  { title: "Name", key: "name", width: 320 },
  { title: "Id", key: "id", width: 220 },
  {
    title: "Audited",
    key: "audited",
    width: 100,
    render: (row: Shelf) =>
      h(
        NCheckbox,
        {
          checked: Boolean(row.audited),
          disabled: updateAuditMutation.isPending.value,
          "aria-label": `Mark ${row.name} audited`,
          "onUpdate:checked": (checked: boolean) => updateAuditMutation.mutate({ row, audited: checked })
        }
      )
  },
  { title: "Books", key: "usageCount", width: 100 },
  {
    title: "",
    key: "actions",
    width: 120,
    render: (row: Shelf) =>
      h(
        NButton,
        {
          type: "error",
          secondary: true,
          class: (row.usageCount ?? 0) === 0 ? "delete-ready-button" : undefined,
          onClick: () => handleDeleteShelf(row as { id: string; name: string; usageCount?: number })
        },
        { default: () => "Delete" }
      )
  }
];

const coordinateColumns = [
  { title: "Shelf", key: "shelfName", width: 320 },
  { title: "Coordinates", key: "shelfCoords", width: 220 },
  {
    title: "Audited",
    key: "audited",
    width: 100,
    render: (row: ShelfCoordinateAudit) =>
      h(
        NCheckbox,
        {
          checked: Boolean(row.audited),
          disabled: updateCoordinateAuditMutation.isPending.value,
          "aria-label": `Mark ${row.shelfName} ${row.shelfCoords} audited`,
          "onUpdate:checked": (checked: boolean) => updateCoordinateAuditMutation.mutate({ row, audited: checked })
        }
      )
  },
  { title: "", key: "booksSpacer", width: 100 },
  { title: "", key: "actionsSpacer", width: 120 }
];
</script>

<template>
  <section class="stack">
    <header class="page-header">
      <div>
        <p class="page-subtitle">Track the physical placement vocabulary used by the library.</p>
      </div>
    </header>

    <NAlert v-if="shelvesQuery.isError.value" type="error" title="Could not load shelves">
      {{ shelvesQuery.error.value instanceof Error ? shelvesQuery.error.value.message : "Unknown error" }}
    </NAlert>

    <NAlert v-if="createMutation.isError.value" type="error" title="Could not add shelf">
      {{ createMutation.error.value instanceof Error ? createMutation.error.value.message : "Unknown error" }}
    </NAlert>

    <NAlert v-if="deleteMutation.isError.value" type="error" title="Could not delete shelf">
      {{ deleteMutation.error.value instanceof Error ? deleteMutation.error.value.message : "Unknown error" }}
    </NAlert>

    <NAlert v-if="updateAuditMutation.isError.value" type="error" title="Could not update shelf audit status">
      {{ updateAuditMutation.error.value instanceof Error ? updateAuditMutation.error.value.message : "Unknown error" }}
    </NAlert>

    <NAlert v-if="coordinateAuditsQuery.isError.value" type="error" title="Could not load coordinate audits">
      {{ coordinateAuditsQuery.error.value instanceof Error ? coordinateAuditsQuery.error.value.message : "Unknown error" }}
    </NAlert>

    <NAlert v-if="updateCoordinateAuditMutation.isError.value" type="error" title="Could not update coordinate audit status">
      {{ updateCoordinateAuditMutation.error.value instanceof Error ? updateCoordinateAuditMutation.error.value.message : "Unknown error" }}
    </NAlert>

    <div class="dashboard-grid dashboard-grid-two">
      <NCard title="Add Shelf">
        <div class="stack">
          <div class="field">
            <label for="shelf-id">Shelf id</label>
            <NInput id="shelf-id" v-model:value="form.id" placeholder="shelf-a" />
          </div>
          <div class="field">
            <label for="shelf-name">Shelf name</label>
            <NInput id="shelf-name" v-model:value="form.name" placeholder="General shelf" />
          </div>
          <NCheckbox v-model:checked="form.audited">Audited</NCheckbox>
          <NSpace justify="end">
            <NButton
              type="primary"
              :loading="createMutation.isPending.value"
              :disabled="createDisabled()"
              @click="createMutation.mutate()"
            >
              Add shelf
            </NButton>
          </NSpace>
        </div>
      </NCard>

      <NCard title="Shelf Notes">
        <p class="page-subtitle compact-copy">
          Shelf ids should point to stable physical locations. The Books column shows how many records are still placed on each shelf.
        </p>
      </NCard>
    </div>

    <NCard title="Existing Shelves">
      <NSpin :show="shelvesQuery.isLoading.value">
        <NDataTable
          v-if="shelvesQuery.data.value?.length"
          :columns="columns"
          :data="shelvesQuery.data.value"
          :bordered="false"
        />
        <NEmpty v-else description="No shelves yet." class="empty-state" />
      </NSpin>
    </NCard>

    <NCard title="Coordinates Audit">
      <NSpin :show="coordinateAuditsQuery.isLoading.value">
        <NDataTable
          v-if="coordinateAuditsQuery.data.value?.length"
          :columns="coordinateColumns"
          :data="coordinateAuditsQuery.data.value"
          :bordered="false"
        />
        <NEmpty v-else description="No shelf coordinates yet." class="empty-state" />
      </NSpin>
    </NCard>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useMutation } from "@tanstack/vue-query";
import {
  NAlert,
  NButton,
  NCard,
  NCheckbox,
  NCode,
  NInput,
  NSpace,
  useMessage
} from "naive-ui";

import { createExport } from "../api/catalog";
import type { ExportResponse } from "../types/catalog";

const message = useMessage();
const lastExport = ref<ExportResponse | null>(null);

const form = reactive({
  outputDirectory: "",
  portableJsonl: true,
  postgresDump: true,
  compress: false
});

const includeSomething = computed(() => form.portableJsonl || form.postgresDump);
const createDisabled = computed(() => !includeSomething.value);

const exportMutation = useMutation({
  mutationFn: () => createExport({
    outputDirectory: form.outputDirectory.trim() || null,
    portableJsonl: form.portableJsonl,
    postgresDump: form.postgresDump,
    compress: form.compress
  }),
  onSuccess: (payload) => {
    lastExport.value = payload;
    message.success("Backup created");
  }
});
</script>

<template>
  <section class="stack">
    <header class="page-header">
      <div>
        <p class="page-subtitle">
          Create a timestamped export directory containing portable JSONL data, a PostgreSQL dump, or both.
        </p>
      </div>
    </header>

    <NAlert type="info" title="About directory selection">
      Browsers do not expose a writable absolute folder path to server-side code. Enter the output directory manually,
      or leave it blank to use the default shared <NCode>exports/</NCode> directory beside the repo.
    </NAlert>

    <NAlert v-if="exportMutation.isError.value" type="error" title="Could not create backup">
      {{ exportMutation.error.value instanceof Error ? exportMutation.error.value.message : "Unknown error" }}
    </NAlert>

    <div class="dashboard-grid dashboard-grid-two">
      <NCard title="Create Backup">
        <div class="stack">
          <div class="field">
            <label for="export-output-directory">Output directory</label>
            <NInput
              id="export-output-directory"
              v-model:value="form.outputDirectory"
              placeholder="/path/to/bookshelves-backups"
            />
            <p class="page-subtitle compact-copy">A timestamped child directory will be created beneath this path.</p>
          </div>

          <div class="stack">
            <label class="muted-label">Include</label>
            <label class="inline-actions">
              <NCheckbox v-model:checked="form.portableJsonl" />
              <span>Portable JSONL export</span>
            </label>
            <label class="inline-actions">
              <NCheckbox v-model:checked="form.postgresDump" />
              <span>PostgreSQL custom dump</span>
            </label>
            <label class="inline-actions">
              <NCheckbox v-model:checked="form.compress" :disabled="!form.portableJsonl" />
              <span>Compress JSONL files with gzip</span>
            </label>
          </div>

          <NAlert v-if="!includeSomething" type="warning" title="Nothing selected">
            Choose at least one export type.
          </NAlert>

          <NSpace justify="end">
            <NButton
              type="primary"
              :loading="exportMutation.isPending.value"
              :disabled="createDisabled"
              @click="exportMutation.mutate()"
            >
              Create backup
            </NButton>
          </NSpace>
        </div>
      </NCard>

      <NCard title="What Gets Created">
        <p class="page-subtitle compact-copy">
          Portable exports use one JSON object per line for each table, while the PostgreSQL dump supports faithful
          restoration into another Postgres database.
        </p>
        <ul class="compact-list">
          <li><NCode>manifest.json</NCode> describing the export contents</li>
          <li><NCode>book.jsonl</NCode>, <NCode>collection.jsonl</NCode>, <NCode>shelf.jsonl</NCode>, and <NCode>book_collection.jsonl</NCode></li>
          <li><NCode>bookshelves.dump</NCode> when the PostgreSQL dump option is selected</li>
        </ul>
      </NCard>
    </div>

    <NCard v-if="lastExport" title="Latest Backup">
      <div class="stack">
        <p class="page-subtitle compact-copy">
          Export created at <strong>{{ lastExport.exportedAt }}</strong> using schema version
          <strong>{{ lastExport.schemaVersion }}</strong>.
        </p>
        <div class="field">
          <label>Export directory</label>
          <NInput :value="lastExport.exportDirectory" readonly />
        </div>
        <div class="field">
          <label>Manifest path</label>
          <NInput :value="lastExport.manifestPath" readonly />
        </div>
        <div class="dashboard-grid dashboard-grid-two">
          <NCard title="Row Counts" size="small">
            <ul class="compact-list">
              <li v-for="(count, key) in lastExport.rowCounts" :key="key">{{ key }}: {{ count }}</li>
            </ul>
          </NCard>
          <NCard title="Files" size="small">
            <ul class="compact-list">
              <li v-for="file in lastExport.files" :key="file.name">
                {{ file.name }} ({{ file.format }}, {{ file.sizeBytes }} bytes)
              </li>
            </ul>
          </NCard>
        </div>
      </div>
    </NCard>
  </section>
</template>

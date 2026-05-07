<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { NConfigProvider, NDialogProvider, NMessageProvider, darkTheme } from "naive-ui";

import { fetchAppConfig } from "./api/catalog";

const route = useRoute();
const appName = ref("Bookshelves");
const currentTitle = computed(() => String(route.meta.title ?? appName.value));

onMounted(async () => {
  try {
    const config = await fetchAppConfig();
    appName.value = config.appName || "Bookshelves";
  } catch {
    appName.value = "Bookshelves";
  } finally {
    document.title = appName.value;
  }
});
</script>

<template>
  <NConfigProvider :theme="darkTheme">
    <NMessageProvider>
      <NDialogProvider>
        <div class="shell">
          <aside class="sidebar">
            <div class="brand-lockup">
              <h1>{{ appName }}</h1>
              <p>Catalog for shelves, collections, and books.</p>
            </div>
            <nav class="nav">
              <RouterLink to="/books">Books</RouterLink>
              <RouterLink to="/authors">Authors</RouterLink>
              <RouterLink to="/value">Value</RouterLink>
              <RouterLink to="/reports">Reports</RouterLink>
              <RouterLink to="/missing-location">Missing Location</RouterLink>
              <RouterLink to="/publishers">Publishers</RouterLink>
              <RouterLink to="/collections">Collections</RouterLink>
              <RouterLink to="/shelves">Shelves</RouterLink>
              <RouterLink to="/backup">Backup</RouterLink>
            </nav>
          </aside>
          <main class="content">
            <div class="content-header">
              <div>
                <h2 class="content-title">{{ currentTitle }}</h2>
              </div>
            </div>
            <RouterView />
          </main>
        </div>
      </NDialogProvider>
    </NMessageProvider>
  </NConfigProvider>
</template>

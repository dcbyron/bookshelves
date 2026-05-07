import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import { createRouter, createWebHistory } from "vue-router";
import { QueryClient, VueQueryPlugin } from "@tanstack/vue-query";
import { defineComponent } from "vue";
import { NConfigProvider, NMessageProvider } from "naive-ui";

import AuthorsView from "./AuthorsView.vue";

vi.mock("../api/catalog", () => ({
  fetchAuthors: vi.fn().mockResolvedValue([
    { key: "zeta", name: "Zeta Writer", bookCount: 5 },
    { key: "ada", name: "Ada Author", bookCount: 1 },
    { key: "bert", name: "Bert Bookman", bookCount: 3 }
  ]),
  fetchBooksByAuthor: vi.fn().mockResolvedValue([
    { id: "book-2", title: "Bravo", year: 2003, shelfId: "shelf-b", dateAdded: "2026-03-20" },
    { id: "book-3", title: "Charlie", year: 1999, shelfId: "shelf-a", dateAdded: "2026-03-18" },
    { id: "book-1", title: "Alpha", year: 2001, shelfId: "attic", dateAdded: "2026-03-22" }
  ])
}));

async function mountAuthorsView() {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: "/authors", component: AuthorsView },
      { path: "/books/:id", component: { template: "<div>detail</div>" } }
    ]
  });

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false
      }
    }
  });

  await router.push("/authors");
  await router.isReady();

  const Root = defineComponent({
    components: { AuthorsView, NConfigProvider, NMessageProvider },
    template: `
      <n-config-provider>
        <n-message-provider>
          <AuthorsView />
        </n-message-provider>
      </n-config-provider>
    `
  });

  return mount(Root, {
    global: {
      plugins: [router, [VueQueryPlugin, { queryClient }]]
    }
  });
}

describe("AuthorsView", () => {
  it("can sort authors by name or book count", async () => {
    const wrapper = await mountAuthorsView();

    await flushPromises();
    const text = () => wrapper.text();

    expect(text().indexOf("Ada Author")).toBeLessThan(text().indexOf("Bert Bookman"));
    expect(text().indexOf("Bert Bookman")).toBeLessThan(text().indexOf("Zeta Writer"));

    await wrapper.get("#author-sort").setValue("count");
    await flushPromises();

    expect(text().indexOf("Zeta Writer")).toBeLessThan(text().indexOf("Bert Bookman"));
    expect(text().indexOf("Bert Bookman")).toBeLessThan(text().indexOf("Ada Author"));
  });

  it("can sort selected author books by any visible column", async () => {
    const wrapper = await mountAuthorsView();

    await flushPromises();

    expect(wrapper.text().indexOf("Alpha")).toBeLessThan(wrapper.text().indexOf("Bravo"));
    expect(wrapper.text().indexOf("Bravo")).toBeLessThan(wrapper.text().indexOf("Charlie"));

    await wrapper.get("#author-books-sort").setValue("year");
    await flushPromises();
    expect(wrapper.text().indexOf("Charlie")).toBeLessThan(wrapper.text().indexOf("Alpha"));

    await wrapper.get("#author-books-sort").setValue("shelf");
    await flushPromises();
    expect(wrapper.text().indexOf("Alpha")).toBeLessThan(wrapper.text().indexOf("Bravo"));

    await wrapper.get("#author-books-sort").setValue("added");
    await flushPromises();
    expect(wrapper.text().indexOf("Charlie")).toBeLessThan(wrapper.text().indexOf("Bravo"));
  });
});

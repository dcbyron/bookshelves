import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import { createRouter, createWebHistory } from "vue-router";
import { QueryClient, VueQueryPlugin } from "@tanstack/vue-query";
import { defineComponent } from "vue";
import { NConfigProvider, NMessageProvider } from "naive-ui";
import BooksView from "./BooksView.vue";
import { fetchBooks } from "../api/catalog";

vi.mock("../api/catalog", () => ({
  fetchBooks: vi.fn().mockResolvedValue({
    items: [
      {
        id: "book-1",
        title: "The Name of the Rose",
        author: "Umberto Eco",
        year: 1980,
        shelfId: "shelf-a",
        dateAdded: "2026-03-21",
        updatedAt: "2026-03-21T20:15:00Z"
      }
    ],
    page: 0,
    size: 25,
    totalItems: 1,
    totalPages: 1
  }),
  fetchCollections: vi.fn().mockResolvedValue([
    { id: "general", name: "General" }
  ]),
  fetchShelves: vi.fn().mockResolvedValue([
    { id: "shelf-a", name: "General shelf" }
  ])
}));

async function mountBooksView(path = "/books") {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: "/books", component: BooksView },
      { path: "/books/new", component: { template: "<div>new</div>" } },
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

  await router.push(path);
  await router.isReady();

  const Root = defineComponent({
    components: { BooksView, NConfigProvider, NMessageProvider },
    template: `
      <n-config-provider>
        <n-message-provider>
          <BooksView />
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

describe("BooksView", () => {
  it("renders fetched book data", async () => {
    const wrapper = await mountBooksView();

    await flushPromises();

    expect(wrapper.text()).toContain("The Name of the Rose");
    expect(wrapper.text()).toContain("Umberto Eco");
    expect(wrapper.text()).toContain("Current Focus");
    expect(wrapper.text()).toContain("Showing 1-1 of 1 books");
    expect(wrapper.text()).toContain("Last Audited");
    expect(wrapper.text()).toContain("Audit Date");
    expect(wrapper.text()).toContain("Digital");
    expect(wrapper.text()).toContain("No Collection");
    expect(wrapper.text()).toContain("No Best Estimate");
    expect(wrapper.text()).toContain("Collection Match");
  });

  it("passes multi-collection route filters through to the books query", async () => {
    await mountBooksView("/books?collectionIds=general,secondary&collectionMode=and");

    await flushPromises();

    expect(fetchBooks).toHaveBeenCalledWith(
      expect.objectContaining({
        collectionIds: ["general", "secondary"],
        collectionMode: "and"
      })
    );
  });

  it("passes no-best-estimate route filters through to the books query", async () => {
    await mountBooksView("/books?noBestEstimateOnly=true");

    await flushPromises();

    expect(fetchBooks).toHaveBeenCalledWith(
      expect.objectContaining({
        noBestEstimateOnly: true
      })
    );
  });
});

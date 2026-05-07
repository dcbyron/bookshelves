import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import { createRouter, createWebHistory } from "vue-router";
import { QueryClient, VueQueryPlugin } from "@tanstack/vue-query";
import { defineComponent } from "vue";
import { NConfigProvider, NMessageProvider } from "naive-ui";

import ValueAnalyticsView from "./ValueAnalyticsView.vue";
import { fetchValueDetails, fetchValueSummary } from "../api/catalog";

vi.mock("../api/catalog", () => ({
  fetchCollections: vi.fn().mockResolvedValue([
    { id: "general", name: "General" }
  ]),
  fetchShelves: vi.fn().mockResolvedValue([
    { id: "shelf-a", name: "General shelf" }
  ]),
  fetchValueSummary: vi.fn().mockResolvedValue({
    basis: "paid",
    totalValue: 42.5,
    includedCount: 2,
    missingValueCount: 1,
    totalScopedCount: 3,
    coveragePercent: 66.67,
    missingReplacementCheckedCount: 1,
    oldestReplacementChecked: "2026-03-18"
  }),
  fetchValueDetails: vi.fn().mockResolvedValue({
    basis: "paid",
    totalItems: 3,
    items: [
      {
        id: "book-1",
        title: "Alpha",
        author: "Anne Author",
        year: 2020,
        shelfId: "shelf-a",
        dateAdded: "2026-03-20",
        updatedAt: "2026-03-21T12:00:00Z",
        valueAmount: 12.5,
        priceToReplaceChecked: "2026-03-18",
        special: false,
        digital: true,
        hardback: false,
        dustjacket: false,
        slipcase: false,
        video: true
      },
      {
        id: "book-2",
        title: "Beta",
        author: "Brian Bibliophile",
        year: 2021,
        shelfId: null,
        dateAdded: "2026-03-21",
        updatedAt: "2026-03-21T12:10:00Z",
        valueAmount: null,
        priceToReplaceChecked: null,
        special: true,
        digital: false,
        hardback: true,
        dustjacket: true,
        slipcase: false,
        video: false
      }
    ]
  })
}));

async function mountValueView() {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: "/value", component: ValueAnalyticsView },
      { path: "/books/:id", component: { template: "<div>book detail</div>" } }
    ]
  });

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false }
    }
  });

  await router.push("/value");
  await router.isReady();

  const Root = defineComponent({
    components: { ValueAnalyticsView, NConfigProvider, NMessageProvider },
    template: `
      <n-config-provider>
        <n-message-provider>
          <ValueAnalyticsView />
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

describe("ValueAnalyticsView", () => {
  it("renders summary cards and scoped book details", async () => {
    const wrapper = await mountValueView();

    await flushPromises();

    expect(wrapper.text()).toContain("Total Value");
    expect(wrapper.text()).toContain("$42.50");
    expect(wrapper.text()).toContain("Books Included");
    expect(wrapper.text()).toContain("Coverage");
    expect(wrapper.text()).toContain("66.67%");
    expect(wrapper.text()).toContain("Alpha");
    expect(wrapper.text()).toContain("Beta");
    expect(wrapper.text()).toContain("Books with blank value remain visible");
  });

  it("renders date-added scope controls", async () => {
    const wrapper = await mountValueView();

    await flushPromises();

    expect(wrapper.text()).toContain("Date Added");
    expect(wrapper.text()).toContain("best estimate");
    expect(wrapper.text()).toContain("Digital");
    expect(wrapper.text()).toContain("Dustjacket");
    expect(wrapper.text()).toContain("Video");
    wrapper.get("#analytics-dateAddedMode");
    const dateInput = wrapper.get<HTMLInputElement>("#analytics-dateAddedOn");
    expect(dateInput.element.disabled).toBe(true);
    expect(dateInput.attributes("placeholder")).toBe("YYYY-MM-DD");
  });
});

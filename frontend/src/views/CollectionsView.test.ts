import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import { QueryClient, VueQueryPlugin } from "@tanstack/vue-query";
import { defineComponent } from "vue";
import { NConfigProvider, NDialogProvider, NMessageProvider } from "naive-ui";

import CollectionsView from "./CollectionsView.vue";

vi.mock("../api/catalog", () => ({
  fetchCollections: vi.fn().mockResolvedValue([
    { id: "zeta", name: "Zeta", usageCount: 5 },
    { id: "alpha", name: "Alpha", usageCount: 1 },
    { id: "beta", name: "Beta", usageCount: 7 }
  ]),
  createCollection: vi.fn(),
  deleteCollection: vi.fn()
}));

vi.mock("../composables/useConfirmation", () => ({
  useConfirmation: () => ({
    confirm: vi.fn().mockResolvedValue(true)
  })
}));

async function mountCollectionsView() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false
      }
    }
  });

  const Root = defineComponent({
    components: { CollectionsView, NConfigProvider, NMessageProvider, NDialogProvider },
    template: `
      <n-config-provider>
        <n-dialog-provider>
          <n-message-provider>
            <CollectionsView />
          </n-message-provider>
        </n-dialog-provider>
      </n-config-provider>
    `
  });

  return mount(Root, {
    global: {
      plugins: [[VueQueryPlugin, { queryClient }]]
    }
  });
}

describe("CollectionsView", () => {
  it("can sort existing collections by name or count", async () => {
    const wrapper = await mountCollectionsView();
    await flushPromises();

    const text = () => wrapper.text();
    expect(text().indexOf("Alpha")).toBeLessThan(text().indexOf("Beta"));
    expect(text().indexOf("Beta")).toBeLessThan(text().indexOf("Zeta"));

    await wrapper.get("#collection-sort").setValue("count");
    await flushPromises();

    expect(text().indexOf("Beta")).toBeLessThan(text().indexOf("Zeta"));
    expect(text().indexOf("Zeta")).toBeLessThan(text().indexOf("Alpha"));
  });
});

import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import { QueryClient, VueQueryPlugin } from "@tanstack/vue-query";
import { defineComponent } from "vue";
import { NConfigProvider, NDialogProvider, NMessageProvider } from "naive-ui";

import PublishersView from "./PublishersView.vue";

const renamePublisher = vi.fn();

vi.mock("../api/catalog", () => ({
  fetchPublishers: vi.fn().mockResolvedValue([
    { publisher: "Zeta Press", bookCount: 5 },
    { publisher: "Alpha House", bookCount: 1 },
    { publisher: "Beta Books", bookCount: 7 }
  ]),
  renamePublisher: vi.fn()
}));

async function mountPublishersView() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false
      }
    }
  });

  const Root = defineComponent({
    components: { PublishersView, NConfigProvider, NMessageProvider, NDialogProvider },
    template: `
      <n-config-provider>
        <n-dialog-provider>
          <n-message-provider>
            <PublishersView />
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

describe("PublishersView", () => {
  it("can sort existing publishers by name or count", async () => {
    const wrapper = await mountPublishersView();
    await flushPromises();

    const text = () => wrapper.text();
    expect(text().indexOf("Alpha House")).toBeLessThan(text().indexOf("Beta Books"));
    expect(text().indexOf("Beta Books")).toBeLessThan(text().indexOf("Zeta Press"));

    await wrapper.get("#publisher-sort").setValue("count");
    await flushPromises();

    expect(text().indexOf("Beta Books")).toBeLessThan(text().indexOf("Zeta Press"));
    expect(text().indexOf("Zeta Press")).toBeLessThan(text().indexOf("Alpha House"));
  });
});

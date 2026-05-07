import { flushPromises, mount } from "@vue/test-utils";
import { defineComponent } from "vue";
import { describe, expect, it } from "vitest";
import { NConfigProvider, NMessageProvider } from "naive-ui";

import BookForm from "./BookForm.vue";
import type { BookRequest } from "../types/catalog";

function makeBook(): BookRequest {
  return {
    title: "",
    author: null,
    publicationCity: null,
    publisher: null,
    imprint: null,
    year: null,
    firstPublished: null,
    edition: null,
    pages: null,
    frontpages: null,
    dateAdded: null,
    lastAudited: null,
    shelfId: null,
    authorSecondary: null,
    isbn: null,
    special: false,
    digital: false,
    hardback: false,
    dustjacket: false,
    slipcase: false,
    video: false,
    vols: 1,
    series: null,
    shelfCoords: null,
    note: null,
    pricePaid: null,
    priceOnItem: null,
    priceToReplace: null,
    priceToReplaceChecked: null,
    collectionIds: []
  };
}

function mountBookForm(initialValue: BookRequest) {
  const Root = defineComponent({
    components: { BookForm, NConfigProvider, NMessageProvider },
    data() {
      return {
        initialValue
      };
    },
    template: `
      <n-config-provider>
        <n-message-provider>
          <BookForm
            :initial-value="initialValue"
            :collections="[
              { id: 'general', name: 'General' },
              { id: 'bestsellers', name: 'Bestsellers' }
            ]"
            :shelves="[{ id: 'shelf-a', name: 'General shelf' }]"
          />
        </n-message-provider>
      </n-config-provider>
    `
  });

  return mount(Root);
}

describe("BookForm", () => {
  it("renders initial book values", async () => {
    const wrapper = mountBookForm({
      ...makeBook(),
      title: "A Book",
      author: "An Author",
      imprint: "An Imprint",
      firstPublished: 1901,
      frontpages: "xii",
      pages: 220,
      pricePaid: 130,
      lastAudited: "2026-03-26",
      digital: true,
      dustjacket: true,
      collectionIds: ["general"]
    });

    await flushPromises();

    expect(wrapper.text()).toContain("Bibliographic Details");
    expect((wrapper.get("#title input").element as HTMLInputElement).value).toBe("A Book");
    expect((wrapper.get("#author input").element as HTMLInputElement).value).toBe("An Author");
    expect((wrapper.get("#imprint input").element as HTMLInputElement).value).toBe("An Imprint");
    expect((wrapper.get("#firstPublished input").element as HTMLInputElement).value).toBe("1901");
    expect((wrapper.get("#frontpages input").element as HTMLInputElement).value).toBe("xii");
    expect((wrapper.get("#pages input").element as HTMLInputElement).value).toBe("220");
    expect((wrapper.get("#pricePaid input").element as HTMLInputElement).value).toBe("130.00");
    expect((wrapper.get("#lastAudited input").element as HTMLInputElement).value).toBe("2026-03-26");
    expect(wrapper.get("#digital").attributes("aria-checked")).toBe("true");
    expect(wrapper.get("#dustjacket").attributes("aria-checked")).toBe("true");
    expect(wrapper.text()).toContain("General");
  });

  it("renders available collection choices for assignment", async () => {
    const wrapper = mountBookForm(makeBook());

    await flushPromises();

    expect(wrapper.text()).toContain("Collections");
    expect(wrapper.text()).toContain("General");
    expect(wrapper.text()).toContain("Bestsellers");
    expect(wrapper.text()).toContain("Save book");
  });

  it("rejects isbn values longer than 13 characters", async () => {
    const wrapper = mountBookForm({
      ...makeBook(),
      title: "A Book"
    });
    const form = wrapper.findComponent(BookForm);

    await flushPromises();

    await wrapper.get("#isbn input").setValue("12345678901234");
    await wrapper.get("button").trigger("click");
    await flushPromises();

    expect(form.emitted("submit")).toBeUndefined();
  });
});

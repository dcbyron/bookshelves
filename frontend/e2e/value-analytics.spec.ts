import { expect, test } from "@playwright/test";

test("value analytics summarizes paid values and exposes missing coverage", async ({ page, request }) => {
  const stamp = Date.now();

  const create = async (payload: Record<string, unknown>) => {
    const response = await request.post("/api/books", { data: payload });
    expect(response.ok()).toBeTruthy();
  };

  await create({
    title: `TEST DATA--Value Alpha ${stamp}`,
    author: "Anne Author",
    publisher: "Test Publisher",
    year: 2020,
    edition: "1",
    pages: 250,
    frontpages: "xii",
    dateAdded: "2026-03-20",
    shelfId: "shelf-a",
    authorSecondary: null,
    isbn: null,
    special: false,
    hardback: false,
    vols: 1,
    series: null,
    shelfCoords: null,
    note: null,
    pricePaid: 12.5,
    priceOnItem: 15,
    priceToReplace: 18,
    priceToReplaceChecked: "2026-03-18",
    collectionIds: ["general"]
  });

  await create({
    title: `TEST DATA--Value Beta ${stamp}`,
    author: "Brian Bibliophile",
    publisher: "Test Publisher",
    year: 2021,
    edition: "1",
    pages: 275,
    frontpages: "xii",
    dateAdded: "2026-03-21",
    shelfId: null,
    authorSecondary: null,
    isbn: null,
    special: true,
    hardback: true,
    vols: 1,
    series: null,
    shelfCoords: null,
    note: null,
    pricePaid: null,
    priceOnItem: 28,
    priceToReplace: 30,
    priceToReplaceChecked: null,
    collectionIds: ["general"]
  });

  await page.goto("/value");

  await expect(page.getByText("Valuation Scope")).toBeVisible();
  await expect(page.locator(".n-card").filter({ hasText: "Total Value" }).getByText("$12.50")).toBeVisible();
  await expect(page.getByText("TEST DATA--Value Alpha")).toBeVisible();
  await expect(page.getByText("TEST DATA--Value Beta")).toBeVisible();
  await expect(page.getByText("Books with blank value remain visible")).toBeVisible();
});

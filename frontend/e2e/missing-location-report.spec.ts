import { expect, test } from "@playwright/test";

test("missing-location report can assign a shelf inline and keeps the row until refresh", async ({ page }) => {
  const stamp = Date.now();
  const title = `TEST DATA--Missing Location Book ${stamp}`;
  const author = `TEST DATA--Missing Location Author ${stamp}`;
  const coords = "A4";
  const createResponse = await page.request.post("http://localhost:18080/api/books", {
    data: { title, author, collectionIds: [] }
  });
  expect(createResponse.ok()).toBeTruthy();
  const createdBook = await createResponse.json();

  await page.goto("/missing-location");
  await expect(page.getByText("Books Without a Shelf")).toBeVisible();

  const row = page.locator(".report-book-row").filter({ has: page.getByText(title, { exact: true }) });
  await expect(row).toBeVisible();

  await row.locator(".n-base-selection").click();
  await page.keyboard.press("ArrowDown");
  await page.keyboard.press("Enter");
  await row.locator(".report-coords-input").fill(coords);
  await row.getByRole("button", { name: "Apply", exact: true }).click();

  await expect(row.getByText(/Assigned to /)).toBeVisible();
  await expect(row.getByRole("button", { name: "Apply", exact: true })).toBeDisabled();
  await expect(row).toContainText(title);

  await page.getByRole("button", { name: "Refresh", exact: true }).click();
  await expect(page.getByText(title, { exact: true })).toHaveCount(0);
  await expect
    .poll(async () => {
      const bookResponse = await page.request.get(`http://localhost:18080/api/books/${createdBook.id}`);
      if (!bookResponse.ok()) {
        return null;
      }
      const bookPayload = await bookResponse.json();
      const reportResponse = await page.request.get("http://localhost:18080/api/reports/missing-location");
      if (!reportResponse.ok()) {
        return null;
      }
      const payload = await reportResponse.json();
      return {
        stillInReport: payload.books.some((book: { title: string }) => book.title === title),
        shelfCoords: bookPayload.shelfCoords
      };
    })
    .toEqual({ stillInReport: false, shelfCoords: coords });
});

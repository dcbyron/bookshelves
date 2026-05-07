import { expect, test } from "@playwright/test";

test("book can be updated and deleted, and in-use collection deletion is blocked", async ({ page }) => {
  const stamp = Date.now();
  const initialTitle = `TEST DATA--Playwright Book ${stamp}`;
  const updatedTitle = `TEST DATA--Playwright Book Updated ${stamp}`;
  const titleInput = page.getByPlaceholder("Book title");

  const createResponse = await page.request.post("http://localhost:18080/api/books", {
    data: { title: initialTitle, collectionIds: ["general"] }
  });
  expect(createResponse.ok()).toBeTruthy();
  const createdBook = await createResponse.json();

  await page.goto(`/books/${createdBook.id}`);
  await expect(titleInput).toHaveValue(initialTitle);

  await titleInput.fill(updatedTitle);
  await page.getByRole("button", { name: "Save book", exact: true }).click();

  await expect(page.getByText("Book saved")).toBeVisible();
  await expect(titleInput).toHaveValue(updatedTitle);

  await page.goto("/collections");
  const generalRow = page
    .locator("tr")
    .filter({ has: page.locator("td", { hasText: /^General$/ }) })
    .filter({ has: page.locator("td", { hasText: /^general$/ }) });
  await expect(generalRow).toBeVisible();
  await generalRow.getByRole("button", { name: "Delete", exact: true }).click();
  await page.getByRole("button", { name: "Attempt delete", exact: true }).click();
  await expect(page.getByText("cannot be deleted")).toBeVisible();

  await page.getByRole("link", { name: "Books", exact: true }).click();
  await page.getByPlaceholder("Title, author, or series").fill(updatedTitle);
  await page.getByRole("link", { name: updatedTitle, exact: true }).click();

  await page.getByRole("button", { name: "Delete", exact: true }).click();
  await page.getByRole("button", { name: "Delete book", exact: true }).click();

  await expect(page.getByText("Book deleted")).toBeVisible();
});

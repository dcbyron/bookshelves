import { expect, test } from "@playwright/test";

test("main catalog routes are reachable", async ({ page }) => {
  await page.goto("/books");
  await expect(page.getByRole("heading", { name: "Bookshelves", exact: true })).toBeVisible();
  await expect(page.getByRole("link", { name: "Books", exact: true })).toBeVisible();

  await page.getByRole("link", { name: "Missing Location", exact: true }).click();
  await expect(page.getByText("Assign shelves to books that still lack a location")).toBeVisible();

  await page.getByRole("link", { name: "Publishers", exact: true }).click();
  await expect(page.getByText("Normalize publisher names across existing books")).toBeVisible();

  await page.getByRole("link", { name: "Collections", exact: true }).click();
  await expect(page.getByText("Manage the named collection buckets used across the catalog.")).toBeVisible();

  await page.getByRole("link", { name: "Shelves", exact: true }).click();
  await expect(page.getByText("Track the physical placement vocabulary used by the library.")).toBeVisible();
});

import { expect, test } from "@playwright/test";

test("books view supports multi-collection and no-collection filtering", async ({ page }) => {
  const stamp = Date.now();
  const generalTitle = `TEST DATA--General Only ${stamp}`;
  const secondaryTitle = `TEST DATA--Secondary Only ${stamp}`;
  const bothTitle = `TEST DATA--Both Collections ${stamp}`;
  const noCollectionTitle = `TEST DATA--No Collection ${stamp}`;

  const createBook = async (title: string, collectionIds: string[]) => {
    const response = await page.request.post("http://localhost:18080/api/books", {
      data: { title, collectionIds }
    });
    expect(response.ok()).toBeTruthy();
    return response.json();
  };

  await createBook(generalTitle, ["general"]);
  await createBook(secondaryTitle, ["secondary"]);
  await createBook(bothTitle, ["general", "secondary"]);
  await createBook(noCollectionTitle, []);

  await page.goto("/books");

  await page.getByRole("button", { name: "General", exact: true }).click();
  await expect(page.getByText(generalTitle)).toBeVisible();
  await expect(page.getByText(bothTitle)).toBeVisible();
  await expect(page.getByText(secondaryTitle)).not.toBeVisible();
  await expect(page.getByText(noCollectionTitle)).not.toBeVisible();

  await page.getByRole("button", { name: "Secondary", exact: true }).click();
  await expect(page.getByText(generalTitle)).toBeVisible();
  await expect(page.getByText(secondaryTitle)).toBeVisible();
  await expect(page.getByText(bothTitle)).toBeVisible();

  await page.getByRole("button", { name: "AND", exact: true }).click();
  await expect(page.getByText(bothTitle)).toBeVisible();
  await expect(page.getByText(generalTitle)).not.toBeVisible();
  await expect(page.getByText(secondaryTitle)).not.toBeVisible();

  await page.getByRole("button", { name: "No Collection", exact: true }).click();
  await expect(page.getByText(noCollectionTitle)).toBeVisible();
  await expect(page.getByText(generalTitle)).not.toBeVisible();
  await expect(page.getByText(secondaryTitle)).not.toBeVisible();
  await expect(page.getByText(bothTitle)).not.toBeVisible();
});

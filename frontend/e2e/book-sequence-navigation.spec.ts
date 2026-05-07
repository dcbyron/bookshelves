import { expect, test } from "@playwright/test";

test("books view can navigate through the full filtered result set from edit mode", async ({ page }) => {
  const stamp = Date.now();
  const prefix = `TEST DATA--Sequence ${stamp}`;
  const titles = [`${prefix} Alpha`, `${prefix} Beta`, `${prefix} Gamma`];

  for (const title of titles) {
    const response = await page.request.post("http://localhost:18080/api/books", {
      data: { title, collectionIds: ["general"] }
    });
    expect(response.ok()).toBeTruthy();
  }

  await page.goto("/books");
  await page.getByPlaceholder("Title, author, or series").fill(prefix);
  await expect(page.getByText("Showing 1-3 of 3 books")).toBeVisible();

  await page.getByRole("link", { name: titles[0], exact: true }).click();
  await expect(page.getByPlaceholder("Book title")).toHaveValue(titles[0]);
  await expect(page.getByText("1 of 3")).toBeVisible();
  await expect(page.getByRole("button", { name: "Previous", exact: true })).toBeDisabled();

  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByPlaceholder("Book title")).toHaveValue(titles[1]);
  await expect(page.getByText("2 of 3")).toBeVisible();

  await page.getByPlaceholder("Book title").fill(`${titles[1]} Revised`);
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByText("Discard unsaved changes?")).toBeVisible();
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  await expect(page.getByPlaceholder("Book title")).toHaveValue(`${titles[1]} Revised`);
  await expect(page.getByText("2 of 3")).toBeVisible();

  await page.getByRole("button", { name: "Next", exact: true }).click();
  await page.getByRole("button", { name: "Discard changes", exact: true }).click();
  await expect(page.getByPlaceholder("Book title")).toHaveValue(titles[2]);
  await expect(page.getByText("3 of 3")).toBeVisible();
  await expect(page.getByRole("button", { name: "Next", exact: true })).toBeDisabled();

  const revisedThirdTitle = `${titles[2]} Revised`;
  await page.getByPlaceholder("Book title").fill(revisedThirdTitle);
  await page.getByRole("button", { name: "Save book", exact: true }).click();
  await expect(page.getByText("Book saved")).toBeVisible();

  await page.getByRole("button", { name: "Previous", exact: true }).click();
  await expect(page.getByPlaceholder("Book title")).toHaveValue(titles[1]);
  await expect(page.getByText("2 of 3")).toBeVisible();
  await expect(page.getByText("Book saved")).toBeHidden();

  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByPlaceholder("Book title")).toHaveValue(revisedThirdTitle);
  await expect(page.getByText("3 of 3")).toBeVisible();
  await expect(page.getByText("Book saved")).toBeHidden();

  await page.getByRole("link", { name: "Back to books", exact: true }).click();
  await expect(page.getByPlaceholder("Title, author, or series")).toHaveValue(prefix);
  await expect(page.getByText("Showing 1-3 of 3 books")).toBeVisible();
});

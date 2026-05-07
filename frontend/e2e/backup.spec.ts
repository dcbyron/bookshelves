import { expect, test } from "@playwright/test";
import { existsSync, mkdtempSync, readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";

test("backup screen creates export artifacts", async ({ page }) => {
  const outputBase = mkdtempSync(join(tmpdir(), "bookshelves-backup-ui-"));

  await page.goto("/backup");
  await page.getByPlaceholder("/path/to/bookshelves-backups").fill(outputBase);
  await page.getByRole("button", { name: "Create backup", exact: true }).click();

  await expect(page.getByText("Backup created")).toBeVisible();
  await expect(page.getByText("Latest Backup", { exact: true })).toBeVisible();

  await expect.poll(() => readdirSync(outputBase).length).toBe(1);

  const exportDir = join(outputBase, readdirSync(outputBase)[0]);
  const manifestPath = join(exportDir, "manifest.json");
  const bookJsonlPath = join(exportDir, "book.jsonl");
  const dumpPath = join(exportDir, "bookshelves.dump");

  expect(existsSync(manifestPath)).toBeTruthy();
  expect(existsSync(bookJsonlPath)).toBeTruthy();
  expect(existsSync(dumpPath)).toBeTruthy();

  const manifest = JSON.parse(readFileSync(manifestPath, "utf8"));
  expect(manifest.includes).toEqual({
    portableJsonl: true,
    postgresDump: true
  });
  expect(manifest.files.map((file: { name: string }) => file.name)).toEqual(
    expect.arrayContaining(["book.jsonl", "collection.jsonl", "shelf.jsonl", "book_collection.jsonl", "bookshelves.dump"])
  );
});

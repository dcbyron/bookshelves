import { formatCurrency, formatDate, formatDateTime } from "./format";

describe("format helpers", () => {
  it("formats dates into a readable short form", () => {
    expect(formatDate("2026-03-21")).toContain("2026");
  });

  it("formats timestamps into a readable date-time", () => {
    expect(formatDateTime("2026-03-21T20:15:00Z")).toContain("2026");
  });

  it("formats money as USD", () => {
    expect(formatCurrency(12.5)).toBe("$12.50");
    expect(formatCurrency(130)).toBe("$130.00");
  });

  it("returns em dash when value is missing", () => {
    expect(formatDate(null)).toBe("—");
    expect(formatCurrency(null)).toBe("—");
  });
});

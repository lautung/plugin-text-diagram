import { describe, expect, it } from "vitest";
import { detectTheme } from "./theme";

describe("detectTheme", () => {
  it("detects a configured dark selector", () => {
    document.documentElement.className = "dark";

    expect(detectTheme("html[class~=dark]")).toBe("dark");
  });

  it("falls back to light for an invalid selector", () => {
    expect(detectTheme("[invalid")).toBe("light");
  });
});

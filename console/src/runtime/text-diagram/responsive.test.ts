import { describe, expect, it } from "vitest";
import { applyMobileLayout } from "./responsive";

describe("applyMobileLayout", () => {
  it("uses the configured mode below 768px", () => {
    const card = document.createElement("section");

    applyMobileLayout(card, "scroll", 767);

    expect(card.dataset.textDiagramMobileLayout).toBe("scroll");
  });

  it("disables the mobile strategy at 768px", () => {
    const card = document.createElement("section");

    applyMobileLayout(card, "thumbnail", 768);

    expect(card.dataset.textDiagramMobileLayout).toBe("desktop");
  });
});

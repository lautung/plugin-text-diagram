import { describe, expect, it, vi } from "vitest";
import { createDiagramCard } from "./card";

describe("createDiagramCard", () => {
  it("switches views, copies source, and exits fullscreen", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", { configurable: true, value: { writeText } });
    const host = document.createElement("text-diagram");
    document.body.append(host);
    const card = createDiagramCard(host, "mermaid", "graph TD;A-->B");

    card.querySelector<HTMLButtonElement>("[data-action='source']")!.click();
    expect(card.dataset.textDiagramView).toBe("source");
    card.querySelector<HTMLButtonElement>("[data-action='copy']")!.click();
    await vi.waitFor(() => expect(writeText).toHaveBeenCalledWith("graph TD;A-->B"));
    card.querySelector<HTMLButtonElement>("[data-action='fullscreen']")!.click();
    document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }));
    expect(card.dataset.textDiagramFullscreen).toBe("false");
  });
});

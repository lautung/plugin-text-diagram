import { expect, it, vi } from "vitest";
import { createMermaidAdapter } from "./mermaid-adapter";

it("renders Mermaid with safe labels disabled", async () => {
  const initialize = vi.fn();
  const run = vi.fn(async ({ nodes }: { nodes: HTMLElement[] }) => {
    expect(nodes[0].isConnected).toBe(true);
    nodes[0].innerHTML = "<svg></svg>";
  });
  const adapter = createMermaidAdapter(async () => ({ initialize, run }));

  const result = await adapter.render(document.createElement("div"), "graph TD;A-->B", "light");

  expect(initialize).toHaveBeenCalledWith(expect.objectContaining({ startOnLoad: false, htmlLabels: false }));
  expect(result.element.tagName.toLowerCase()).toBe("svg");
});

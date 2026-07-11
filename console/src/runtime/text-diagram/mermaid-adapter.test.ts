import { expect, it, vi } from "vitest";
import { createMermaidAdapter } from "./mermaid-adapter";

it("renders Mermaid with safe labels disabled", async () => {
  const initialize = vi.fn();
  const run = vi.fn(async ({ nodes }: { nodes: HTMLElement[] }) => {
    expect(nodes[0].isConnected).toBe(true);
    nodes[0].innerHTML = "<svg></svg>";
  });
  const exporter = vi.fn(async () => ({
    element: document.createElement("img"),
    filename: "text-diagram.png",
    mimeType: "image/png",
  }));
  const adapter = createMermaidAdapter(async () => ({ initialize, run }), "png", exporter);

  const result = await adapter.render(document.createElement("div"), "graph TD;A-->B", "light");

  expect(initialize).toHaveBeenCalledWith(expect.objectContaining({ startOnLoad: false, htmlLabels: false }));
  expect(exporter).toHaveBeenCalledWith(expect.any(SVGElement), "png", "text-diagram");
  expect(result.filename).toBe("text-diagram.png");
});

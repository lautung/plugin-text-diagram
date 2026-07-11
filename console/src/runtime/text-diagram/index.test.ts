import { beforeEach, expect, it, vi } from "vitest";
import { createRuntime } from "./index";
import type { DiagramAdapter, RuntimeConfig } from "./types";

const config: RuntimeConfig = {
  darkClassSelector: "html.dark",
  mermaidSelector: "text-diagram[data-type=mermaid]",
  mobileLayoutMode: "scroll",
};

beforeEach(() => {
  document.documentElement.className = "";
  document.body.innerHTML = "";
});

it("renders PJAX nodes without wrapping old nodes twice", async () => {
  document.body.innerHTML = `<text-diagram data-type="mermaid" data-content="graph TD;A-->B"></text-diagram>`;
  const render = vi.fn(async () => ({ element: document.createElement("svg"), filename: "x.svg", mimeType: "image/svg+xml" }));
  const adapters = { mermaid: { render }, plantuml: { render } } as Record<"mermaid" | "plantuml", DiagramAdapter>;
  const runtime = createRuntime(config, adapters);
  await runtime.scan();
  const firstCard = document.querySelector("[data-text-diagram-card]");

  document.dispatchEvent(new Event("pjax:success"));
  await runtime.whenIdle();

  expect(document.querySelectorAll("[data-text-diagram-card]")).toHaveLength(1);
  expect(document.querySelector("[data-text-diagram-card]")).toBe(firstCard);
  expect(render).toHaveBeenCalledTimes(1);
  runtime.destroy();
});

it("isolates a failed diagram from the next diagram", async () => {
  document.body.innerHTML = `<text-diagram data-type="mermaid" data-content="bad"></text-diagram><text-diagram data-type="plantuml" data-content="ok" data-src="x.png"></text-diagram>`;
  const mermaid = { render: vi.fn().mockRejectedValue(new Error("bad diagram")) };
  const plantuml = { render: vi.fn(async () => ({ element: document.createElement("img"), filename: "x.png", mimeType: "image/png" })) };
  const runtime = createRuntime(config, { mermaid, plantuml });

  await runtime.scan();

  expect(document.querySelectorAll("[data-text-diagram-state='error']")).toHaveLength(1);
  expect(document.querySelectorAll("[data-text-diagram-state='ready']")).toHaveLength(1);
  runtime.destroy();
});

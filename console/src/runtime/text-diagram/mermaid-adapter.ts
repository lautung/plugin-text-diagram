import { exportSvg, type SvgInput } from "./image-exporter";
import type { DiagramAdapter, DiagramOutputFormat, DiagramTheme, RenderResult } from "./types";

const MERMAID_SRC = "/plugins/text-diagram/assets/static/mermaid.min.js";

export interface MermaidRuntime {
  initialize(config: Record<string, unknown>): void;
  run(options: { nodes: HTMLElement[] }): Promise<void>;
}

export type SvgExporter = (
  svg: SvgInput,
  format: DiagramOutputFormat,
  filenamePrefix?: string,
) => Promise<RenderResult>;

declare global {
  interface Window {
    mermaid?: MermaidRuntime;
  }
}

let sharedLoader: Promise<MermaidRuntime> | undefined;

export function loadMermaid(): Promise<MermaidRuntime> {
  if (window.mermaid) return Promise.resolve(window.mermaid);
  if (sharedLoader) return sharedLoader;
  sharedLoader = new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>("script[data-halo-text-diagram-mermaid]");
    const script = existing ?? document.createElement("script");
    script.dataset.haloTextDiagramMermaid = "";
    script.src ||= MERMAID_SRC;
    script.async = true;
    const timeout = window.setTimeout(() => fail(new Error("Mermaid 加载超时")), 10000);
    const cleanup = () => {
      window.clearTimeout(timeout);
      script.removeEventListener("load", loaded);
      script.removeEventListener("error", failed);
    };
    const fail = (error: Error) => {
      cleanup();
      sharedLoader = undefined;
      reject(error);
    };
    const loaded = () => window.mermaid ? (cleanup(), resolve(window.mermaid)) : fail(new Error("Mermaid API 不可用"));
    const failed = () => fail(new Error("Mermaid 资源加载失败"));
    script.addEventListener("load", loaded);
    script.addEventListener("error", failed);
    if (!existing) document.head.append(script);
  });
  return sharedLoader;
}

export function createMermaidAdapter(
  loader: () => Promise<MermaidRuntime> = loadMermaid,
  outputFormat: DiagramOutputFormat = "svg",
  svgExporter: SvgExporter = exportSvg,
): DiagramAdapter {
  return {
    async render(_target: HTMLElement, source: string, theme: DiagramTheme): Promise<RenderResult> {
      const mermaid = await loader();
      mermaid.initialize({
        startOnLoad: false,
        htmlLabels: false,
        theme: theme === "dark" ? "dark" : "default",
        flowchart: { htmlLabels: false, useMaxWidth: false },
      });
      if (document.fonts?.ready) await document.fonts.ready;
      const renderTarget = document.createElement("div");
      renderTarget.textContent = source;
      renderTarget.hidden = true;
      document.body.append(renderTarget);
      try {
        await mermaid.run({ nodes: [renderTarget] });
        const svg = renderTarget.querySelector("svg");
        if (!svg) throw new Error("Mermaid 未生成 SVG");
        svg.remove();
        return svgExporter(svg, outputFormat, "text-diagram");
      } finally {
        renderTarget.remove();
      }
    },
  };
}

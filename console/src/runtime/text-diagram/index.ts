import "./style.css";
import { createDiagramCard, type DiagramCard } from "./card";
import { createMermaidAdapter } from "./mermaid-adapter";
import { createPlantUmlAdapter } from "./plantuml-adapter";
import { applyMobileLayout } from "./responsive";
import { detectTheme, observeTheme } from "./theme";
import type {
  DiagramAdapter,
  DiagramOutputFormat,
  DiagramTheme,
  DiagramType,
  MobileLayoutMode,
  RuntimeConfig,
} from "./types";

const DEFAULT_CONFIG: RuntimeConfig = {
  darkClassSelector: "html[class~=dark]",
  mermaidSelector: "text-diagram[data-type=mermaid]",
  mobileLayoutMode: "scroll",
  mermaidOutputFormat: "svg",
  plantumlOutputFormat: "svg",
};

interface RenderRecord {
  card: DiagramCard;
  host: HTMLElement;
  source: string;
  type: DiagramType;
  objectUrl?: string;
}

export interface TextDiagramRuntime {
  scan(): Promise<void>;
  whenIdle(): Promise<void>;
  destroy(): void;
}

function isDiagramType(value: string | undefined): value is DiagramType {
  return value === "mermaid" || value === "plantuml";
}

function isOutputFormat(value: unknown): value is DiagramOutputFormat {
  return value === "svg" || value === "png" || value === "webp";
}

function errorMessage(error: unknown): string {
  if (error instanceof Error) return error.message;
  if (typeof error === "object" && error !== null) {
    const message = (error as { message?: unknown }).message;
    if (typeof message === "string") return message;
    const description = (error as { str?: unknown }).str;
    if (typeof description === "string") return description;
  }
  return String(error);
}

function createObjectUrl(element: HTMLElement | SVGElement, mimeType: string): string | undefined {
  if (typeof URL.createObjectURL !== "function") return undefined;
  const content = element instanceof SVGElement ? new XMLSerializer().serializeToString(element) : element.outerHTML;
  return URL.createObjectURL(new Blob([content], { type: mimeType }));
}

export function createRuntime(
  config: RuntimeConfig,
  adapters: Record<DiagramType, DiagramAdapter>,
): TextDiagramRuntime {
  const records = new Map<HTMLElement, RenderRecord>();
  let renderChain = Promise.resolve();
  let theme: DiagramTheme = detectTheme(config.darkClassSelector);

  const renderRecord = async (record: RenderRecord) => {
    record.card.setTheme(theme);
    applyMobileLayout(record.card, config.mobileLayoutMode);
    try {
      const result = await adapters[record.type].render(record.host, record.source, theme);
      record.card.setPreview(result.element);
      if (record.objectUrl) URL.revokeObjectURL(record.objectUrl);
      record.objectUrl = result.downloadUrl ?? createObjectUrl(result.element, result.mimeType);
      if (record.objectUrl) record.card.setDownload(record.objectUrl, result.filename);
      record.host.dataset.textDiagramState = "ready";
    } catch (error) {
      record.card.setError(errorMessage(error));
      record.host.dataset.textDiagramState = "error";
      console.error("Text Diagram rendering failed", error);
    }
  };

  const scan = () => {
    renderChain = renderChain.then(async () => {
      let nodes: HTMLElement[] = [];
      try {
        nodes = Array.from(document.querySelectorAll<HTMLElement>("text-diagram[data-type]"));
        if (config.mermaidSelector) {
          nodes.push(...Array.from(document.querySelectorAll<HTMLElement>(config.mermaidSelector)));
        }
      } catch (error) {
        console.error("Text Diagram selector is invalid", error);
      }
      for (const host of Array.from(new Set(nodes))) {
        if (host.dataset.textDiagramState || host.dataset.textDiagramCard !== undefined) continue;
        if (!isDiagramType(host.dataset.type)) continue;
        const type = host.dataset.type;
        const source = host.dataset.content ?? host.textContent ?? "";
        host.dataset.textDiagramState = "loading";
        const card = createDiagramCard(host, type, source);
        const record = { card, host, source, type };
        records.set(host, record);
        await renderRecord(record);
      }
    });
    return renderChain;
  };

  const onPjax = () => void scan();
  const onResize = () => records.forEach(({ card }) => applyMobileLayout(card, config.mobileLayoutMode));
  document.addEventListener("pjax:success", onPjax);
  window.addEventListener("resize", onResize);
  const themeObserver = observeTheme(config.darkClassSelector, (nextTheme) => {
    if (nextTheme === theme) return;
    theme = nextTheme;
    records.forEach((record) => {
      record.card.setTheme(theme);
      void renderRecord(record);
    });
  });

  return {
    scan,
    whenIdle: () => renderChain,
    destroy: () => {
      document.removeEventListener("pjax:success", onPjax);
      window.removeEventListener("resize", onResize);
      themeObserver.disconnect();
      records.forEach((record) => {
        if (record.objectUrl) URL.revokeObjectURL(record.objectUrl);
        record.card.destroy();
      });
      records.clear();
    },
  };
}

function parseConfig(script: HTMLScriptElement): RuntimeConfig {
  try {
    const raw = JSON.parse(script.dataset.config ?? "{}") as Partial<RuntimeConfig>;
    const modes: MobileLayoutMode[] = ["scroll", "scale", "thumbnail"];
    return {
      darkClassSelector: raw.darkClassSelector || DEFAULT_CONFIG.darkClassSelector,
      mermaidSelector: raw.mermaidSelector || DEFAULT_CONFIG.mermaidSelector,
      mobileLayoutMode: modes.includes(raw.mobileLayoutMode as MobileLayoutMode)
        ? raw.mobileLayoutMode as MobileLayoutMode
        : "scroll",
      mermaidOutputFormat: isOutputFormat(raw.mermaidOutputFormat)
        ? raw.mermaidOutputFormat
        : "svg",
      plantumlOutputFormat: isOutputFormat(raw.plantumlOutputFormat)
        ? raw.plantumlOutputFormat
        : "svg",
    };
  } catch {
    return DEFAULT_CONFIG;
  }
}

export function startTextDiagramRuntime(): TextDiagramRuntime | undefined {
  const script = document.querySelector<HTMLScriptElement>("script[data-halo-text-diagram-runtime]");
  if (!script) return undefined;
  const config = parseConfig(script);
  const runtime = createRuntime(config, {
    mermaid: createMermaidAdapter(undefined, config.mermaidOutputFormat),
    plantuml: createPlantUmlAdapter(config.plantumlOutputFormat),
  });
  void runtime.scan();
  return runtime;
}

startTextDiagramRuntime();

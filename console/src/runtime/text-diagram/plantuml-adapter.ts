import { exportSvg, type SvgInput } from "./image-exporter";
import type { DiagramAdapter, DiagramOutputFormat, DiagramTheme, RenderResult } from "./types";

const PLANTUML_SRC = "/plugins/text-diagram/assets/static/plantuml.js";
const VIZ_SRC = "/plugins/text-diagram/assets/static/viz-global.js";

interface PlantUmlRuntime {
  render(lines: string[], targetId: string, options?: { dark?: boolean }): void;
}

type PlantUmlModule = PlantUmlRuntime;

export type PlantUmlLoader = () => Promise<PlantUmlRuntime>;
export type SvgExporter = (
  svg: SvgInput,
  format: DiagramOutputFormat,
  filenamePrefix?: string,
) => Promise<RenderResult>;

declare global {
  interface Window {
    Viz?: unknown;
  }
}

let sharedLoader: Promise<PlantUmlRuntime> | undefined;
let plantUmlTargetId = 0;

function extensionFromUrl(url: string): string {
  const match = url.split(/[?#]/, 1)[0].match(/\.(svg|png|webp)$/i);
  return match?.[1].toLowerCase() ?? "png";
}

function loadScriptOnce(src: string, marker: string): Promise<void> {
  if (window.Viz) return Promise.resolve();
  const existing = document.querySelector<HTMLScriptElement>(`script[${marker}]`);
  const script = existing ?? document.createElement("script");
  script.setAttribute(marker, "");
  script.src ||= src;
  script.async = true;
  return new Promise((resolve, reject) => {
    const cleanup = () => {
      window.clearTimeout(timeout);
      script.removeEventListener("load", loaded);
      script.removeEventListener("error", failed);
    };
    const done = () => {
      cleanup();
      resolve();
    };
    const fail = (error: Error) => {
      cleanup();
      reject(error);
    };
    const timeout = window.setTimeout(() => fail(new Error("PlantUML 布局引擎加载超时")), 10000);
    const loaded = () => done();
    const failed = () => fail(new Error("PlantUML 布局引擎加载失败"));
    script.addEventListener("load", loaded);
    script.addEventListener("error", failed);
    if (!existing) document.head.append(script);
  });
}

export function loadPlantUml(): Promise<PlantUmlRuntime> {
  if (sharedLoader) return sharedLoader;
  sharedLoader = loadScriptOnce(VIZ_SRC, "data-halo-text-diagram-viz")
    .then(async () => {
      const module = await import(/* webpackIgnore: true */ PLANTUML_SRC) as PlantUmlModule;
      return module;
    })
    .catch((error) => {
      sharedLoader = undefined;
      throw error;
    });
  return sharedLoader;
}

function renderToSvg(runtime: PlantUmlRuntime, source: string, theme: DiagramTheme): Promise<string> {
  const target = document.createElement("div");
  target.id = `text-diagram-plantuml-${++plantUmlTargetId}`;
  target.hidden = true;
  document.body.append(target);

  return new Promise((resolve, reject) => {
    const cleanup = () => {
      window.clearTimeout(timeout);
      observer.disconnect();
      target.remove();
    };
    const fail = (error: Error) => {
      cleanup();
      reject(error);
    };
    const timeout = window.setTimeout(() => fail(new Error("PlantUML 渲染超时")), 10000);
    const observer = new MutationObserver(() => {
      if (!target.querySelector("svg")) return;
      const svg = target.innerHTML;
      cleanup();
      resolve(svg);
    });
    observer.observe(target, { childList: true, subtree: true });

    try {
      runtime.render(source.split(/\r\n|\r|\n/), target.id, theme === "dark" ? { dark: true } : undefined);
    } catch (error) {
      fail(error instanceof Error ? error : new Error(String(error)));
    }
  });
}

function renderLegacyImage(target: HTMLElement): Promise<RenderResult> {
  const sourceUrl = target.dataset.src;
  if (!sourceUrl) return Promise.reject(new Error("PlantUML 图片地址缺失"));
  const image = document.createElement("img");
  image.alt = "PlantUML diagram";
  image.src = sourceUrl;
  return new Promise((resolve, reject) => {
    image.addEventListener("load", () => {
      const extension = extensionFromUrl(sourceUrl);
      resolve({
        element: image,
        filename: `text-diagram.${extension}`,
        mimeType: extension === "svg" ? "image/svg+xml" : `image/${extension}`,
        downloadUrl: sourceUrl,
      });
    }, { once: true });
    image.addEventListener("error", () => reject(new Error("PlantUML 图片加载失败")), { once: true });
  });
}

export function createPlantUmlAdapter(
  outputFormat: DiagramOutputFormat = "svg",
  loader: PlantUmlLoader = loadPlantUml,
  svgExporter: SvgExporter = exportSvg,
): DiagramAdapter {
  return {
    async render(target: HTMLElement, source: string, theme: DiagramTheme): Promise<RenderResult> {
      if (!source.trim()) {
        return renderLegacyImage(target);
      }
      try {
        const runtime = await loader();
        const svg = await renderToSvg(runtime, source, theme);
        return svgExporter(svg, outputFormat, "text-diagram");
      } catch (error) {
        if (target.dataset.src) {
          return renderLegacyImage(target);
        }
        throw error;
      }
    },
  };
}

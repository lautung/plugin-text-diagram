import type { DiagramOutputFormat, RenderResult } from "./types";

export type SvgInput = SVGElement | string;

function parseSvg(svg: string): SVGElement {
  const document = new DOMParser().parseFromString(svg, "image/svg+xml");
  const element = document.documentElement;
  if (element.tagName.toLowerCase() !== "svg") {
    throw new Error("SVG 内容无效");
  }
  return element as unknown as SVGElement;
}

function cloneSvg(svg: SvgInput): SVGElement {
  if (typeof svg === "string") return parseSvg(svg);
  return svg.cloneNode(true) as SVGElement;
}

function serializeSvg(svg: SVGElement): string {
  return new XMLSerializer().serializeToString(svg);
}

function readDimension(value: string | null): number | undefined {
  if (!value) return undefined;
  const match = value.trim().match(/^(\d+(?:\.\d+)?)/);
  if (!match) return undefined;
  const number = Number(match[1]);
  return Number.isFinite(number) && number > 0 ? number : undefined;
}

function svgSize(svg: SVGElement): { width: number; height: number } {
  const width = readDimension(svg.getAttribute("width"));
  const height = readDimension(svg.getAttribute("height"));
  if (width && height) return { width, height };

  const viewBox = svg.getAttribute("viewBox")?.trim().split(/\s+/).map(Number);
  if (viewBox?.length === 4 && viewBox.every(Number.isFinite) && viewBox[2] > 0 && viewBox[3] > 0) {
    return { width: viewBox[2], height: viewBox[3] };
  }

  return { width: 300, height: 150 };
}

function loadImage(url: string): Promise<HTMLImageElement> {
  const image = document.createElement("img");
  image.decoding = "async";
  return new Promise((resolve, reject) => {
    image.addEventListener("load", () => resolve(image), { once: true });
    image.addEventListener("error", () => reject(new Error("SVG 位图转换失败")), { once: true });
    image.src = url;
  });
}

function canvasToBlob(canvas: HTMLCanvasElement, mimeType: string): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (blob) {
        resolve(blob);
        return;
      }
      reject(new Error("图表导出失败"));
    }, mimeType);
  });
}

export async function exportSvg(
  svgInput: SvgInput,
  format: DiagramOutputFormat,
  filenamePrefix = "text-diagram",
): Promise<RenderResult> {
  const svg = cloneSvg(svgInput);
  if (format === "svg") {
    return {
      element: svg,
      filename: `${filenamePrefix}.svg`,
      mimeType: "image/svg+xml",
    };
  }

  const { width, height } = svgSize(svg);
  const svgUrl = URL.createObjectURL(new Blob([serializeSvg(svg)], { type: "image/svg+xml;charset=utf-8" }));
  try {
    const image = await loadImage(svgUrl);
    const canvas = document.createElement("canvas");
    canvas.width = Math.ceil(width);
    canvas.height = Math.ceil(height);
    const context = canvas.getContext("2d");
    if (!context) throw new Error("Canvas 不可用");
    context.drawImage(image, 0, 0, canvas.width, canvas.height);

    const mimeType = format === "webp" ? "image/webp" : "image/png";
    const blob = await canvasToBlob(canvas, mimeType);
    const url = URL.createObjectURL(blob);
    const preview = document.createElement("img");
    preview.alt = "diagram";
    preview.src = url;
    return {
      element: preview,
      filename: `${filenamePrefix}.${format}`,
      mimeType,
      downloadUrl: url,
    };
  } finally {
    URL.revokeObjectURL(svgUrl);
  }
}

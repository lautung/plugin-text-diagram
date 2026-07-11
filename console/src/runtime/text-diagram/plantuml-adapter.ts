import type { DiagramAdapter, RenderResult } from "./types";

function extensionFromUrl(url: string): string {
  const match = url.split(/[?#]/, 1)[0].match(/\.(svg|png|webp)$/i);
  return match?.[1].toLowerCase() ?? "png";
}

export function createPlantUmlAdapter(): DiagramAdapter {
  return {
    render(target: HTMLElement): Promise<RenderResult> {
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
    },
  };
}

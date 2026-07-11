import { expect, it, vi } from "vitest";
import { exportSvg } from "./image-exporter";

it("returns an SVG element without rasterizing when format is svg", async () => {
  const result = await exportSvg(`<svg viewBox="0 0 10 20"></svg>`, "svg");

  expect(result.filename).toBe("text-diagram.svg");
  expect(result.mimeType).toBe("image/svg+xml");
  expect(result.element.tagName.toLowerCase()).toBe("svg");
});

it("rasterizes SVG to PNG through canvas", async () => {
  const createObjectURL = vi.fn()
    .mockReturnValueOnce("blob:source-svg")
    .mockReturnValueOnce("blob:output-png");
  const revokeObjectURL = vi.fn();
  Object.defineProperty(URL, "createObjectURL", { configurable: true, value: createObjectURL });
  Object.defineProperty(URL, "revokeObjectURL", { configurable: true, value: revokeObjectURL });

  const createElement = document.createElement.bind(document);
  const canvas = createElement("canvas");
  const context = { drawImage: vi.fn() };
  const getContext = vi.fn(() => context as unknown as CanvasRenderingContext2D);
  const toBlob = vi.fn((callback: BlobCallback) => {
    callback(new Blob(["png"], { type: "image/png" }));
  });
  Object.defineProperty(canvas, "getContext", { configurable: true, value: getContext });
  Object.defineProperty(canvas, "toBlob", { configurable: true, value: toBlob });
  let image: HTMLImageElement | undefined;
  vi.spyOn(document, "createElement").mockImplementation(((tagName: string, options?: ElementCreationOptions) => {
    if (tagName.toLowerCase() === "canvas") return canvas;
    const element = createElement(tagName, options);
    if (tagName.toLowerCase() === "img") image = element as HTMLImageElement;
    return element;
  }) as typeof document.createElement);

  const pending = exportSvg(`<svg width="12" height="24"></svg>`, "png");
  await Promise.resolve();
  expect(image).toBeDefined();
  image!.dispatchEvent(new Event("load"));
  const result = await pending;

  expect(canvas.width).toBe(12);
  expect(canvas.height).toBe(24);
  expect(toBlob).toHaveBeenCalledWith(expect.any(Function), "image/png");
  expect(result.filename).toBe("text-diagram.png");
  expect(result.mimeType).toBe("image/png");
  expect(result.downloadUrl).toBe("blob:output-png");
  expect(revokeObjectURL).toHaveBeenCalledWith("blob:source-svg");
});

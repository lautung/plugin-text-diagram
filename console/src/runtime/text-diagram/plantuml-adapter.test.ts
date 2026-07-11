import { expect, it, vi } from "vitest";
import { createPlantUmlAdapter } from "./plantuml-adapter";

function captureCreatedImage() {
  const createElement = document.createElement.bind(document);
  let image: HTMLImageElement | undefined;
  vi.spyOn(document, "createElement").mockImplementation(((tagName: string, options?: ElementCreationOptions) => {
    const element = createElement(tagName, options);
    if (tagName.toLowerCase() === "img") image = element as HTMLImageElement;
    return element;
  }) as typeof document.createElement);
  return () => image;
}

it("renders PlantUML source in the browser and exports the configured format", async () => {
  const render = vi.fn((_: string[], targetId: string) => {
    document.getElementById(targetId)!.innerHTML = `<svg viewBox="0 0 10 10"></svg>`;
  });
  const exporter = vi.fn(async () => ({
    element: document.createElement("svg"),
    filename: "text-diagram.webp",
    mimeType: "image/webp",
  }));
  const adapter = createPlantUmlAdapter("webp", async () => ({ render }), exporter);

  const result = await adapter.render(document.createElement("div"), "@startuml\nA->B\n@enduml", "light");

  expect(render).toHaveBeenCalled();
  expect(exporter).toHaveBeenCalledWith(expect.stringContaining("<svg"), "webp", "text-diagram");
  expect(result.filename).toBe("text-diagram.webp");
});

it("uses a legacy PlantUML image when source is missing", async () => {
  const image = captureCreatedImage();
  const target = document.createElement("text-diagram");
  target.dataset.src = "https://example.test/diagram.png";
  const pending = createPlantUmlAdapter("svg").render(target, "", "light");
  await Promise.resolve();
  expect(image()).toBeDefined();
  image()!.dispatchEvent(new Event("load"));

  await expect(pending).resolves.toMatchObject({
    filename: "text-diagram.png",
    mimeType: "image/png",
    downloadUrl: "https://example.test/diagram.png",
  });
});

it("falls back to a legacy image when local PlantUML rendering fails", async () => {
  const image = captureCreatedImage();
  const target = document.createElement("text-diagram");
  target.dataset.src = "https://example.test/diagram.svg";
  const adapter = createPlantUmlAdapter("png", async () => {
    throw new Error("local failed");
  });

  const pending = adapter.render(target, "@startuml\nA->B\n@enduml", "light");
  await Promise.resolve();
  expect(image()).toBeDefined();
  image()!.dispatchEvent(new Event("load"));

  await expect(pending).resolves.toMatchObject({
    filename: "text-diagram.svg",
    mimeType: "image/svg+xml",
  });
});

import { icons } from "./icons";
import type { DiagramTheme, DiagramType } from "./types";

export interface DiagramCard extends HTMLElement {
  setPreview(element: HTMLElement | SVGElement): void;
  setError(message: string): void;
  setDownload(url: string, filename: string): void;
  setTheme(theme: DiagramTheme): void;
  destroy(): void;
}

function createButton(action: string, label: string, icon: string): HTMLButtonElement {
  const button = document.createElement("button");
  button.type = "button";
  button.dataset.action = action;
  button.title = label;
  button.setAttribute("aria-label", label);
  button.innerHTML = icon;
  return button;
}

function selectSource(code: HTMLElement): void {
  const selection = window.getSelection();
  const range = document.createRange();
  range.selectNodeContents(code);
  selection?.removeAllRanges();
  selection?.addRange(range);
}

export function createDiagramCard(
  host: HTMLElement,
  type: DiagramType,
  source: string,
): DiagramCard {
  const card = host as DiagramCard;
  const toolbar = document.createElement("div");
  const title = document.createElement("div");
  const actions = document.createElement("div");
  const canvas = document.createElement("div");
  const sourcePanel = document.createElement("pre");
  const code = document.createElement("code");
  const sourceButton = createButton("source", "查看源码", icons.source);
  const previewButton = createButton("preview", "显示图表", icons.preview);
  const fullscreenButton = createButton("fullscreen", "全屏查看", icons.fullscreen);
  const copyButton = createButton("copy", "复制源码", icons.copy);
  const download = document.createElement("a");

  card.dataset.textDiagramCard = "";
  card.dataset.textDiagramView = "preview";
  card.dataset.textDiagramFullscreen = "false";
  card.dataset.textDiagramTheme = "light";
  toolbar.dataset.textDiagramToolbar = "";
  title.dataset.textDiagramTitle = "";
  title.innerHTML = icons.diagram;
  title.append(document.createTextNode(type === "mermaid" ? "Mermaid" : "PlantUML"));
  actions.dataset.textDiagramActions = "";
  canvas.dataset.textDiagramCanvas = "";
  sourcePanel.dataset.textDiagramSource = "";
  code.textContent = source;
  sourcePanel.append(code);

  download.dataset.action = "download";
  download.title = "下载图表";
  download.setAttribute("aria-label", "下载图表");
  download.setAttribute("aria-disabled", "true");
  download.innerHTML = icons.download;
  actions.append(sourceButton, previewButton, fullscreenButton, copyButton, download);
  toolbar.append(title, actions);
  card.replaceChildren(toolbar, canvas, sourcePanel);

  const setView = (view: "preview" | "source") => {
    card.dataset.textDiagramView = view;
    sourceButton.setAttribute("aria-pressed", String(view === "source"));
    previewButton.setAttribute("aria-pressed", String(view === "preview"));
  };

  const onClick = async (event: Event) => {
    const actionElement = (event.target as Element).closest<HTMLElement>("[data-action]");
    if (!actionElement || !card.contains(actionElement)) return;
    const action = actionElement.dataset.action;
    if (action === "source") setView("source");
    if (action === "preview") setView("preview");
    if (action === "fullscreen") {
      card.dataset.textDiagramFullscreen = String(card.dataset.textDiagramFullscreen !== "true");
    }
    if (action === "copy") {
      try {
        await navigator.clipboard.writeText(source);
        copyButton.innerHTML = icons.check;
        window.setTimeout(() => (copyButton.innerHTML = icons.copy), 1500);
      } catch {
        setView("source");
        selectSource(code);
      }
    }
  };

  const onKeydown = (event: KeyboardEvent) => {
    if (event.key === "Escape") card.dataset.textDiagramFullscreen = "false";
  };

  card.addEventListener("click", onClick);
  document.addEventListener("keydown", onKeydown);
  setView("preview");

  card.setPreview = (element) => canvas.replaceChildren(element);
  card.setError = (message) => {
    const error = document.createElement("pre");
    error.dataset.textDiagramError = "";
    error.textContent = message;
    canvas.replaceChildren(error);
  };
  card.setDownload = (url, filename) => {
    download.href = url;
    download.download = filename;
    download.setAttribute("aria-disabled", "false");
  };
  card.setTheme = (theme) => {
    card.dataset.textDiagramTheme = theme;
  };
  card.destroy = () => {
    card.removeEventListener("click", onClick);
    document.removeEventListener("keydown", onKeydown);
  };
  return card;
}

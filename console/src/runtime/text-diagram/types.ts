export type DiagramType = "mermaid" | "plantuml";
export type MobileLayoutMode = "scroll" | "scale" | "thumbnail";
export type DiagramTheme = "light" | "dark";

export interface RuntimeConfig {
  darkClassSelector: string;
  mermaidSelector: string;
  mobileLayoutMode: MobileLayoutMode;
}

export interface RenderResult {
  element: HTMLElement | SVGElement;
  filename: string;
  mimeType: string;
  downloadUrl?: string;
}

export interface DiagramAdapter {
  render(
    target: HTMLElement,
    source: string,
    theme: DiagramTheme,
  ): Promise<RenderResult>;
}

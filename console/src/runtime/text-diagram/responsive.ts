import type { MobileLayoutMode } from "./types";

export const MOBILE_BREAKPOINT = 768;

export function applyMobileLayout(
  card: HTMLElement,
  mode: MobileLayoutMode,
  width = window.innerWidth,
): void {
  card.dataset.textDiagramMobileLayout =
    width < MOBILE_BREAKPOINT ? mode : "desktop";
}

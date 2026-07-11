import type { DiagramTheme } from "./types";

export function detectTheme(selector: string): DiagramTheme {
  try {
    return document.querySelector(selector) ? "dark" : "light";
  } catch {
    return "light";
  }
}

export function observeTheme(
  selector: string,
  callback: (theme: DiagramTheme) => void,
): MutationObserver {
  const observer = new MutationObserver(() => callback(detectTheme(selector)));
  observer.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ["class", "data-theme"],
  });
  return observer;
}

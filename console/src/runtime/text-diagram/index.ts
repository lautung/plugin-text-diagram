import "./style.css";

export function startTextDiagramRuntime(): void {
  document.documentElement.dataset.textDiagramRuntime = "ready";
}

startTextDiagramRuntime();

package run.halo.plugin.textdiagram;

public class JSInjector {
    private static final String STATE_KEY = "__haloTextDiagramRenderer";

    static String getParsedMermaidScript(String darkClassSelector, String mermaidSelector) {
        String mermaidScript = """
            <style data-halo-text-diagram-style>
              [data-text-diagram-render-target] {
                display: block !important;
                max-width: 100%%;
                overflow-x: auto;
                overscroll-behavior-x: contain;
                -webkit-overflow-scrolling: touch;
              }

              [data-text-diagram-render-target] > svg {
                display: block;
                max-width: none !important;
                height: auto;
                margin-inline: auto;
              }

              [data-text-diagram-render-target] foreignObject p {
                margin: 0 !important;
                line-height: 1.5 !important;
              }

              [data-text-diagram-card] {
                --text-diagram-card-background: #ffffff;
                --text-diagram-card-border: #e5e7eb;
                --text-diagram-toolbar-background: #ffffff;
                --text-diagram-canvas-background: #f7f7f8;
                --text-diagram-text: #111827;
                --text-diagram-muted: #6b7280;
                --text-diagram-button-background: transparent;
                --text-diagram-button-active: #ececf0;
                --text-diagram-code-background: #f7f7f8;
                display: block !important;
                max-width: 100%%;
                margin: 1rem 0;
                overflow: hidden;
                color: var(--text-diagram-text);
                background: var(--text-diagram-card-background);
                border: 1px solid var(--text-diagram-card-border);
                border-radius: 14px;
                box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
              }

              [data-text-diagram-card][data-text-diagram-theme="dark"] {
                --text-diagram-card-background: #18181b;
                --text-diagram-card-border: #3f3f46;
                --text-diagram-toolbar-background: #18181b;
                --text-diagram-canvas-background: #242428;
                --text-diagram-text: #f4f4f5;
                --text-diagram-muted: #a1a1aa;
                --text-diagram-button-active: #3f3f46;
                --text-diagram-code-background: #27272a;
              }

              [data-text-diagram-toolbar] {
                display: flex;
                min-height: 52px;
                align-items: center;
                justify-content: space-between;
                gap: 1rem;
                padding: 0.625rem 0.875rem;
                background: var(--text-diagram-toolbar-background);
                border-bottom: 1px solid var(--text-diagram-card-border);
              }

              [data-text-diagram-title] {
                display: inline-flex;
                min-width: 0;
                align-items: center;
                gap: 0.5rem;
                font-weight: 600;
                line-height: 1.25;
              }

              [data-text-diagram-title-icon] {
                display: inline-grid;
                width: 1.5rem;
                height: 1.5rem;
                flex: 0 0 auto;
                place-items: center;
                color: var(--text-diagram-muted);
                font-size: 1.125rem;
              }

              [data-text-diagram-actions] {
                display: inline-flex;
                flex: 0 0 auto;
                align-items: center;
                gap: 0.25rem;
              }

              [data-text-diagram-action] {
                display: inline-grid;
                width: 2.25rem;
                height: 2.25rem;
                place-items: center;
                padding: 0;
                color: var(--text-diagram-text);
                background: var(--text-diagram-button-background);
                border: 0;
                border-radius: 999px;
                cursor: pointer;
                font: inherit;
                line-height: 1;
                text-decoration: none;
              }

              [data-text-diagram-action]:hover,
              [data-text-diagram-action]:focus-visible,
              [data-text-diagram-action][aria-pressed="true"] {
                background: var(--text-diagram-button-active);
                outline: none;
              }

              [data-text-diagram-action][aria-disabled="true"] {
                color: var(--text-diagram-muted);
                cursor: not-allowed;
              }

              [data-text-diagram-canvas] {
                min-height: 150px;
                padding: 1.5rem;
                overflow: auto;
                overscroll-behavior: contain;
                background: var(--text-diagram-canvas-background);
                -webkit-overflow-scrolling: touch;
              }

              [data-text-diagram-canvas] > svg {
                display: block;
                max-width: none !important;
                height: auto;
                margin: 0 auto;
              }

              [data-text-diagram-canvas] foreignObject p {
                margin: 0 !important;
                line-height: 1.5 !important;
              }

              [data-text-diagram-error] {
                margin: 0;
                padding: 0.875rem;
                overflow: auto;
                color: #b42318;
                background: #fff1f1;
                border: 1px solid #fecaca;
                border-radius: 8px;
                white-space: pre-wrap;
              }

              [data-text-diagram-code-panel] {
                display: none;
                margin: 0;
                padding: 1rem;
                overflow: auto;
                color: var(--text-diagram-text);
                background: var(--text-diagram-code-background);
                font-size: 0.875rem;
                line-height: 1.6;
              }

              [data-text-diagram-code-panel] code {
                white-space: pre;
              }

              [data-text-diagram-card][data-text-diagram-view="code"] [data-text-diagram-canvas] {
                display: none;
              }

              [data-text-diagram-card][data-text-diagram-view="code"] [data-text-diagram-code-panel] {
                display: block;
              }

              [data-text-diagram-card][data-text-diagram-fullscreen="true"] {
                position: fixed;
                inset: 1.5rem;
                z-index: 2147483647;
                display: flex !important;
                flex-direction: column;
                margin: 0;
                border-radius: 18px;
                box-shadow: 0 24px 80px rgba(0, 0, 0, 0.28);
              }

              [data-text-diagram-card][data-text-diagram-fullscreen="true"] [data-text-diagram-canvas],
              [data-text-diagram-card][data-text-diagram-fullscreen="true"] [data-text-diagram-code-panel] {
                flex: 1 1 auto;
                min-height: 0;
              }

              @media (max-width: 640px) {
                [data-text-diagram-toolbar] {
                  padding-inline: 0.75rem;
                }

                [data-text-diagram-canvas] {
                  min-height: 120px;
                  padding: 1rem;
                }

                [data-text-diagram-card][data-text-diagram-fullscreen="true"] {
                  inset: 0.5rem;
                  border-radius: 14px;
                }
              }
            </style>
            <script>
              (function () {
                const STATE_KEY = "%s";
                const MERMAID_SRC = "/plugins/text-diagram/assets/static/mermaid.min.js";
                const configuration = {
                  darkClassSelector: %s,
                  mermaidSelector: %s,
                };

                function reportError(error) {
                  console.error("Text Diagram Mermaid rendering failed", error);
                }

                function detectDarkMode() {
                  try {
                    return Boolean(
                      (configuration.darkClassSelector
                        && document.querySelector(configuration.darkClassSelector) !== null)
                      || document.documentElement.classList.contains("night")
                    );
                  } catch (error) {
                    reportError(error);
                    return document.documentElement.classList.contains("night");
                  }
                }

                function isMermaidReady() {
                  return Boolean(window.mermaid && window.mermaid.initialize && window.mermaid.run);
                }

                function getTargets() {
                  if (!configuration.mermaidSelector) {
                    return [];
                  }

                  try {
                    return Array.from(document.querySelectorAll(configuration.mermaidSelector))
                      .filter((element) => !element.hasAttribute("data-processed"));
                  } catch (error) {
                    reportError(error);
                    return [];
                  }
                }

                function waitForFonts() {
                  if (document.fonts && document.fonts.ready) {
                    return document.fonts.ready;
                  }
                return Promise.resolve();
              }

              function getDiagramSource(target) {
                return target.getAttribute("data-content") || target.textContent || "";
                }

                function createAction(action, label, title) {
                  const button = document.createElement("button");
                button.type = "button";
                button.setAttribute("data-text-diagram-action", action);
                button.setAttribute("aria-label", title);
                button.title = title;
                  button.innerHTML = label;
                  return button;
                }

                function createDownloadAction(label, title) {
                  const link = document.createElement("a");
                  link.setAttribute("data-text-diagram-action", "download");
                  link.setAttribute("aria-disabled", "true");
                  link.setAttribute("aria-label", title);
                  link.title = title;
                  link.innerHTML = label;
                  return link;
                }

                function setCardView(card, view) {
                  card.setAttribute("data-text-diagram-view", view);
                  card.querySelectorAll("[data-text-diagram-action]").forEach(function (button) {
                    const action = button.getAttribute("data-text-diagram-action");
                    if (action === "show-code" || action === "show-preview") {
                      button.setAttribute("aria-pressed", action === "show-" + view ? "true" : "false");
                    }
                  });
                }

                function copyTextFallback(text) {
                  const textarea = document.createElement("textarea");
                  textarea.value = text;
                  textarea.setAttribute("readonly", "");
                  textarea.style.position = "fixed";
                  textarea.style.top = "0";
                  textarea.style.left = "0";
                  textarea.style.opacity = "0";
                  document.body.appendChild(textarea);
                  textarea.focus({ preventScroll: true });
                  textarea.select();
                  textarea.setSelectionRange(0, textarea.value.length);
                  const copied = document.execCommand("copy");
                  textarea.remove();
                  return copied
                    ? Promise.resolve()
                    : Promise.reject(new Error("Copy command was not accepted"));
                }

                function copyText(text) {
                  if (navigator.clipboard && navigator.clipboard.writeText) {
                    return navigator.clipboard.writeText(text).catch(function () {
                      return copyTextFallback(text);
                    });
                  }

                  return copyTextFallback(text);
                }

                function updateDownloadAction(card) {
                  const svg = card.querySelector("[data-text-diagram-canvas] svg");
                  const link = card.querySelector("[data-text-diagram-action='download']");
                  if (!svg || !(link instanceof HTMLAnchorElement)) {
                    if (link) {
                      const previousUrl = link.getAttribute("data-text-diagram-object-url");
                      if (previousUrl) {
                        URL.revokeObjectURL(previousUrl);
                      }
                      link.removeAttribute("href");
                      link.removeAttribute("data-text-diagram-object-url");
                      link.setAttribute("aria-disabled", "true");
                    }
                    return;
                  }

                  const svgText = new XMLSerializer().serializeToString(svg);
                  const previousUrl = link.getAttribute("data-text-diagram-object-url");
                  if (previousUrl) {
                    URL.revokeObjectURL(previousUrl);
                  }
                  const url = URL.createObjectURL(new Blob([svgText], {
                    type: "image/svg+xml;charset=utf-8",
                  }));
                  link.href = url;
                  link.download = "text-diagram.svg";
                  link.setAttribute("data-text-diagram-object-url", url);
                  link.removeAttribute("aria-disabled");
                }

                function selectCodePanel(card) {
                  const code = card.querySelector("[data-text-diagram-code-panel] code");
                  if (!code || !window.getSelection) {
                    return;
                  }

                  setCardView(card, "code");
                  const range = document.createRange();
                  range.selectNodeContents(code);
                  const selection = window.getSelection();
                  selection.removeAllRanges();
                  selection.addRange(range);
                }

                function onCardClick(event) {
                  if (!(event.target instanceof Element)) {
                    return;
                  }

                  const actionButton = event.target.closest("[data-text-diagram-action]");
                  if (!actionButton) {
                    return;
                  }

                  const card = actionButton.closest("[data-text-diagram-card]");
                  if (!card) {
                    return;
                  }

                  const action = actionButton.getAttribute("data-text-diagram-action");
                  if (action === "show-code") {
                    setCardView(card, "code");
                  } else if (action === "show-preview") {
                    setCardView(card, "preview");
                  } else if (action === "fullscreen") {
                    const nextValue = card.getAttribute("data-text-diagram-fullscreen") !== "true";
                    card.setAttribute("data-text-diagram-fullscreen", String(nextValue));
                  } else if (action === "copy") {
                    card.setAttribute("data-text-diagram-copy-state", "copying");
                    copyText(card.getAttribute("data-text-diagram-source") || "")
                      .then(function () {
                        card.setAttribute("data-text-diagram-copy-state", "copied");
                      })
                      .catch(function (error) {
                        card.setAttribute("data-text-diagram-copy-state", "failed");
                        selectCodePanel(card);
                        reportError(error);
                      });
                  }
                }

                function createCardShell(target, source) {
                  const toolbar = document.createElement("div");
                  const title = document.createElement("div");
                  const titleIcon = document.createElement("span");
                  const actions = document.createElement("div");
                  const canvas = document.createElement("div");
                  const codePanel = document.createElement("pre");
                  const code = document.createElement("code");

                  target.setAttribute("data-text-diagram-card", "");
                  target.setAttribute("data-text-diagram-source", source);
                  target.setAttribute("data-text-diagram-theme", detectDarkMode() ? "dark" : "light");
                  toolbar.setAttribute("data-text-diagram-toolbar", "");
                  title.setAttribute("data-text-diagram-title", "");
                  titleIcon.setAttribute("data-text-diagram-title-icon", "");
                  titleIcon.textContent = "▱";
                  title.append(titleIcon, "Mermaid");
                  actions.setAttribute("data-text-diagram-actions", "");
                  actions.append(
                    createAction("show-code", "&lt;/&gt;", "显示代码"),
                    createAction("show-preview", "▶", "显示图表"),
                    createAction("fullscreen", "⛶", "全屏查看"),
                    createAction("copy", "⧉", "复制代码"),
                    createDownloadAction("↓", "下载 SVG")
                  );
                  toolbar.append(title, actions);

                  canvas.setAttribute("data-text-diagram-canvas", "");
                  codePanel.setAttribute("data-text-diagram-code-panel", "");
                  code.textContent = source;
                  codePanel.append(code);
                  target.replaceChildren(toolbar, canvas, codePanel);
                  target.addEventListener("click", onCardClick);
                  setCardView(target, "preview");
                  return canvas;
                }

                function decorateRenderedTarget(renderItem) {
                  const target = renderItem.target;
                  if (!target.isConnected || target.hasAttribute("data-text-diagram-card")) {
                    return;
                  }

                  const renderedNodes = Array.from(target.childNodes);
                  const canvas = createCardShell(target, renderItem.source);
                  canvas.append(...renderedNodes);
                  updateDownloadAction(target);
                }

                function decorateFailedTarget(renderItem, error) {
                  const target = renderItem.target;
                  if (!target.isConnected || target.hasAttribute("data-text-diagram-card")) {
                    return;
                  }

                  const canvas = createCardShell(target, renderItem.source);
                  const errorElement = document.createElement("pre");
                  errorElement.setAttribute("data-text-diagram-error", "");
                  errorElement.textContent = error instanceof Error ? error.message : String(error);
                  canvas.replaceChildren(errorElement);
                }

                function createRendererState() {
                  const state = {
                    renderChain: Promise.resolve(),
                    initialization: null,
                    loadPromise: null,
                    scheduleRender: null,
                    updateConfiguration: null,
                  };

                  state.updateConfiguration = function (nextConfiguration) {
                    const changed =
                      configuration.darkClassSelector !== nextConfiguration.darkClassSelector
                      || configuration.mermaidSelector !== nextConfiguration.mermaidSelector;
                    configuration.darkClassSelector = nextConfiguration.darkClassSelector;
                    configuration.mermaidSelector = nextConfiguration.mermaidSelector;
                    if (changed) {
                      state.initialization = null;
                    }
                  };

                  function findMermaidScript() {
                    const markedScript = document.querySelector(
                      "script[data-halo-text-diagram-mermaid]"
                    );
                    if (markedScript) {
                      return markedScript;
                    }

                    const expectedSource = new URL(MERMAID_SRC, document.baseURI).href;
                    return Array.from(document.scripts)
                      .find((script) => script.src === expectedSource) || null;
                  }

                  function loadMermaid() {
                    if (isMermaidReady()) {
                      return Promise.resolve(window.mermaid);
                    }
                    if (state.loadPromise) {
                      return state.loadPromise;
                    }

                    state.loadPromise = new Promise(function (resolve, reject) {
                      let script = findMermaidScript();
                      const shouldAppend = !script;
                      if (!script) {
                        script = document.createElement("script");
                        script.src = MERMAID_SRC;
                        script.async = true;
                      }
                      script.setAttribute("data-halo-text-diagram-mermaid", "");

                      let timeoutId;

                      function cleanup() {
                        window.clearTimeout(timeoutId);
                        script.removeEventListener("load", onLoad);
                        script.removeEventListener("error", onError);
                      }

                      function rejectLoad(error) {
                        cleanup();
                        script.remove();
                        state.loadPromise = null;
                        state.initialization = null;
                        reject(error);
                      }

                      function onLoad() {
                        if (!isMermaidReady()) {
                          rejectLoad(new Error("Mermaid library loaded without a compatible API"));
                          return;
                        }
                        cleanup();
                        resolve(window.mermaid);
                      }

                      function onError() {
                        rejectLoad(new Error("Mermaid library failed to load"));
                      }

                      script.addEventListener("load", onLoad);
                      script.addEventListener("error", onError);
                      timeoutId = window.setTimeout(function () {
                        rejectLoad(new Error("Mermaid library was not loaded within 10 seconds"));
                      }, 10000);

                      if (isMermaidReady()) {
                        onLoad();
                      } else if (shouldAppend) {
                        document.head.appendChild(script);
                      }
                    });
                    return state.loadPromise;
                  }

                  function initializeMermaid() {
                    if (state.initialization) {
                      return state.initialization;
                    }

                    state.initialization = loadMermaid()
                      .then(function (mermaid) {
                        if (mermaid.mermaidAPI && typeof mermaid.mermaidAPI.reset === "function") {
                          mermaid.mermaidAPI.reset();
                        }
                        mermaid.initialize({
                          startOnLoad: false,
                          htmlLabels: false,
                          theme: detectDarkMode() ? "dark" : "default",
                          flowchart: {
                            htmlLabels: false,
                            useMaxWidth: false,
                          },
                        });
                        return mermaid;
                      })
                      .catch(function (error) {
                        state.initialization = null;
                        throw error;
                      });
                    return state.initialization;
                  }

                  state.scheduleRender = function () {
                    state.renderChain = state.renderChain
                      .catch(reportError)
                      .then(async function () {
                        if (getTargets().length === 0) {
                          return;
                        }

                        const [mermaid] = await Promise.all([
                          initializeMermaid(),
                          waitForFonts(),
                        ]);
                        const targets = getTargets();
                        if (targets.length === 0) {
                          return;
                        }
                        const renderItems = targets.map((target) => ({
                          target,
                          source: getDiagramSource(target),
                        }));
                        targets.forEach((target) => {
                          target.setAttribute("data-text-diagram-render-target", "");
                        });
                        try {
                          await mermaid.run({ nodes: targets });
                          renderItems.forEach(decorateRenderedTarget);
                        } catch (error) {
                          renderItems.forEach(function (renderItem) {
                            decorateFailedTarget(renderItem, error);
                          });
                          throw error;
                        }
                      })
                      .catch(reportError);
                    return state.renderChain;
                  };

                  document.addEventListener("pjax:success", state.scheduleRender);
                  document.addEventListener("keydown", function (event) {
                    if (event.key !== "Escape") {
                      return;
                    }
                    document
                      .querySelectorAll("[data-text-diagram-card][data-text-diagram-fullscreen='true']")
                      .forEach(function (card) {
                        card.setAttribute("data-text-diagram-fullscreen", "false");
                      });
                  });
                  return state;
                }

                function startRenderer() {
                  const existingState = window[STATE_KEY];
                  if (existingState) {
                    if (typeof existingState.updateConfiguration === "function") {
                      existingState.updateConfiguration(configuration);
                    }
                    existingState.scheduleRender();
                    return;
                  }

                  const state = createRendererState();
                  window[STATE_KEY] = state;
                  state.scheduleRender();
                }

                if (document.readyState === "loading") {
                  document.addEventListener("DOMContentLoaded", startRenderer, { once: true });
                } else {
                  startRenderer();
                }
              })();
            </script>
            """;
        return String.format(
            mermaidScript,
            STATE_KEY,
            toJavaScriptString(darkClassSelector),
            toJavaScriptString(mermaidSelector)
        );
    }

    private static String toJavaScriptString(String value) {
        String safeValue = value == null ? "" : value;
        StringBuilder result = new StringBuilder(safeValue.length() + 2);
        result.append('"');

        for (int index = 0; index < safeValue.length(); index++) {
            char character = safeValue.charAt(index);
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                case '\u2028' -> result.append("\\u2028");
                case '\u2029' -> result.append("\\u2029");
                default -> {
                    if (character < 0x20) {
                        String hex = Integer.toHexString(character);
                        result.append("\\u");
                        result.append("0000", 0, 4 - hex.length());
                        result.append(hex);
                    } else {
                        result.append(character);
                    }
                }
            }
        }

        return result.append('"').toString();
    }
}

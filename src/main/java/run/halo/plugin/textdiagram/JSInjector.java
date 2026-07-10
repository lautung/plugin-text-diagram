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
                        const isDarkMode =
                          (configuration.darkClassSelector
                            && document.querySelector(configuration.darkClassSelector) !== null)
                          || document.documentElement.classList.contains("night");
                        if (mermaid.mermaidAPI && typeof mermaid.mermaidAPI.reset === "function") {
                          mermaid.mermaidAPI.reset();
                        }
                        mermaid.initialize({
                          startOnLoad: false,
                          htmlLabels: false,
                          theme: isDarkMode ? "dark" : "default",
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
                        targets.forEach((target) => {
                          target.setAttribute("data-text-diagram-render-target", "");
                        });
                        await mermaid.run({ nodes: targets });
                      })
                      .catch(reportError);
                    return state.renderChain;
                  };

                  document.addEventListener("pjax:success", state.scheduleRender);
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

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
            <script defer src="/plugins/text-diagram/assets/static/mermaid.min.js"></script>
            <script>
              (function () {
                const STATE_KEY = "%s";
                const configuration = {
                  darkClassSelector: %s,
                  mermaidSelector: %s,
                };

                function reportError(error) {
                  console.error("Text Diagram Mermaid rendering failed", error);
                }

                function waitForMermaid() {
                  if (window.mermaid && window.mermaid.initialize && window.mermaid.run) {
                    return Promise.resolve(window.mermaid);
                  }

                  return new Promise(function (resolve, reject) {
                    const startedAt = Date.now();

                    function check() {
                      if (window.mermaid && window.mermaid.initialize && window.mermaid.run) {
                        resolve(window.mermaid);
                        return;
                      }

                      if (Date.now() - startedAt > 10000) {
                        reject(new Error("Mermaid library was not loaded within 10 seconds"));
                        return;
                      }

                      window.setTimeout(check, 30);
                    }

                    check();
                  });
                }

                function getTargets() {
                  if (!configuration.mermaidSelector) {
                    return [];
                  }

                  return Array.from(document.querySelectorAll(configuration.mermaidSelector))
                    .filter((element) => !element.hasAttribute("data-processed"));
                }

                function createRendererState() {
                  const state = {
                    renderChain: Promise.resolve(),
                    initialization: null,
                    scheduleRender: null,
                  };

                  state.scheduleRender = function () {
                    state.renderChain = state.renderChain
                      .catch(reportError)
                      .then(async function () {
                        const targets = getTargets();
                        if (targets.length === 0) {
                          return;
                        }

                        targets.forEach((target) => {
                          target.setAttribute("data-text-diagram-render-target", "");
                        });
                        await window.mermaid.run({ nodes: targets });
                      })
                      .catch(reportError);
                    return state.renderChain;
                  };

                  state.initialization = (async function () {
                    if (document.fonts && document.fonts.ready) {
                      await document.fonts.ready;
                    }

                    const mermaid = await waitForMermaid();
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
                    document.addEventListener("pjax:success", state.scheduleRender);
                    await state.scheduleRender();
                  })();

                  return state;
                }

                function startRenderer() {
                  const existingState = window[STATE_KEY];
                  if (existingState) {
                    existingState.initialization
                      .then(existingState.scheduleRender)
                      .catch(reportError);
                    return;
                  }

                  const state = createRendererState();
                  window[STATE_KEY] = state;
                  state.initialization.catch(reportError);
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

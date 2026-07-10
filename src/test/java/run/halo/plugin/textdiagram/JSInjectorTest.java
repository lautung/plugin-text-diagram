package run.halo.plugin.textdiagram;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JSInjectorTest {

    @Test
    void shouldConfigureThemeIsolatedFlowchartRendering() {
        String script = JSInjector.getParsedMermaidScript(
            "html[class~=dark]",
            "text-diagram[data-type=mermaid]"
        );

        assertTrue(script.contains("mermaid.mermaidAPI.reset"));
        assertTrue(script.matches("(?s).*startOnLoad: false,\\s+htmlLabels: false,\\s+theme:.*"));
        assertTrue(script.contains("htmlLabels: false"));
        assertTrue(script.contains("useMaxWidth: false"));
        assertTrue(script.contains("data-text-diagram-render-target"));
        assertTrue(script.contains("data-text-diagram-card"));
        assertTrue(script.contains("data-text-diagram-canvas"));
        assertTrue(script.contains("foreignObject p"));
        assertTrue(script.contains("document.documentElement.classList.contains(\"night\")"));
    }

    @Test
    void shouldRenderInitialAndPjaxContentOnlyOnce() {
        String script = JSInjector.getParsedMermaidScript("html.dark", ".mermaid");

        assertTrue(script.contains("window[STATE_KEY]"));
        assertFalse(script.contains("<script defer src=\"/plugins/text-diagram/assets/static/mermaid.min.js\">"));
        assertTrue(script.contains("if (getTargets().length === 0)"));
        assertTrue(script.contains("document.createElement(\"script\")"));
        assertTrue(script.contains("state.loadPromise"));
        assertTrue(script.contains("state.initialization"));
        assertTrue(script.contains("await Promise.all"));
        assertTrue(script.contains("document.addEventListener(\"pjax:success\", state.scheduleRender)"));
        assertTrue(script.contains("!element.hasAttribute(\"data-processed\")"));
        assertTrue(script.contains("await mermaid.run({ nodes: targets })"));
        assertTrue(script.contains("renderItems.forEach(decorateRenderedTarget)"));
        assertTrue(script.contains("document.addEventListener(\"DOMContentLoaded\", startRenderer, { once: true })"));
    }

    @Test
    void shouldDecorateRenderedMermaidWithGptStyleControls() {
        String script = JSInjector.getParsedMermaidScript("html.dark", ".mermaid");

        assertTrue(script.contains("function decorateRenderedTarget(renderItem)"));
        assertTrue(script.contains("function decorateFailedTarget(renderItem, error)"));
        assertTrue(script.contains("getDiagramSource(target)"));
        assertTrue(script.contains("createAction(\"show-code\""));
        assertTrue(script.contains("createAction(\"show-preview\""));
        assertTrue(script.contains("createAction(\"fullscreen\""));
        assertTrue(script.contains("createAction(\"copy\""));
        assertTrue(script.contains("createDownloadAction(\"↓\", \"下载 SVG\")"));
        assertTrue(script.contains("function updateDownloadAction(card)"));
        assertTrue(script.contains("setCardView(target, \"preview\")"));
        assertTrue(script.contains("data-text-diagram-fullscreen"));
        assertTrue(script.contains("data-text-diagram-error"));
        assertTrue(script.contains("image/svg+xml;charset=utf-8"));
        assertTrue(script.contains("data-text-diagram-object-url"));
        assertTrue(script.contains("URL.createObjectURL"));
        assertTrue(script.contains("URL.revokeObjectURL(previousUrl)"));
        assertTrue(script.contains("navigator.clipboard"));
        assertTrue(script.contains("textarea.focus({ preventScroll: true })"));
        assertTrue(script.contains("textarea.setSelectionRange(0, textarea.value.length)"));
        assertTrue(script.contains("Copy command was not accepted"));
        assertTrue(script.contains("data-text-diagram-copy-state"));
        assertTrue(script.contains("function selectCodePanel(card)"));
        assertTrue(script.contains("range.selectNodeContents(code)"));
        assertEquals(1, script.split("canvas\\.append\\(\\.\\.\\.renderedNodes\\);", -1).length - 1);
    }

    @Test
    void shouldRetryAfterMermaidAssetLoadFailure() {
        String script = JSInjector.getParsedMermaidScript("html.dark", ".mermaid");

        assertTrue(script.contains("script.remove()"));
        assertTrue(script.contains("state.loadPromise = null"));
        assertTrue(script.contains("state.initialization = null"));
        assertTrue(script.contains("script.addEventListener(\"error\", onError)"));
        assertTrue(script.contains("Mermaid library was not loaded within 10 seconds"));
    }

    @Test
    void shouldReuseRendererAndRefreshConfigurationAcrossPjaxNavigation() {
        String script = JSInjector.getParsedMermaidScript("html.dark", ".mermaid");

        assertTrue(script.contains("state.updateConfiguration"));
        assertTrue(script.contains("existingState.updateConfiguration(configuration)"));
        assertTrue(script.contains("data-halo-text-diagram-mermaid"));
    }

    @Test
    void shouldEscapeConfiguredSelectorsBeforeEmbeddingThemInJavaScript() {
        String darkSelector = "html[data-theme=\"dark\"]\nwindow.bad = true;";
        String mermaidSelector = ".diagram[data-label=\"a\\b\"]";

        String script = JSInjector.getParsedMermaidScript(darkSelector, mermaidSelector);

        assertTrue(script.contains("darkClassSelector: \"html[data-theme=\\\"dark\\\"]\\nwindow.bad = true;\""));
        assertTrue(script.contains("mermaidSelector: \".diagram[data-label=\\\"a\\\\b\\\"]\""));
        assertFalse(script.contains("darkClassSelector: html[data-theme"));
    }
}

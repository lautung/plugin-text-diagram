package run.halo.plugin.textdiagram;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertTrue(script.contains("document.addEventListener(\"DOMContentLoaded\", startRenderer, { once: true })"));
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

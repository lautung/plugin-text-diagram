package run.halo.plugin.textdiagram;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JSInjectorTest {

    @Test
    void shouldInjectRuntimeAssetsAndConfiguration() {
        String html = JSInjector.getRuntimeInjection(
            "html[class~=dark]",
            "text-diagram[data-type=mermaid]",
            "scroll"
        );

        assertTrue(html.contains("/plugins/text-diagram/assets/static/text-diagram-runtime.css"));
        assertTrue(html.contains("/plugins/text-diagram/assets/static/text-diagram-runtime.js"));
        assertTrue(html.contains("data-halo-text-diagram-runtime"));
        assertTrue(html.contains("&quot;mobileLayoutMode&quot;:&quot;scroll&quot;"));
        assertFalse(html.contains("function createCardShell"));
    }

    @Test
    void shouldKeepConfiguredValuesInsideTheDataAttribute() {
        String html = JSInjector.getRuntimeInjection(
            "html[data-theme=\"dark\"]\n</script><script>alert(1)</script>",
            ".diagram[data-label=\"a\\b\"]",
            "thumbnail"
        );

        assertFalse(html.contains("</script><script>alert(1)</script>"));
        assertTrue(html.contains("&lt;/script&gt;&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(html.contains("\\&quot;dark\\&quot;"));
        assertTrue(html.contains("a\\\\b"));
    }
}

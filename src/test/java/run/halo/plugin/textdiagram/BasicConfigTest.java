package run.halo.plugin.textdiagram;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BasicConfigTest {

    @Test
    void shouldApplyDefaultsToMissingSettingsOnly() {
        BasicConfig config = new BasicConfig();
        config.setDark_class_selector("  ");
        config.setMermaid_selector(".custom-mermaid");

        BasicConfig result = BasicConfig.withDefaults(config);

        assertEquals(BasicConfig.DEFAULT_DARK_CLASS_SELECTOR, result.getDark_class_selector());
        assertEquals(".custom-mermaid", result.getMermaid_selector());
        assertEquals("svg", result.getMermaid_output_format());
        assertEquals("svg", result.getPlantuml_output_format());
    }

    @Test
    void shouldUseScrollAsDefaultMobileLayoutMode() {
        BasicConfig defaults = BasicConfig.defaults();

        assertEquals("scroll", defaults.getMobile_layout_mode());
    }

    @Test
    void shouldReplaceBlankMobileLayoutMode() {
        BasicConfig config = new BasicConfig();
        config.setMobile_layout_mode("  ");

        assertEquals("scroll", BasicConfig.withDefaults(config).getMobile_layout_mode());
    }

    @Test
    void shouldNormalizeDiagramOutputFormats() {
        BasicConfig config = new BasicConfig();
        config.setMermaid_output_format("PNG");
        config.setPlantuml_output_format("bad");

        BasicConfig result = BasicConfig.withDefaults(config);

        assertEquals("png", result.getMermaid_output_format());
        assertEquals("svg", result.getPlantuml_output_format());
    }
}

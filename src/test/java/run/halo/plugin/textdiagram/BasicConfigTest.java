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
}

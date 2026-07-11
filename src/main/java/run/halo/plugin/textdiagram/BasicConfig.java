package run.halo.plugin.textdiagram;

import lombok.Data;

@Data
public class BasicConfig {
    static final String DEFAULT_DARK_CLASS_SELECTOR = "html[class~=dark]";
    static final String DEFAULT_MERMAID_SELECTOR = "text-diagram[data-type=mermaid]";
    static final String DEFAULT_MOBILE_LAYOUT_MODE = "scroll";

    String dark_class_selector;
    String mermaid_selector;
    String mobile_layout_mode;

    static BasicConfig defaults() {
        return withDefaults(null);
    }

    static BasicConfig withDefaults(BasicConfig config) {
        BasicConfig result = new BasicConfig();
        result.setDark_class_selector(
            hasText(config == null ? null : config.getDark_class_selector())
                ? config.getDark_class_selector()
                : DEFAULT_DARK_CLASS_SELECTOR
        );
        result.setMermaid_selector(
            hasText(config == null ? null : config.getMermaid_selector())
                ? config.getMermaid_selector()
                : DEFAULT_MERMAID_SELECTOR
        );
        result.setMobile_layout_mode(
            hasText(config == null ? null : config.getMobile_layout_mode())
                ? config.getMobile_layout_mode()
                : DEFAULT_MOBILE_LAYOUT_MODE
        );
        return result;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

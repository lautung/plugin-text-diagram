package run.halo.plugin.textdiagram;

import lombok.Data;

@Data
public class BasicConfig {
    static final String DEFAULT_DARK_CLASS_SELECTOR = "html[class~=dark]";
    static final String DEFAULT_MERMAID_SELECTOR = "text-diagram[data-type=mermaid]";
    static final String DEFAULT_MOBILE_LAYOUT_MODE = "scroll";
    static final String DEFAULT_OUTPUT_FORMAT = "svg";

    String dark_class_selector;
    String mermaid_selector;
    String mobile_layout_mode;
    String mermaid_output_format;
    String plantuml_output_format;

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
        result.setMermaid_output_format(
            normalizeOutputFormat(config == null ? null : config.getMermaid_output_format())
        );
        result.setPlantuml_output_format(
            normalizeOutputFormat(config == null ? null : config.getPlantuml_output_format())
        );
        return result;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeOutputFormat(String value) {
        if (!hasText(value)) {
            return DEFAULT_OUTPUT_FORMAT;
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "svg", "png", "webp" -> normalized;
            default -> DEFAULT_OUTPUT_FORMAT;
        };
    }
}

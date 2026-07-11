package run.halo.plugin.textdiagram;

final class JSInjector {
    private static final String RUNTIME_CSS =
        "/plugins/text-diagram/assets/static/text-diagram-runtime.css";
    private static final String RUNTIME_JS =
        "/plugins/text-diagram/assets/static/text-diagram-runtime.js";

    private JSInjector() {
    }

    static String getRuntimeInjection(
        String darkClassSelector,
        String mermaidSelector,
        String mobileLayoutMode
    ) {
        String config = "{" +
            "\"darkClassSelector\":" + toJsonString(darkClassSelector) + "," +
            "\"mermaidSelector\":" + toJsonString(mermaidSelector) + "," +
            "\"mobileLayoutMode\":" + toJsonString(mobileLayoutMode) +
            "}";
        return """
            <link rel="stylesheet" href="%s" data-halo-text-diagram-runtime-style>
            <script defer src="%s" data-halo-text-diagram-runtime data-config="%s"></script>
            """.formatted(RUNTIME_CSS, RUNTIME_JS, encodeHtmlAttribute(config));
    }

    private static String toJsonString(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder result = new StringBuilder(value.length() + 2).append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (character < 0x20) {
                        result.append("\\u%04x".formatted((int) character));
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        return result.append('"').toString();
    }

    private static String encodeHtmlAttribute(String value) {
        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }
}

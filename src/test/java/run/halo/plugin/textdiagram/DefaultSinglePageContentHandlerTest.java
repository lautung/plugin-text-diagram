package run.halo.plugin.textdiagram;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.ReactiveSinglePageContentHandler.SinglePageContentContext;

class DefaultSinglePageContentHandlerTest {

    @Test
    void shouldInjectScriptWhenBasicSettingsAreEmpty() {
        DefaultSinglePageContentHandler handler = new DefaultSinglePageContentHandler(emptySettingFetcher());
        SinglePageContentContext context = SinglePageContentContext.builder()
            .content("<text-diagram data-type=\"mermaid\">graph TD; A-->B</text-diagram>")
            .build();

        SinglePageContentContext result = handler.handle(context).block();

        assertNotNull(result);
        assertTrue(result.getContent().contains("data-halo-text-diagram-runtime-style"));
        assertTrue(result.getContent().contains("data-halo-text-diagram-runtime"));
        assertTrue(result.getContent().contains("text-diagram-runtime.js"));
        assertTrue(result.getContent().contains("&quot;mobileLayoutMode&quot;:&quot;scroll&quot;"));
        assertTrue(result.getContent().contains("text-diagram[data-type=mermaid]"));
        assertTrue(result.getContent().contains("<text-diagram data-type=\"mermaid\">"));
    }

    private static ReactiveSettingFetcher emptySettingFetcher() {
        return new ReactiveSettingFetcher() {
            @Override
            public <T> Mono<T> fetch(String group, Class<T> type) {
                return Mono.empty();
            }

            @Override
            public Mono<com.fasterxml.jackson.databind.JsonNode> get(String group) {
                return Mono.empty();
            }

            @Override
            public Mono<tools.jackson.databind.JsonNode> getSettingValue(String group) {
                return Mono.empty();
            }

            @Override
            public Mono<Map<String, com.fasterxml.jackson.databind.JsonNode>> getValues() {
                return Mono.empty();
            }

            @Override
            public Mono<Map<String, tools.jackson.databind.JsonNode>> getSettingValues() {
                return Mono.empty();
            }
        };
    }
}

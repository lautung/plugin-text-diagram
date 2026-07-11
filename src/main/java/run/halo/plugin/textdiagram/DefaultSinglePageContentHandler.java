package run.halo.plugin.textdiagram;

import com.google.common.base.Throwables;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.ReactiveSinglePageContentHandler;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultSinglePageContentHandler implements ReactiveSinglePageContentHandler {

    private final ReactiveSettingFetcher reactiveSettingFetcher;

    private static void injectJS(SinglePageContentContext contentContext, BasicConfig basicConfig) {
        String parsedScript = JSInjector.getRuntimeInjection(
            basicConfig.getDark_class_selector(),
            basicConfig.getMermaid_selector(),
            basicConfig.getMobile_layout_mode(),
            basicConfig.getMermaid_output_format(),
            basicConfig.getPlantuml_output_format()
        );
        contentContext.setContent(parsedScript + "\n" + contentContext.getContent());
    }

    @Override
    public Mono<SinglePageContentContext> handle(SinglePageContentContext contentContext) {
        return reactiveSettingFetcher.fetch("basic", BasicConfig.class)
            .defaultIfEmpty(BasicConfig.defaults())
            .map(BasicConfig::withDefaults)
            .map(basicConfig -> {
                injectJS(contentContext, basicConfig);
                return contentContext;
            }).onErrorResume(e -> {
                log.error("TextDiagram SinglePageContent handle failed", Throwables.getRootCause(e));
                return Mono.just(contentContext);
            });
    }
}

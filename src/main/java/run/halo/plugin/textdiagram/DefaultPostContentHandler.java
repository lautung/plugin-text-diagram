package run.halo.plugin.textdiagram;

import com.google.common.base.Throwables;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.ReactivePostContentHandler;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultPostContentHandler implements ReactivePostContentHandler {

    private final ReactiveSettingFetcher reactiveSettingFetcher;

    private static void injectJS(PostContentContext contentContext, BasicConfig basicConfig) {
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
    public Mono<PostContentContext> handle(PostContentContext contentContext) {
        return reactiveSettingFetcher.fetch("basic", BasicConfig.class)
            .defaultIfEmpty(BasicConfig.defaults())
            .map(BasicConfig::withDefaults)
            .map(basicConfig -> {
                injectJS(contentContext, basicConfig);
                return contentContext;
            }).onErrorResume(e -> {
                log.error("TextDiagram PostContent handle failed", Throwables.getRootCause(e));
                return Mono.just(contentContext);
            });
    }
}

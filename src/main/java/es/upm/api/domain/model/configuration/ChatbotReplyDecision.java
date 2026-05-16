package es.upm.api.domain.model.configuration;

import es.upm.api.domain.enums.ChatbotResponseMode;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatbotReplyDecision {

    private final String assistantReply;

    private final ChatbotResponseMode responseMode;

    private final boolean usedPlatformData;

    @Builder.Default
    private final List<String> sourcesSummary = List.of();
}

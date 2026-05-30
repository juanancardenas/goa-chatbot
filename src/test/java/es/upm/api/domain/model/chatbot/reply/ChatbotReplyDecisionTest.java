package es.upm.api.domain.model.chatbot.reply;

import es.upm.api.domain.enums.ChatbotResponseMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotReplyDecisionTest {

    @Test
    void builderShouldUseEmptySourcesSummaryByDefault() {
        ChatbotReplyDecision decision = ChatbotReplyDecision.builder()
                .assistantReply("Respuesta")
                .responseMode(ChatbotResponseMode.GENERAL)
                .usedPlatformData(false)
                .build();

        assertThat(decision.getAssistantReply()).isEqualTo("Respuesta");
        assertThat(decision.getResponseMode()).isEqualTo(ChatbotResponseMode.GENERAL);
        assertThat(decision.isUsedAi()).isFalse();
        assertThat(decision.isUsedPlatformData()).isFalse();
        assertThat(decision.getSourcesSummary()).isEmpty();
    }

    @Test
    void builderShouldKeepExplicitSourcesSummary() {
        ChatbotReplyDecision decision = ChatbotReplyDecision.builder()
                .assistantReply("Respuesta contextual")
                .responseMode(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA)
                .usedAi(true)
                .usedPlatformData(true)
                .sourcesSummary(List.of("Hoja de encargo", "Eventos recientes"))
                .build();

        assertThat(decision.getAssistantReply()).isEqualTo("Respuesta contextual");
        assertThat(decision.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(decision.isUsedAi()).isTrue();
        assertThat(decision.isUsedPlatformData()).isTrue();
        assertThat(decision.getSourcesSummary()).containsExactly("Hoja de encargo", "Eventos recientes");
    }
}

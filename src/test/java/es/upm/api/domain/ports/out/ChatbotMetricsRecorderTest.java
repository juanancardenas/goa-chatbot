package es.upm.api.domain.ports.out;

import es.upm.api.domain.enums.ChatbotResponseMode;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.model.metrics.ChatbotAiMetric;
import es.upm.api.domain.model.metrics.ChatbotEscalationMetric;
import es.upm.api.domain.model.metrics.ChatbotFallbackMetric;
import es.upm.api.domain.model.metrics.ChatbotMessageMetric;
import es.upm.api.domain.model.metrics.ChatbotModerationMetric;
import es.upm.api.domain.model.safety.ChatbotModerationAction;
import es.upm.api.domain.model.safety.ChatbotModerationReason;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotMetricsRecorderTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, Month.MAY, 26, 14, 15);

    @Test
    void recorderContractShouldAcceptEveryMetricType() {
        InMemoryChatbotMetricsRecorder recorder = new InMemoryChatbotMetricsRecorder();
        ChatbotMessageMetric messageMetric = ChatbotMessageMetric.builder()
                .conversationId("conversation-1")
                .requestMessageId("message-1")
                .userId("user-1")
                .conversationType(ConversationType.GENERAL)
                .responseMode(ChatbotResponseMode.GENERAL)
                .durationMs(100L)
                .success(true)
                .createdAt(CREATED_AT)
                .build();
        ChatbotAiMetric aiMetric = ChatbotAiMetric.builder()
                .conversationId("conversation-1")
                .provider("openai")
                .model("gpt-test")
                .durationMs(90L)
                .success(true)
                .fallback(false)
                .createdAt(CREATED_AT)
                .build();
        ChatbotEscalationMetric escalationMetric = ChatbotEscalationMetric.builder()
                .conversationId("conversation-1")
                .userId("user-1")
                .success(true)
                .createdAt(CREATED_AT)
                .build();
        ChatbotFallbackMetric fallbackMetric = ChatbotFallbackMetric.builder()
                .conversationId("conversation-1")
                .fallbackType("AI_PROVIDER")
                .reason("Provider error")
                .createdAt(CREATED_AT)
                .build();
        ChatbotModerationMetric moderationMetric = ChatbotModerationMetric.builder()
                .conversationId("conversation-1")
                .userId("user-1")
                .action(ChatbotModerationAction.BLOCK)
                .reason(ChatbotModerationReason.PII_CARD)
                .containsPii(true)
                .blocked(true)
                .usedAi(false)
                .createdAt(CREATED_AT)
                .build();

        recorder.recordMessageHandled(messageMetric);
        recorder.recordAiCall(aiMetric);
        recorder.recordEscalation(escalationMetric);
        recorder.recordFallback(fallbackMetric);
        recorder.recordModeration(moderationMetric);

        assertThat(recorder.messageMetric).isSameAs(messageMetric);
        assertThat(recorder.aiMetric).isSameAs(aiMetric);
        assertThat(recorder.escalationMetric).isSameAs(escalationMetric);
        assertThat(recorder.fallbackMetric).isSameAs(fallbackMetric);
        assertThat(recorder.moderationMetric).isSameAs(moderationMetric);
    }

    private static class InMemoryChatbotMetricsRecorder implements ChatbotMetricsRecorder {

        private ChatbotMessageMetric messageMetric;
        private ChatbotAiMetric aiMetric;
        private ChatbotEscalationMetric escalationMetric;
        private ChatbotFallbackMetric fallbackMetric;
        private ChatbotModerationMetric moderationMetric;

        @Override
        public void recordMessageHandled(ChatbotMessageMetric metric) {
            this.messageMetric = metric;
        }

        @Override
        public void recordAiCall(ChatbotAiMetric metric) {
            this.aiMetric = metric;
        }

        @Override
        public void recordEscalation(ChatbotEscalationMetric metric) {
            this.escalationMetric = metric;
        }

        @Override
        public void recordFallback(ChatbotFallbackMetric metric) {
            this.fallbackMetric = metric;
        }

        @Override
        public void recordModeration(ChatbotModerationMetric metric) {
            this.moderationMetric = metric;
        }
    }
}

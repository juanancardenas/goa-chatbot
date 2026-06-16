package es.upm.api.domain.model.metrics;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotFallbackMetricTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, Month.MAY, 26, 12, 30);

    @Test
    void builderShouldPopulateAllFields() {
        ChatbotFallbackMetric metric = ChatbotFallbackMetric.builder()
                .conversationId("conversation-1")
                .fallbackType("CONTEXT_UNAVAILABLE")
                .reason("No platform context")
                .createdAt(CREATED_AT)
                .build();

        assertThat(metric.getConversationId()).isEqualTo("conversation-1");
        assertThat(metric.getFallbackType()).isEqualTo("CONTEXT_UNAVAILABLE");
        assertThat(metric.getReason()).isEqualTo("No platform context");
        assertThat(metric.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void allArgsConstructorShouldPopulateAllFields() {
        ChatbotFallbackMetric metric = new ChatbotFallbackMetric(
                "conversation-2",
                "AI_PROVIDER",
                "Provider returned blank content",
                CREATED_AT
        );

        assertThat(metric.getConversationId()).isEqualTo("conversation-2");
        assertThat(metric.getFallbackType()).isEqualTo("AI_PROVIDER");
        assertThat(metric.getReason()).isEqualTo("Provider returned blank content");
        assertThat(metric.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void noArgsConstructorAndSettersShouldAllowMutation() {
        ChatbotFallbackMetric metric = new ChatbotFallbackMetric();

        metric.setConversationId("conversation-3");
        metric.setFallbackType("SCOPE_RESTRICTED");
        metric.setReason("Question outside engagement scope");
        metric.setCreatedAt(CREATED_AT);

        assertThat(metric.getConversationId()).isEqualTo("conversation-3");
        assertThat(metric.getFallbackType()).isEqualTo("SCOPE_RESTRICTED");
        assertThat(metric.getReason()).isEqualTo("Question outside engagement scope");
        assertThat(metric.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void dataMethodsShouldUseFieldValues() {
        ChatbotFallbackMetric metric = ChatbotFallbackMetric.builder()
                .conversationId("conversation-1")
                .fallbackType("CONTEXT_UNAVAILABLE")
                .reason("No platform context")
                .createdAt(CREATED_AT)
                .build();
        ChatbotFallbackMetric sameMetric = ChatbotFallbackMetric.builder()
                .conversationId("conversation-1")
                .fallbackType("CONTEXT_UNAVAILABLE")
                .reason("No platform context")
                .createdAt(CREATED_AT)
                .build();
        ChatbotFallbackMetric differentMetric = ChatbotFallbackMetric.builder()
                .conversationId("conversation-1")
                .fallbackType("AI_PROVIDER")
                .reason("No platform context")
                .createdAt(CREATED_AT)
                .build();

        assertThat(metric).isEqualTo(sameMetric);
        assertThat(metric).hasSameHashCodeAs(sameMetric);
        assertThat(metric).isNotEqualTo(differentMetric);
        assertThat(metric.toString())
                .contains("conversation-1")
                .contains("CONTEXT_UNAVAILABLE")
                .contains("No platform context");
    }
}

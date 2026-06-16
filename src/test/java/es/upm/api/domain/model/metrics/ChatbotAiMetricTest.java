package es.upm.api.domain.model.metrics;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotAiMetricTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, Month.MAY, 26, 10, 15);

    @Test
    void builderShouldPopulateAllFields() {
        ChatbotAiMetric metric = ChatbotAiMetric.builder()
                .conversationId("conversation-1")
                .provider("openai")
                .model("gpt-test")
                .durationMs(245L)
                .success(true)
                .fallback(false)
                .errorType(null)
                .createdAt(CREATED_AT)
                .build();

        assertThat(metric.getConversationId()).isEqualTo("conversation-1");
        assertThat(metric.getProvider()).isEqualTo("openai");
        assertThat(metric.getModel()).isEqualTo("gpt-test");
        assertThat(metric.getDurationMs()).isEqualTo(245L);
        assertThat(metric.isSuccess()).isTrue();
        assertThat(metric.isFallback()).isFalse();
        assertThat(metric.getErrorType()).isNull();
        assertThat(metric.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void allArgsConstructorShouldPopulateAllFields() {
        ChatbotAiMetric metric = new ChatbotAiMetric(
                "conversation-2",
                "ollama",
                "llama-test",
                700L,
                false,
                true,
                "TIMEOUT",
                CREATED_AT
        );

        assertThat(metric.getConversationId()).isEqualTo("conversation-2");
        assertThat(metric.getProvider()).isEqualTo("ollama");
        assertThat(metric.getModel()).isEqualTo("llama-test");
        assertThat(metric.getDurationMs()).isEqualTo(700L);
        assertThat(metric.isSuccess()).isFalse();
        assertThat(metric.isFallback()).isTrue();
        assertThat(metric.getErrorType()).isEqualTo("TIMEOUT");
        assertThat(metric.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void noArgsConstructorAndSettersShouldAllowMutation() {
        ChatbotAiMetric metric = new ChatbotAiMetric();

        metric.setConversationId("conversation-3");
        metric.setProvider("gemini");
        metric.setModel("gemini-test");
        metric.setDurationMs(120L);
        metric.setSuccess(true);
        metric.setFallback(true);
        metric.setErrorType("BLANK_CONTENT");
        metric.setCreatedAt(CREATED_AT);

        assertThat(metric.getConversationId()).isEqualTo("conversation-3");
        assertThat(metric.getProvider()).isEqualTo("gemini");
        assertThat(metric.getModel()).isEqualTo("gemini-test");
        assertThat(metric.getDurationMs()).isEqualTo(120L);
        assertThat(metric.isSuccess()).isTrue();
        assertThat(metric.isFallback()).isTrue();
        assertThat(metric.getErrorType()).isEqualTo("BLANK_CONTENT");
        assertThat(metric.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void dataMethodsShouldUseFieldValues() {
        ChatbotAiMetric metric = ChatbotAiMetric.builder()
                .conversationId("conversation-1")
                .provider("openai")
                .model("gpt-test")
                .durationMs(245L)
                .success(true)
                .fallback(false)
                .createdAt(CREATED_AT)
                .build();
        ChatbotAiMetric sameMetric = ChatbotAiMetric.builder()
                .conversationId("conversation-1")
                .provider("openai")
                .model("gpt-test")
                .durationMs(245L)
                .success(true)
                .fallback(false)
                .createdAt(CREATED_AT)
                .build();
        ChatbotAiMetric differentMetric = ChatbotAiMetric.builder()
                .conversationId("conversation-1")
                .provider("openai")
                .model("gpt-test")
                .durationMs(246L)
                .success(true)
                .fallback(false)
                .createdAt(CREATED_AT)
                .build();

        assertThat(metric).isEqualTo(sameMetric);
        assertThat(metric).hasSameHashCodeAs(sameMetric);
        assertThat(metric).isNotEqualTo(differentMetric);
        assertThat(metric.toString())
                .contains("conversation-1")
                .contains("openai")
                .contains("gpt-test")
                .contains("245");
    }
}

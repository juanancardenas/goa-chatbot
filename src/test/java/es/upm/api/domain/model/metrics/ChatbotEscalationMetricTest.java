package es.upm.api.domain.model.metrics;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotEscalationMetricTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, Month.MAY, 26, 11, 0);

    @Test
    void builderShouldPopulateAllFields() {
        ChatbotEscalationMetric metric = ChatbotEscalationMetric.builder()
                .conversationId("conversation-1")
                .userId("user-1")
                .success(true)
                .errorType(null)
                .createdAt(CREATED_AT)
                .build();

        assertThat(metric.getConversationId()).isEqualTo("conversation-1");
        assertThat(metric.getUserId()).isEqualTo("user-1");
        assertThat(metric.isSuccess()).isTrue();
        assertThat(metric.getErrorType()).isNull();
        assertThat(metric.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void allArgsConstructorShouldPopulateAllFields() {
        ChatbotEscalationMetric metric = new ChatbotEscalationMetric(
                "conversation-2",
                "user-2",
                false,
                "CONTACT_UNAVAILABLE",
                CREATED_AT
        );

        assertThat(metric.getConversationId()).isEqualTo("conversation-2");
        assertThat(metric.getUserId()).isEqualTo("user-2");
        assertThat(metric.isSuccess()).isFalse();
        assertThat(metric.getErrorType()).isEqualTo("CONTACT_UNAVAILABLE");
        assertThat(metric.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void noArgsConstructorAndSettersShouldAllowMutation() {
        ChatbotEscalationMetric metric = new ChatbotEscalationMetric();

        metric.setConversationId("conversation-3");
        metric.setUserId("user-3");
        metric.setSuccess(false);
        metric.setErrorType("ARCHIVE_FAILED");
        metric.setCreatedAt(CREATED_AT);

        assertThat(metric.getConversationId()).isEqualTo("conversation-3");
        assertThat(metric.getUserId()).isEqualTo("user-3");
        assertThat(metric.isSuccess()).isFalse();
        assertThat(metric.getErrorType()).isEqualTo("ARCHIVE_FAILED");
        assertThat(metric.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void dataMethodsShouldUseFieldValues() {
        ChatbotEscalationMetric metric = ChatbotEscalationMetric.builder()
                .conversationId("conversation-1")
                .userId("user-1")
                .success(true)
                .createdAt(CREATED_AT)
                .build();
        ChatbotEscalationMetric sameMetric = ChatbotEscalationMetric.builder()
                .conversationId("conversation-1")
                .userId("user-1")
                .success(true)
                .createdAt(CREATED_AT)
                .build();
        ChatbotEscalationMetric differentMetric = ChatbotEscalationMetric.builder()
                .conversationId("conversation-1")
                .userId("user-2")
                .success(true)
                .createdAt(CREATED_AT)
                .build();

        assertThat(metric)
                .isEqualTo(sameMetric)
                .hasSameHashCodeAs(sameMetric)
                .isNotEqualTo(differentMetric);
        assertThat(metric.toString())
                .contains("conversation-1")
                .contains("user-1")
                .contains("success=true");
    }
}

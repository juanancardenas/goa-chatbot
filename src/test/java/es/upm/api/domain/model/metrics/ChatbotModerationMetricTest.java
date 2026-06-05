package es.upm.api.domain.model.metrics;

import es.upm.api.domain.model.safety.ChatbotModerationAction;
import es.upm.api.domain.model.safety.ChatbotModerationReason;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotModerationMetricTest {
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 6, 5, 12, 30);

    @Test
    void builderShouldPopulateAllFields() {
        ChatbotModerationMetric metric = ChatbotModerationMetric.builder()
                .conversationId("conversation-1")
                .userId("user-1")
                .action(ChatbotModerationAction.BLOCK)
                .reason(ChatbotModerationReason.PII_CARD)
                .containsPii(true)
                .blocked(true)
                .usedAi(false)
                .createdAt(CREATED_AT)
                .build();

        assertThat(metric.getConversationId()).isEqualTo("conversation-1");
        assertThat(metric.getUserId()).isEqualTo("user-1");
        assertThat(metric.getAction()).isEqualTo(ChatbotModerationAction.BLOCK);
        assertThat(metric.getReason()).isEqualTo(ChatbotModerationReason.PII_CARD);
        assertThat(metric.isContainsPii()).isTrue();
        assertThat(metric.isBlocked()).isTrue();
        assertThat(metric.getUsedAi()).isFalse();
        assertThat(metric.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void allArgsConstructorShouldPopulateAllFields() {
        ChatbotModerationMetric metric = new ChatbotModerationMetric(
                "conversation-2",
                "user-2",
                ChatbotModerationAction.WARN,
                ChatbotModerationReason.PII_EMAIL,
                true,
                false,
                null,
                CREATED_AT
        );

        assertThat(metric.getConversationId()).isEqualTo("conversation-2");
        assertThat(metric.getUserId()).isEqualTo("user-2");
        assertThat(metric.getAction()).isEqualTo(ChatbotModerationAction.WARN);
        assertThat(metric.getReason()).isEqualTo(ChatbotModerationReason.PII_EMAIL);
        assertThat(metric.isContainsPii()).isTrue();
        assertThat(metric.isBlocked()).isFalse();
        assertThat(metric.getUsedAi()).isNull();
        assertThat(metric.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void settersShouldUpdateFields() {
        ChatbotModerationMetric metric = new ChatbotModerationMetric();

        metric.setConversationId("conversation-3");
        metric.setUserId("user-3");
        metric.setAction(ChatbotModerationAction.ALLOW);
        metric.setReason(ChatbotModerationReason.NONE);
        metric.setContainsPii(false);
        metric.setBlocked(false);
        metric.setUsedAi(null);
        metric.setCreatedAt(CREATED_AT);

        assertThat(metric.getConversationId()).isEqualTo("conversation-3");
        assertThat(metric.getUserId()).isEqualTo("user-3");
        assertThat(metric.getAction()).isEqualTo(ChatbotModerationAction.ALLOW);
        assertThat(metric.getReason()).isEqualTo(ChatbotModerationReason.NONE);
        assertThat(metric.isContainsPii()).isFalse();
        assertThat(metric.isBlocked()).isFalse();
        assertThat(metric.getUsedAi()).isNull();
        assertThat(metric.getCreatedAt()).isEqualTo(CREATED_AT);
    }
}

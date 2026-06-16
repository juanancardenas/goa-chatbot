package es.upm.api.domain.model.metrics;

import es.upm.api.domain.enums.ChatbotResponseMode;
import es.upm.api.domain.enums.ConversationType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotMessageMetricTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, Month.MAY, 26, 13, 45);

    @Test
    void builderShouldPopulateAllFields() {
        ChatbotMessageMetric metric = ChatbotMessageMetric.builder()
                .conversationId("conversation-1")
                .requestMessageId("message-1")
                .userId("user-1")
                .conversationType(ConversationType.CONTEXTUAL)
                .responseMode(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA)
                .usedAi(true)
                .usedPlatformData(true)
                .durationMs(450L)
                .success(true)
                .createdAt(CREATED_AT)
                .build();

        assertThat(metric.getConversationId()).isEqualTo("conversation-1");
        assertThat(metric.getRequestMessageId()).isEqualTo("message-1");
        assertThat(metric.getUserId()).isEqualTo("user-1");
        assertThat(metric.getConversationType()).isEqualTo(ConversationType.CONTEXTUAL);
        assertThat(metric.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(metric.isUsedAi()).isTrue();
        assertThat(metric.isUsedPlatformData()).isTrue();
        assertThat(metric.getDurationMs()).isEqualTo(450L);
        assertThat(metric.isSuccess()).isTrue();
        assertThat(metric.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void allArgsConstructorShouldPopulateAllFields() {
        ChatbotMessageMetric metric = new ChatbotMessageMetric(
                "conversation-2",
                "message-2",
                "user-2",
                ConversationType.GENERAL,
                ChatbotResponseMode.GENERAL,
                false,
                false,
                80L,
                false,
                CREATED_AT
        );

        assertThat(metric.getConversationId()).isEqualTo("conversation-2");
        assertThat(metric.getRequestMessageId()).isEqualTo("message-2");
        assertThat(metric.getUserId()).isEqualTo("user-2");
        assertThat(metric.getConversationType()).isEqualTo(ConversationType.GENERAL);
        assertThat(metric.getResponseMode()).isEqualTo(ChatbotResponseMode.GENERAL);
        assertThat(metric.isUsedAi()).isFalse();
        assertThat(metric.isUsedPlatformData()).isFalse();
        assertThat(metric.getDurationMs()).isEqualTo(80L);
        assertThat(metric.isSuccess()).isFalse();
        assertThat(metric.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void noArgsConstructorAndSettersShouldAllowMutation() {
        ChatbotMessageMetric metric = new ChatbotMessageMetric();

        metric.setConversationId("conversation-3");
        metric.setRequestMessageId("message-3");
        metric.setUserId("user-3");
        metric.setConversationType(ConversationType.CONTEXTUAL);
        metric.setResponseMode(ChatbotResponseMode.CONTEXTUAL_RESTRICTED);
        metric.setUsedAi(false);
        metric.setUsedPlatformData(false);
        metric.setDurationMs(120L);
        metric.setSuccess(false);
        metric.setCreatedAt(CREATED_AT);

        assertThat(metric.getConversationId()).isEqualTo("conversation-3");
        assertThat(metric.getRequestMessageId()).isEqualTo("message-3");
        assertThat(metric.getUserId()).isEqualTo("user-3");
        assertThat(metric.getConversationType()).isEqualTo(ConversationType.CONTEXTUAL);
        assertThat(metric.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_RESTRICTED);
        assertThat(metric.isUsedAi()).isFalse();
        assertThat(metric.isUsedPlatformData()).isFalse();
        assertThat(metric.getDurationMs()).isEqualTo(120L);
        assertThat(metric.isSuccess()).isFalse();
        assertThat(metric.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void dataMethodsShouldUseFieldValues() {
        ChatbotMessageMetric metric = ChatbotMessageMetric.builder()
                .conversationId("conversation-1")
                .requestMessageId("message-1")
                .userId("user-1")
                .conversationType(ConversationType.CONTEXTUAL)
                .responseMode(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA)
                .usedAi(true)
                .usedPlatformData(true)
                .durationMs(450L)
                .success(true)
                .createdAt(CREATED_AT)
                .build();
        ChatbotMessageMetric sameMetric = ChatbotMessageMetric.builder()
                .conversationId("conversation-1")
                .requestMessageId("message-1")
                .userId("user-1")
                .conversationType(ConversationType.CONTEXTUAL)
                .responseMode(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA)
                .usedAi(true)
                .usedPlatformData(true)
                .durationMs(450L)
                .success(true)
                .createdAt(CREATED_AT)
                .build();
        ChatbotMessageMetric differentMetric = ChatbotMessageMetric.builder()
                .conversationId("conversation-1")
                .requestMessageId("message-1")
                .userId("user-1")
                .conversationType(ConversationType.GENERAL)
                .responseMode(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA)
                .usedAi(true)
                .usedPlatformData(true)
                .durationMs(450L)
                .success(true)
                .createdAt(CREATED_AT)
                .build();

        assertThat(metric).isEqualTo(sameMetric);
        assertThat(metric).hasSameHashCodeAs(sameMetric);
        assertThat(metric).isNotEqualTo(differentMetric);
        assertThat(metric.toString())
                .contains("conversation-1")
                .contains("message-1")
                .contains("CONTEXTUAL")
                .contains("CONTEXTUAL_PLATFORM_DATA");
    }
}

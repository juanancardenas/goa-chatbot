package es.upm.api.domain.model.metrics;

import es.upm.api.domain.enums.ChatbotResponseMode;
import es.upm.api.domain.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ChatbotMessageMetric {
    private String conversationId;
    private String requestMessageId;
    private String userId;
    private ConversationType conversationType;
    private ChatbotResponseMode responseMode;
    private boolean usedAi;
    private boolean usedPlatformData;
    private long durationMs;
    private boolean success;
    private LocalDateTime createdAt;
}

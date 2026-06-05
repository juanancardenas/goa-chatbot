package es.upm.api.domain.model.metrics;

import es.upm.api.domain.model.safety.ChatbotModerationAction;
import es.upm.api.domain.model.safety.ChatbotModerationReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ChatbotModerationMetric {
    private String conversationId;
    private String userId;
    private ChatbotModerationAction action;
    private ChatbotModerationReason reason;
    private boolean containsPii;
    private boolean blocked;
    private Boolean usedAi;
    private LocalDateTime createdAt;
}

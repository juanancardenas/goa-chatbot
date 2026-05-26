package es.upm.api.domain.model.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ChatbotEscalationMetric {
    private String conversationId;
    private String userId;
    private boolean success;
    private String errorType;
    private LocalDateTime createdAt;
}

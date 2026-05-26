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
public class ChatbotAiMetric {
    private String conversationId;
    private String provider;
    private String model;
    private long durationMs;
    private boolean success;
    private boolean fallback;
    private String errorType;
    private LocalDateTime createdAt;
}

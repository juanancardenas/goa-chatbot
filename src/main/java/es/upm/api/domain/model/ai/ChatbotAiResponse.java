package es.upm.api.domain.model.ai;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ChatbotAiResponse {
    private String content;
    private String provider;
    private String model;
    private String finishReason;
    private Integer usedTokens;
    private String error;
}

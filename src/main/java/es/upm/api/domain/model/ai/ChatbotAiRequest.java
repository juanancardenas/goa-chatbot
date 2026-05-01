package es.upm.api.domain.model.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class ChatbotAiRequest {
    private String conversationId;
    private String userId;
    private String userMessage;
    private String basePrompt;
    private String roleProfile;
    private String conversationType;
    private String platformContext;
    private List<String> recentMessages;
    private String model;
    private Integer maxOutputTokens;
    private Double temperature;
    private Boolean documentsAvailable;
}

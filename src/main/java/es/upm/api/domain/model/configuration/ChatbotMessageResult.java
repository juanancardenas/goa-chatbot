package es.upm.api.domain.model.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotMessageResult {

    private String conversationId;
    private String message;
    private String error;
    private String createdAt;
    private String responseMode;
    private Boolean usedPlatformData;
    private List<String> sourcesSummary;
}

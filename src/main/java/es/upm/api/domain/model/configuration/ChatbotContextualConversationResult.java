package es.upm.api.domain.model.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotContextualConversationResult {

    private String conversationId;
    private String engagementLetterId;
    private String createdAt;
    private String error;
}

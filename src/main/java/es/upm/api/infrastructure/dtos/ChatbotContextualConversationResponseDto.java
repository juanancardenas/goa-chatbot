package es.upm.api.infrastructure.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
public class ChatbotContextualConversationResponseDto {

    private String conversationId;
    private String engagementLetterId;
    private String createdAt;
    private String error;

    public ChatbotContextualConversationResponseDto(
            String conversationId,
            String engagementLetterId,
            String createdAt,
            String error
    ) {
        this.conversationId = conversationId;
        this.engagementLetterId = engagementLetterId;
        this.createdAt = createdAt;
        this.error = error;
    }

}

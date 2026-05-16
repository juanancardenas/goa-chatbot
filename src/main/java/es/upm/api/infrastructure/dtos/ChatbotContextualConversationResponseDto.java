package es.upm.api.infrastructure.dtos;

import es.upm.api.domain.model.chatbot.result.ChatbotContextualConversationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotContextualConversationResponseDto {

    private String conversationId;
    private String engagementLetterId;
    private String createdAt;
    private String error;

    public static ChatbotContextualConversationResponseDto fromDomain(
            ChatbotContextualConversationResult result
    ) {
        return ChatbotContextualConversationResponseDto.builder()
                .conversationId(result.getConversationId())
                .engagementLetterId(result.getEngagementLetterId())
                .createdAt(result.getCreatedAt())
                .error(result.getError())
                .build();
    }
}

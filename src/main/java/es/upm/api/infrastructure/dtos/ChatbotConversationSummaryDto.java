package es.upm.api.infrastructure.dtos;

import es.upm.api.domain.model.configuration.ChatbotConversationSummaryResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotConversationSummaryDto {

    private String conversationId;
    private String type;
    private String status;
    private String engagementLetterId;
    private String createdAt;
    private String lastMessageAt;
    private String preview;

    public static ChatbotConversationSummaryDto fromDomain(ChatbotConversationSummaryResult result) {
        return ChatbotConversationSummaryDto.builder()
                .conversationId(result.getConversationId())
                .type(result.getType())
                .status(result.getStatus())
                .engagementLetterId(result.getEngagementLetterId())
                .createdAt(result.getCreatedAt())
                .lastMessageAt(result.getLastMessageAt())
                .preview(result.getPreview())
                .build();
    }
}

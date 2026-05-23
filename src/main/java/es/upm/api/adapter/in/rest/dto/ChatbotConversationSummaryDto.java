package es.upm.api.adapter.in.rest.dto;

import es.upm.api.domain.model.chatbot.result.ChatbotConversationSummaryResult;
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

package es.upm.api.infrastructure.dtos;

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
}
package es.upm.api.infrastructure.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotConversationResponseDto {

    private String conversationId;
    private String userId;
    private String engagementLetterId;
    private String status;
    private String type;
    private String createdAt;

}

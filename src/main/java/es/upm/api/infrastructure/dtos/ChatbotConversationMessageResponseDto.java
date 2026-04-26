package es.upm.api.infrastructure.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotConversationMessageResponseDto {

    private String messageId;
    private String conversationId;
    private String senderType;
    private String messageType;
    private String content;
    private String timestamp;
    private Integer sequenceNumber;
    private String parentMessageId;

}

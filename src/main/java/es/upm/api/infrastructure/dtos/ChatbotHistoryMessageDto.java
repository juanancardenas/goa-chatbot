package es.upm.api.infrastructure.dtos;

import es.upm.api.domain.model.configuration.ChatbotHistoryMessageResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotHistoryMessageDto {

    private String id;
    private String conversationId;
    private String senderType;
    private String messageType;
    private String content;
    private String timestamp;
    private Integer sequenceNumber;
    private String parentMessageId;

    public static ChatbotHistoryMessageDto fromDomain(ChatbotHistoryMessageResult result) {
        return ChatbotHistoryMessageDto.builder()
                .id(result.getId())
                .conversationId(result.getConversationId())
                .senderType(result.getSenderType())
                .messageType(result.getMessageType())
                .content(result.getContent())
                .timestamp(result.getTimestamp())
                .sequenceNumber(result.getSequenceNumber())
                .parentMessageId(result.getParentMessageId())
                .build();
    }
}

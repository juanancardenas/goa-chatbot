package es.upm.api.infrastructure.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotConversationHistoryResponseDto {
    private String conversationId;
    private String engagementLetterId;
    private String type;
    private String status;
    private Integer page;
    private Integer size;
    private Boolean hasMore;
    private Long totalMessages;
    private List<ChatbotHistoryMessageDto> messages;
}
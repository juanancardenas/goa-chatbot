package es.upm.api.adapter.in.rest.dto;

import es.upm.api.domain.model.chatbot.result.ChatbotConversationHistoryResult;
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

    public static ChatbotConversationHistoryResponseDto fromDomain(ChatbotConversationHistoryResult result) {
        return ChatbotConversationHistoryResponseDto.builder()
                .conversationId(result.getConversationId())
                .engagementLetterId(result.getEngagementLetterId())
                .type(result.getType())
                .status(result.getStatus())
                .page(result.getPage())
                .size(result.getSize())
                .hasMore(result.getHasMore())
                .totalMessages(result.getTotalMessages())
                .messages(
                        result.getMessages()
                                .stream()
                                .map(ChatbotHistoryMessageDto::fromDomain)
                                .toList()
                )
                .build();
    }
}

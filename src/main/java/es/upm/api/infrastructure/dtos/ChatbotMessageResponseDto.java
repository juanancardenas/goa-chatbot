package es.upm.api.infrastructure.dtos;

import es.upm.api.domain.model.configuration.ChatbotMessageResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotMessageResponseDto {

    private String conversationId;
    private String message;
    private String error;
    private String createdAt;
    private String responseMode;
    private Boolean usedPlatformData;
    private List<String> sourcesSummary;

    public static ChatbotMessageResponseDto fromDomain(ChatbotMessageResult result) {
        return ChatbotMessageResponseDto.builder()
                .conversationId(result.getConversationId())
                .message(result.getMessage())
                .error(result.getError())
                .createdAt(result.getCreatedAt())
                .responseMode(result.getResponseMode())
                .usedPlatformData(result.getUsedPlatformData())
                .sourcesSummary(result.getSourcesSummary())
                .build();
    }
}

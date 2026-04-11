package es.upm.api.resources.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
public class ChatbotMessageResponseDto {

    private String conversationId;
    private String message;
    private String error;
    private String createdAt;

    public ChatbotMessageResponseDto(String conversationId, String message, String error, String createdAt) {
        this.conversationId = conversationId;
        this.message = message;
        this.error = error;
        this.createdAt = createdAt;
    }

}
package es.upm.api.resources.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotMessageResponseDto {
    private String conversationId;
    private String message;
    private String error;
    private String createdAt;
}

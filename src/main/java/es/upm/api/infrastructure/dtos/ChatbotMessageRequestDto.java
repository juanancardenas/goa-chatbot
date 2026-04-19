package es.upm.api.infrastructure.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
public class ChatbotMessageRequestDto {

    private String conversationId;

    @NotBlank
    private String message;

    public ChatbotMessageRequestDto(String conversationId, String message) {
        this.conversationId = conversationId;
        this.message = message;
    }

}
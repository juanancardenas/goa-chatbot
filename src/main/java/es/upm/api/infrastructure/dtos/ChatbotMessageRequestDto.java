package es.upm.api.infrastructure.dtos;

import es.upm.api.domain.model.chatbot.command.ChatbotMessageCommand;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotMessageRequestDto {

    private String conversationId;

    @NotBlank
    private String message;

    public ChatbotMessageCommand toCommand() {
        return ChatbotMessageCommand.builder()
                .conversationId(this.conversationId)
                .message(this.message)
                .build();
    }
}

package es.upm.api.adapter.in.rest.dto;

import es.upm.api.domain.model.chatbot.command.ChatbotContextualConversationCommand;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatbotContextualConversationRequestDto {

    @NotBlank(message = "engagementLetterId es obligatorio")
    private String engagementLetterId;

    public void setEngagementLetterId(String engagementLetterId) {
        this.engagementLetterId = engagementLetterId != null && engagementLetterId.isBlank()
                ? null
                : engagementLetterId;
    }

    public ChatbotContextualConversationCommand toCommand() {
        return ChatbotContextualConversationCommand.builder()
                .engagementLetterId(this.engagementLetterId)
                .build();
    }
}

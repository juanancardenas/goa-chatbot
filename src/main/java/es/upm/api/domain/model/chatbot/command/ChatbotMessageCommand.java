package es.upm.api.domain.model.chatbot.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotMessageCommand {

    private String conversationId;
    private String message;
}
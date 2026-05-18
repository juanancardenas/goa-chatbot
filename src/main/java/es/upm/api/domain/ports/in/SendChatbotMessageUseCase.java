package es.upm.api.domain.ports.in;

import es.upm.api.domain.model.chatbot.command.ChatbotMessageCommand;
import es.upm.api.domain.model.chatbot.result.ChatbotMessageResult;
import es.upm.api.domain.model.security.AuthenticatedUserContext;

public interface SendChatbotMessageUseCase {

    ChatbotMessageResult sendMessage(
            AuthenticatedUserContext authenticatedUser,
            ChatbotMessageCommand command
    );
}

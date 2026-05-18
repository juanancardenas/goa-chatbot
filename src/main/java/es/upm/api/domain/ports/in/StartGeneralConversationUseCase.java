package es.upm.api.domain.ports.in;

import es.upm.api.domain.model.chatbot.command.ChatbotMessageCommand;
import es.upm.api.domain.model.chatbot.result.ChatbotMessageResult;
import es.upm.api.domain.model.security.AuthenticatedUserContext;

public interface StartGeneralConversationUseCase {

    ChatbotMessageResult startGeneralConversation(
            AuthenticatedUserContext authenticatedUser,
            ChatbotMessageCommand command
    );
}

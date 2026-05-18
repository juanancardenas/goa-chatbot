package es.upm.api.domain.ports.in;

import es.upm.api.domain.model.chatbot.command.ChatbotContextualConversationCommand;
import es.upm.api.domain.model.chatbot.result.ChatbotContextualConversationResult;
import es.upm.api.domain.model.security.AuthenticatedUserContext;

public interface StartContextualConversationUseCase {

    ChatbotContextualConversationResult startContextualConversation(
            AuthenticatedUserContext authenticatedUser,
            ChatbotContextualConversationCommand command
    );
}

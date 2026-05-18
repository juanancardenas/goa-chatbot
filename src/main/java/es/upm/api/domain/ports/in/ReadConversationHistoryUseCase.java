package es.upm.api.domain.ports.in;

import es.upm.api.domain.model.chatbot.result.ChatbotConversationHistoryResult;
import es.upm.api.domain.model.security.AuthenticatedUserContext;

public interface ReadConversationHistoryUseCase {

    ChatbotConversationHistoryResult readConversationHistory(
            AuthenticatedUserContext authenticatedUser,
            String conversationId,
            Integer page,
            Integer size
    );
}

package es.upm.api.domain.ports.in;

import es.upm.api.domain.model.security.AuthenticatedUserContext;

public interface ReopenConversationUseCase {

    void reopenConversation(
            AuthenticatedUserContext authenticatedUser,
            String conversationId
    );
}

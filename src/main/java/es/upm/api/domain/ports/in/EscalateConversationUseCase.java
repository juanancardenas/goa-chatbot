package es.upm.api.domain.ports.in;

import es.upm.api.domain.model.security.AuthenticatedUserContext;

public interface EscalateConversationUseCase {

    void escalateConversation(
            AuthenticatedUserContext authenticatedUser,
            String conversationId
    );
}

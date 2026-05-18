package es.upm.api.domain.ports.in;

import es.upm.api.domain.model.security.AuthenticatedUserContext;

public interface DeleteConversationUseCase {

    void deleteConversation(
            AuthenticatedUserContext authenticatedUser,
            String conversationId
    );
}

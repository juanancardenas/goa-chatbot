package es.upm.api.domain.ports.in;

import es.upm.api.domain.model.security.AuthenticatedUserContext;

public interface CloseConversationUseCase {

    void closeConversation(
            AuthenticatedUserContext authenticatedUser,
            String conversationId
    );
}

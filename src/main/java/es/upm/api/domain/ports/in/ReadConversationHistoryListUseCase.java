package es.upm.api.domain.ports.in;

import es.upm.api.domain.model.chatbot.result.ChatbotConversationSummaryResult;
import es.upm.api.domain.model.security.AuthenticatedUserContext;

import java.util.List;

public interface ReadConversationHistoryListUseCase {

    List<ChatbotConversationSummaryResult> readConversationHistoryList(
            AuthenticatedUserContext authenticatedUser,
            String type,
            String engagementLetterId
    );
}

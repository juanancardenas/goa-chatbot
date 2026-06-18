package es.upm.api.domain.services.reply.context;

import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.platform.ChatbotDocumentContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatbotDocumentContextService {
    @SuppressWarnings("UnusedParameters")
    public ChatbotDocumentContext loadDocumentContext(Conversation conversation) {
        return ChatbotDocumentContext.builder()
                .available(false)
                .authorizedSourceConfigured(false)
                .visibleDocumentTitles(List.of())
                .sourcesSummary(List.of())
                .build();
    }
}

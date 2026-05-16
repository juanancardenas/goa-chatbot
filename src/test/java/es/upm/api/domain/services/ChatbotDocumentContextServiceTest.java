package es.upm.api.domain.services;

import es.upm.api.domain.enums.ConversationType;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.services.basereply.ChatbotDocumentContextService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotDocumentContextServiceTest {

    @Test
    void loadDocumentContextShouldReturnStubContextWithNoSources() {
        ChatbotDocumentContextService service = new ChatbotDocumentContextService();
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .userId("user-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, 4, 30, 11, 0))
                .build();

        var context = service.loadDocumentContext(conversation);

        assertThat(context.isAvailable()).isFalse();
        assertThat(context.isAuthorizedSourceConfigured()).isFalse();
        assertThat(context.getVisibleDocumentTitles()).isEmpty();
        assertThat(context.getSourcesSummary()).isEmpty();
    }
}

package es.upm.api.domain.services.conversation;

import es.upm.api.domain.enums.ConversationType;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.exceptions.ConflictException;
import es.upm.api.domain.exceptions.ForbiddenException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.ports.out.ConversationGateway;
import es.upm.api.domain.ports.out.MessageGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotConversationServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T10:00:00Z");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 1, 10, 0);

    @Mock
    private ConversationGateway conversationGateway;

    @Mock
    private MessageGateway messageGateway;

    private ChatbotConversationService chatbotConversationService;

    @BeforeEach
    void setUp() {
        this.chatbotConversationService = new ChatbotConversationService(
                this.conversationGateway,
                this.messageGateway,
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)
        );
    }

    @Test
    void createGeneralConversationShouldPersistActiveGeneralConversation() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 11, 0);

        Conversation conversation = this.chatbotConversationService.createGeneralConversation("user-1", createdAt);

        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(this.conversationGateway).create(conversationCaptor.capture());
        Conversation savedConversation = conversationCaptor.getValue();

        assertThat(conversation.getId()).isNotBlank();
        assertThat(conversation.getUserId()).isEqualTo("user-1");
        assertThat(conversation.getType()).isEqualTo(ConversationType.GENERAL);
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(conversation.getCreatedAt()).isEqualTo(createdAt);
        assertThat(savedConversation).usingRecursiveComparison().isEqualTo(conversation);
    }

    @Test
    void findOrCreateContextualConversationShouldReuseExistingActiveConversation() {
        Conversation existingConversation = Conversation.builder()
                .id("conversation-1")
                .userId("user-1")
                .engagementLetterId("EL-7")
                .type(ConversationType.CONTEXTUAL)
                .status(ConversationStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 5, 15, 10, 0))
                .build();
        when(this.conversationGateway.findActiveContextualConversation("user-1", "EL-7", ConversationType.CONTEXTUAL))
                .thenReturn(Optional.of(existingConversation));

        Conversation conversation = this.chatbotConversationService.findOrCreateContextualConversation("user-1", "EL-7");

        assertThat(conversation).isSameAs(existingConversation);
        verify(this.conversationGateway, never()).create(any(Conversation.class));
    }

    @Test
    void findOrCreateContextualConversationShouldCreateConversationWhenNoneExists() {
        when(this.conversationGateway.findActiveContextualConversation("user-1", "EL-8", ConversationType.CONTEXTUAL))
                .thenReturn(Optional.empty());

        Conversation conversation = this.chatbotConversationService.findOrCreateContextualConversation("user-1", "EL-8");

        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(this.conversationGateway).create(conversationCaptor.capture());
        Conversation savedConversation = conversationCaptor.getValue();

        assertThat(conversation.getId()).isNotBlank();
        assertThat(conversation.getUserId()).isEqualTo("user-1");
        assertThat(conversation.getEngagementLetterId()).isEqualTo("EL-8");
        assertThat(conversation.getType()).isEqualTo(ConversationType.CONTEXTUAL);
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(conversation.getCreatedAt()).isEqualTo(FIXED_NOW);
        assertThat(savedConversation).usingRecursiveComparison().isEqualTo(conversation);
    }

    @Test
    void requireOwnedConversationShouldReturnConversationWhenUserOwnsIt() {
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .userId("user-1")
                .build();
        when(this.conversationGateway.readById("conversation-1")).thenReturn(conversation);

        Conversation ownedConversation = this.chatbotConversationService.requireOwnedConversation("conversation-1", "user-1");

        assertThat(ownedConversation).isSameAs(conversation);
    }

    @Test
    void requireOwnedConversationShouldRejectOtherUsersConversation() {
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .userId("user-2")
                .build();
        when(this.conversationGateway.readById("conversation-1")).thenReturn(conversation);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> this.chatbotConversationService.requireOwnedConversation("conversation-1", "user-1")
        );

        assertThat(exception).hasMessageContaining("No tienes permisos sobre esta conversacion");
    }

    @Test
    void requireActiveOwnedConversationShouldRejectNonActiveConversation() {
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .userId("user-1")
                .status(ConversationStatus.CLOSED)
                .build();
        when(this.conversationGateway.readById("conversation-1")).thenReturn(conversation);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> this.chatbotConversationService.requireActiveOwnedConversation("conversation-1", "user-1")
        );

        assertThat(exception).hasMessageContaining("La conversacion no esta activa");
    }

    @Test
    void closeConversationShouldUpdateActiveConversationToClosed() {
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .userId("user-1")
                .status(ConversationStatus.ACTIVE)
                .build();
        when(this.conversationGateway.readById("conversation-1")).thenReturn(conversation);

        this.chatbotConversationService.closeConversation("conversation-1", "user-1");

        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.CLOSED);
        verify(this.conversationGateway).update(conversation);
    }

    @Test
    void closeConversationShouldIgnoreNonActiveConversation() {
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .userId("user-1")
                .status(ConversationStatus.CLOSED)
                .build();
        when(this.conversationGateway.readById("conversation-1")).thenReturn(conversation);

        this.chatbotConversationService.closeConversation("conversation-1", "user-1");

        verify(this.conversationGateway, never()).update(any(Conversation.class));
    }

    @Test
    void reopenConversationShouldUpdateClosedConversationToActive() {
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .userId("user-1")
                .status(ConversationStatus.CLOSED)
                .build();
        when(this.conversationGateway.readById("conversation-1")).thenReturn(conversation);

        this.chatbotConversationService.reopenConversation("conversation-1", "user-1");

        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        verify(this.conversationGateway).update(conversation);
    }

    @Test
    void reopenConversationShouldIgnoreAlreadyActiveConversation() {
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .userId("user-1")
                .status(ConversationStatus.ACTIVE)
                .build();
        when(this.conversationGateway.readById("conversation-1")).thenReturn(conversation);

        this.chatbotConversationService.reopenConversation("conversation-1", "user-1");

        verify(this.conversationGateway, never()).update(any(Conversation.class));
    }

    @Test
    void reopenConversationShouldRejectArchivedConversation() {
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .userId("user-1")
                .status(ConversationStatus.ARCHIVED)
                .build();
        when(this.conversationGateway.readById("conversation-1")).thenReturn(conversation);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> this.chatbotConversationService.reopenConversation("conversation-1", "user-1")
        );

        assertThat(exception).hasMessageContaining("La conversacion archivada no se puede reabrir");
        verify(this.conversationGateway, never()).update(any(Conversation.class));
    }

    @Test
    void deleteConversationShouldDeleteMessagesAndConversationWhenOwned() {
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .userId("user-1")
                .build();
        when(this.conversationGateway.readById("conversation-1")).thenReturn(conversation);

        this.chatbotConversationService.deleteConversation("conversation-1", "user-1");

        verify(this.messageGateway).deleteByConversationId("conversation-1");
        verify(this.conversationGateway).delete("conversation-1");
    }
}

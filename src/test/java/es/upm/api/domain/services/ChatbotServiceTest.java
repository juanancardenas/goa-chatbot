package es.upm.api.domain.services;

import es.upm.api.domain.enums.ChatbotScopeViolationReason;
import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.exceptions.ConflictException;
import es.upm.api.domain.exceptions.ForbiddenException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.Message;
import es.upm.api.domain.persistence.ConversationPersistence;
import es.upm.api.domain.persistence.MessagePersistence;
import es.upm.api.domain.services.policies.ChatbotScopeDecision;
import es.upm.api.domain.services.policies.ChatbotScopePolicy;
import es.upm.api.domain.services.support.ChatbotResponseMessages;
import es.upm.api.infrastructure.dtos.ChatbotContextualConversationRequestDto;
import es.upm.api.infrastructure.dtos.ChatbotMessageRequestDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private ConversationPersistence conversationPersistence;

    @Mock
    private MessagePersistence messagePersistence;

    @Mock
    private ChatbotScopePolicy chatbotScopePolicy;

    @InjectMocks
    private ChatbotService chatbotService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void startGeneralConversationShouldPersistConversationAndMessagesForClient() {
        this.authenticate("client-1", "ROLE_CUSTOMER");
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(null, "Necesito ayuda");

        var response = chatbotService.startGeneralConversation(request);

        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationPersistence).create(conversationCaptor.capture());
        Conversation savedConversation = conversationCaptor.getValue();
        assertThat(savedConversation.getUserId()).isEqualTo("client-1");
        assertThat(savedConversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(savedConversation.getType()).isEqualTo("GENERAL");
        assertThat(savedConversation.getCreatedAt()).isNotNull();

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messagePersistence, times(2)).createAndReturnId(messageCaptor.capture());
        List<Message> savedMessages = messageCaptor.getAllValues();

        assertThat(savedMessages).hasSize(2);
        assertThat(savedMessages.get(0).getSenderType()).isEqualTo(MessageSenderType.USER);
        assertThat(savedMessages.get(0).getMessageType()).isEqualTo(MessageType.REQUEST);
        assertThat(savedMessages.get(0).getSequenceNumber()).isEqualTo(1);
        assertThat(savedMessages.get(0).getContent()).isEqualTo("Necesito ayuda");

        assertThat(savedMessages.get(1).getSenderType()).isEqualTo(MessageSenderType.ASSISTANT);
        assertThat(savedMessages.get(1).getMessageType()).isEqualTo(MessageType.RESPONSE);
        assertThat(savedMessages.get(1).getSequenceNumber()).isEqualTo(2);
        assertThat(savedMessages.get(1).getParentMessageId()).isEqualTo("user-message-id");
        assertThat(savedMessages.get(1).getContent()).isEqualTo(ChatbotResponseMessages.CLIENT_GENERAL_START_REPLY);

        assertThat(response.getConversationId()).isEqualTo(savedConversation.getId());
        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.CLIENT_GENERAL_START_REPLY);
        assertThat(response.getCreatedAt()).isEqualTo(savedConversation.getCreatedAt().toString());
    }

    @Test
    void startContextualConversationShouldReuseExistingConversation() {
        this.authenticate("customer-42", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-1")
                .userId("customer-42")
                .engagementLetterId("EL-1")
                .type("CONTEXTUAL")
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.findContextualConversation("customer-42", "EL-1", "CONTEXTUAL"))
                .thenReturn(Optional.of(existingConversation));

        ChatbotContextualConversationRequestDto request = new ChatbotContextualConversationRequestDto();
        request.setEngagementLetterId("EL-1");

        var response = chatbotService.startContextualConversation(request);

        verify(conversationPersistence, never()).create(any(Conversation.class));
        verify(messagePersistence, never()).createAndReturnId(any(Message.class));
        assertThat(response.getConversationId()).isEqualTo("conversation-1");
        assertThat(response.getEngagementLetterId()).isEqualTo("EL-1");
        assertThat(response.getCreatedAt()).isEqualTo(existingConversation.getCreatedAt().toString());
    }

    @Test
    void sendMessageShouldRejectBlankConversationId() {
        this.authenticate("professional-1", "ROLE_PROFESSIONAL");
        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto("   ", "Hola");

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> chatbotService.sendMessage(request)
        );

        assertThat(exception).hasMessageContaining("conversationId es obligatorio");
        verify(conversationPersistence, never()).readById(any());
        verify(messagePersistence, never()).createAndReturnId(any(Message.class));
    }

    @Test
    void sendMessageShouldPersistSafeReplyWhenPolicyBlocksRequest() {
        this.authenticate("professional-1", "ROLE_PROFESSIONAL");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-99")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();
        when(conversationPersistence.readById("conversation-99")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-99")).thenReturn(5);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Quiero una estrategia legal definitiva")))
                .thenReturn(ChatbotScopeDecision.reject(
                        ChatbotScopeViolationReason.LEGAL_BINDING_ADVICE_REQUESTED,
                        "safe reply",
                        true
                ));

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(
                "conversation-99",
                "Quiero una estrategia legal definitiva"
        );

        var response = chatbotService.sendMessage(request);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messagePersistence, times(2)).createAndReturnId(messageCaptor.capture());
        List<Message> savedMessages = messageCaptor.getAllValues();

        assertThat(savedMessages).hasSize(2);
        assertThat(savedMessages.get(0).getSequenceNumber()).isEqualTo(5);
        assertThat(savedMessages.get(0).getSenderType()).isEqualTo(MessageSenderType.USER);
        assertThat(savedMessages.get(1).getSequenceNumber()).isEqualTo(6);
        assertThat(savedMessages.get(1).getSenderType()).isEqualTo(MessageSenderType.ASSISTANT);
        assertThat(savedMessages.get(1).getParentMessageId()).isEqualTo("user-message-id");
        assertThat(savedMessages.get(1).getContent()).isEqualTo("safe reply");

        assertThat(response.getConversationId()).isEqualTo("conversation-99");
        assertThat(response.getMessage()).isEqualTo("safe reply");
    }

    @Test
    void closeConversationShouldCloseOwnedActiveConversation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-1")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-1")).thenReturn(existingConversation);

        chatbotService.closeConversation("conversation-1");

        assertThat(existingConversation.getStatus()).isEqualTo(ConversationStatus.CLOSED);
        verify(conversationPersistence).update(existingConversation);
        verify(messagePersistence, never()).createAndReturnId(any(Message.class));
    }

    @Test
    void closeConversationShouldRejectOtherUsersConversation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-1")
                .userId("customer-2")
                .status(ConversationStatus.ACTIVE)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-1")).thenReturn(existingConversation);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> chatbotService.closeConversation("conversation-1")
        );

        assertThat(exception).hasMessageContaining("No tienes permisos sobre esta conversacion");
        verify(conversationPersistence, never()).update(any(Conversation.class));
    }

    @Test
    void closeConversationShouldRejectClosedConversation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-1")
                .userId("customer-1")
                .status(ConversationStatus.CLOSED)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-1")).thenReturn(existingConversation);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> chatbotService.closeConversation("conversation-1")
        );

        assertThat(exception).hasMessageContaining("La conversacion no esta activa");
        verify(conversationPersistence, never()).update(any(Conversation.class));
    }

    private void authenticate(String userId, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(userId, "password", authorities)
        );
    }
}

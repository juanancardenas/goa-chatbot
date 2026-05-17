package es.upm.api.domain.services;

import es.upm.api.domain.enums.ConversationType;

import es.upm.api.domain.enums.*;
import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.exceptions.ConflictException;
import es.upm.api.domain.exceptions.ForbiddenException;
import es.upm.api.domain.model.ai.ChatbotAiResponse;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.Message;
import es.upm.api.domain.model.chatbot.command.ChatbotContextualConversationCommand;
import es.upm.api.domain.model.chatbot.command.ChatbotMessageCommand;
import es.upm.api.domain.model.chatbot.result.ChatbotConfigurationResult;
import es.upm.api.domain.model.chatbot.result.ChatbotMessageResult;
import es.upm.api.domain.model.security.AuthenticatedUserContext;
import es.upm.api.domain.model.pagination.PageResult;
import es.upm.api.domain.model.platform.ChatbotDocumentContext;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.ports.out.ConversationGateway;
import es.upm.api.domain.ports.out.EscalationGateway;
import es.upm.api.domain.ports.out.MessageGateway;
import es.upm.api.domain.ports.out.ChatbotAiClient;
import es.upm.api.domain.ports.out.ChatbotAiSettings;
import es.upm.api.domain.ports.out.UserClient;
import es.upm.api.domain.services.aireply.ChatbotAiReplyService;
import es.upm.api.domain.services.basereply.ChatbotBaseReplyBuilder;
import es.upm.api.domain.services.basereply.ChatbotDocumentContextService;
import es.upm.api.domain.services.basereply.ChatbotPlatformContextService;
import es.upm.api.domain.services.classification.ChatbotQuestionClassifier;
import es.upm.api.domain.services.conversation.ChatbotConversationService;
import es.upm.api.domain.services.conversation.ChatbotEscalationService;
import es.upm.api.domain.services.conversation.ChatbotHistoryService;
import es.upm.api.domain.services.conversation.ChatbotMessageService;
import es.upm.api.domain.services.conversation.ChatbotResponseSanitizer;
import es.upm.api.domain.services.reply.ChatbotReplyOrchestrator;
import es.upm.api.domain.services.policies.ChatbotScopeDecision;
import es.upm.api.domain.services.policies.ChatbotScopePolicy;
import es.upm.api.domain.common.ChatbotResponseMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private ConversationGateway conversationPersistence;

    @Mock
    private MessageGateway messagePersistence;

    @Mock
    private EscalationGateway escalationPersistence;

    @Mock
    private ChatbotScopePolicy chatbotScopePolicy;

    @Mock
    private ChatbotAiClient chatbotAiClient;

    @Mock
    private ChatbotAiSettings chatbotAiSettings;

    @Mock
    private ChatbotDocumentContextService chatbotDocumentContextService;

    @Mock
    private ChatbotPlatformContextService chatbotPlatformContextService;

    @Mock
    private ChatbotQuestionClassifier chatbotQuestionClassifier;

    @Mock
    private UserClient userClient;

    private ChatbotService chatbotService;
    private AuthenticatedUserContext authenticatedUser;

    @BeforeEach
    void configureChatbotAiSettings() {
        lenient().when(this.chatbotAiSettings.isEnabled()).thenReturn(false);
        lenient().when(this.chatbotAiSettings.provider()).thenReturn("ollama");
        lenient().when(this.chatbotAiSettings.model()).thenReturn("llama3.2:3b");
        lenient().when(this.chatbotAiSettings.basePrompt()).thenReturn("Prompt base de pruebas");
        lenient().when(this.chatbotAiSettings.maxInputCharacters()).thenReturn(1000);
        lenient().when(this.chatbotAiSettings.maxOutputTokens()).thenReturn(500);
        lenient().when(this.chatbotAiSettings.maxContextMessages()).thenReturn(10);
        lenient().when(this.chatbotAiSettings.temperature()).thenReturn(0.2);
        lenient().when(this.chatbotAiSettings.documentsAvailable()).thenReturn(false);
        lenient().when(this.conversationPersistence.reserveSequenceNumbers(any(), eq(2))).thenReturn(1);
        lenient().when(this.chatbotScopePolicy.evaluate(any(Conversation.class), any()))
                .thenReturn(ChatbotScopeDecision.allow());

        ChatbotMessageService chatbotMessageService = new ChatbotMessageService(this.messagePersistence, this.conversationPersistence);
        ChatbotConversationService chatbotConversationService = new ChatbotConversationService(
                this.conversationPersistence,
                this.messagePersistence
        );
        ChatbotHistoryService chatbotHistoryService = new ChatbotHistoryService(
                this.conversationPersistence,
                this.messagePersistence,
                chatbotConversationService,
                chatbotMessageService
        );
        ChatbotEscalationService chatbotEscalationService = new ChatbotEscalationService(
                chatbotConversationService,
                this.escalationPersistence,
                this.userClient
        );
        ChatbotBaseReplyBuilder chatbotBaseReplyBuilder = new ChatbotBaseReplyBuilder(
                this.chatbotQuestionClassifier,
                this.chatbotDocumentContextService
        );
        ChatbotAiReplyService chatbotAiReplyService = new ChatbotAiReplyService(
                this.chatbotAiClient,
                this.chatbotAiSettings,
                chatbotMessageService
        );
        ChatbotReplyOrchestrator chatbotReplyOrchestrator = new ChatbotReplyOrchestrator(
                chatbotBaseReplyBuilder,
                chatbotAiReplyService,
                this.chatbotPlatformContextService,
                this.chatbotScopePolicy
        );

        this.chatbotService = new ChatbotService(
                chatbotMessageService,
                new ChatbotResponseSanitizer(),
                chatbotConversationService,
                chatbotHistoryService,
                chatbotEscalationService,
                chatbotReplyOrchestrator,
                this.chatbotAiSettings
        );
    }

    @Test
    void startGeneralConversationShouldPersistConversationAndMessagesForClient() {
        this.authenticate("client-1", "ROLE_CUSTOMER");
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");

        ChatbotMessageCommand request = new ChatbotMessageCommand(null, "Necesito ayuda");

        var response = chatbotService.startGeneralConversation(this.authenticatedUser, request);

        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationPersistence).create(conversationCaptor.capture());
        Conversation savedConversation = conversationCaptor.getValue();
        assertThat(savedConversation.getUserId()).isEqualTo("client-1");
        assertThat(savedConversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(savedConversation.getType()).isEqualTo(ConversationType.GENERAL);
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
    void startGeneralConversationShouldPersistConversationAndMessagesForProfessional() {
        this.authenticate("professional-1", "ROLE_ADMIN");
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");

        ChatbotMessageCommand request = new ChatbotMessageCommand(null, "Necesito soporte");

        var response = chatbotService.startGeneralConversation(this.authenticatedUser, request);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messagePersistence, times(2)).createAndReturnId(messageCaptor.capture());
        List<Message> savedMessages = messageCaptor.getAllValues();

        assertThat(savedMessages.get(1).getContent()).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_GENERAL_START_REPLY);
        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.GENERAL);
        assertThat(response.getUsedPlatformData()).isFalse();
        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_GENERAL_START_REPLY);
    }

    @Test
    void startContextualConversationShouldReuseExistingConversation() {
        this.authenticate("customer-42", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-1")
                .userId("customer-42")
                .engagementLetterId("EL-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.findActiveContextualConversation("customer-42", "EL-1", ConversationType.CONTEXTUAL))
                .thenReturn(Optional.of(existingConversation));

        ChatbotContextualConversationCommand request = new ChatbotContextualConversationCommand("EL-1");

        var response = chatbotService.startContextualConversation(this.authenticatedUser, request);

        verify(conversationPersistence, never()).create(any(Conversation.class));
        verify(messagePersistence, never()).createAndReturnId(any(Message.class));
        assertThat(response.getConversationId()).isEqualTo("conversation-1");
        assertThat(response.getEngagementLetterId()).isEqualTo("EL-1");
        assertThat(response.getCreatedAt()).isEqualTo(existingConversation.getCreatedAt().toString());
    }

    @Test
    void startContextualConversationShouldCreateConversationWhenNoActiveConversationExists() {
        this.authenticate("customer-77", "ROLE_CUSTOMER");
        when(conversationPersistence.findActiveContextualConversation("customer-77", "EL-77", ConversationType.CONTEXTUAL))
                .thenReturn(Optional.empty());

        ChatbotContextualConversationCommand request = new ChatbotContextualConversationCommand("EL-77");

        var response = chatbotService.startContextualConversation(this.authenticatedUser, request);

        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationPersistence).create(conversationCaptor.capture());
        Conversation savedConversation = conversationCaptor.getValue();

        assertThat(savedConversation.getUserId()).isEqualTo("customer-77");
        assertThat(savedConversation.getEngagementLetterId()).isEqualTo("EL-77");
        assertThat(savedConversation.getType()).isEqualTo(ConversationType.CONTEXTUAL);
        assertThat(savedConversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(response.getConversationId()).isEqualTo(savedConversation.getId());
        assertThat(response.getEngagementLetterId()).isEqualTo("EL-77");
        assertThat(response.getCreatedAt()).isEqualTo(savedConversation.getCreatedAt().toString());
    }

    @Test
    void readConversationHistoryListShouldReturnGeneralSummariesWithLatestMessagePreview() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation latestConversation = Conversation.builder()
                .id("conversation-1")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 21, 9, 0))
                .build();
        Conversation olderConversation = Conversation.builder()
                .id("conversation-2")
                .userId("professional-1")
                .status(ConversationStatus.CLOSED)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 20, 9, 0))
                .build();

        when(conversationPersistence.findByUserIdAndTypeOrderByCreatedAtDesc("professional-1", ConversationType.GENERAL))
                .thenReturn(List.of(latestConversation, olderConversation));
        when(messagePersistence.findLatestByConversationId("conversation-1"))
                .thenReturn(Optional.of(
                        Message.builder()
                                .id("message-1")
                                .conversationId("conversation-1")
                                .content("Resumen reciente")
                                .timestamp(LocalDateTime.of(2026, 4, 21, 10, 15))
                                .build()
                ));
        when(messagePersistence.findLatestByConversationId("conversation-2"))
                .thenReturn(Optional.empty());

        var response = chatbotService.readConversationHistoryList(this.authenticatedUser, " general ", null);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getConversationId()).isEqualTo("conversation-1");
        assertThat(response.get(0).getPreview()).isEqualTo("Resumen reciente");
        assertThat(response.get(0).getLastMessageAt()).isEqualTo("2026-04-21T10:15");
        assertThat(response.get(1).getConversationId()).isEqualTo("conversation-2");
        assertThat(response.get(1).getPreview()).isNull();
        assertThat(response.get(1).getLastMessageAt()).isNull();
    }

    @Test
    void readConversationHistoryListShouldUseContextualFinderWhenTypeRequiresEngagementLetter() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation conversation = Conversation.builder()
                .id("conversation-ctx-1")
                .userId("customer-1")
                .engagementLetterId("EL-9")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, 4, 21, 12, 0))
                .build();

        when(conversationPersistence.findByUserIdAndEngagementLetterIdAndTypeOrderByCreatedAtDesc(
                "customer-1",
                "EL-9",
                ConversationType.CONTEXTUAL
        )).thenReturn(List.of(conversation));
        when(messagePersistence.findLatestByConversationId("conversation-ctx-1")).thenReturn(Optional.empty());

        var response = chatbotService.readConversationHistoryList(this.authenticatedUser, " contextual ", "EL-9");

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getConversationId()).isEqualTo("conversation-ctx-1");
        verify(conversationPersistence, never()).findByUserIdAndTypeOrderByCreatedAtDesc(
                any(),
                eq(ConversationType.CONTEXTUAL)
        );
    }

    @Test
    void readConversationHistoryListShouldRejectUnsupportedConversationType() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> chatbotService.readConversationHistoryList(this.authenticatedUser, "other", null)
        );

        assertThat(exception).hasMessageContaining("Tipo de conversacion no soportado: other");
        verify(conversationPersistence, never()).findByUserIdAndTypeOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void readConversationHistoryListShouldDefaultToGeneralTypeWhenBlank() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation conversation = Conversation.builder()
                .id("conversation-general-1")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 21, 9, 0))
                .build();

        when(conversationPersistence.findByUserIdAndTypeOrderByCreatedAtDesc("professional-1", ConversationType.GENERAL))
                .thenReturn(List.of(conversation));
        when(messagePersistence.findLatestByConversationId("conversation-general-1")).thenReturn(Optional.empty());

        var response = chatbotService.readConversationHistoryList(this.authenticatedUser, "   ", null);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getConversationId()).isEqualTo("conversation-general-1");
        verify(conversationPersistence).findByUserIdAndTypeOrderByCreatedAtDesc("professional-1", ConversationType.GENERAL);
    }

    @Test
    void readConversationHistoryListShouldReturnAllContextualConversationsWhenEngagementLetterIdIsBlank() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation conversation = Conversation.builder()
                .id("conversation-ctx-all")
                .userId("customer-1")
                .engagementLetterId("EL-33")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, 4, 21, 12, 0))
                .build();

        when(conversationPersistence.findByUserIdAndTypeOrderByCreatedAtDesc("customer-1", ConversationType.CONTEXTUAL))
                .thenReturn(List.of(conversation));
        when(messagePersistence.findLatestByConversationId("conversation-ctx-all")).thenReturn(Optional.empty());

        var response = chatbotService.readConversationHistoryList(this.authenticatedUser, "CONTEXTUAL", " ");

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getConversationId()).isEqualTo("conversation-ctx-all");
        verify(conversationPersistence).findByUserIdAndTypeOrderByCreatedAtDesc("customer-1", ConversationType.CONTEXTUAL);
        verify(conversationPersistence, never())
                .findByUserIdAndEngagementLetterIdAndTypeOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    void readConversationHistoryShouldSortMessagesAscendingAndApplyDefaultPagination() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation conversation = Conversation.builder()
                .id("conversation-history")
                .userId("customer-1")
                .engagementLetterId("EL-10")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, 4, 21, 8, 0))
                .build();

        Message newestInPage = Message.builder()
                .id("message-2")
                .conversationId("conversation-history")
                .senderType(MessageSenderType.ASSISTANT)
                .messageType(MessageType.RESPONSE)
                .content("Segundo")
                .timestamp(LocalDateTime.of(2026, 4, 21, 9, 30))
                .sequenceNumber(2)
                .parentMessageId("message-1")
                .build();
        Message oldestInPage = Message.builder()
                .id("message-1")
                .conversationId("conversation-history")
                .senderType(MessageSenderType.USER)
                .messageType(MessageType.REQUEST)
                .content("Primero")
                .timestamp(LocalDateTime.of(2026, 4, 21, 9, 0))
                .sequenceNumber(1)
                .build();

        when(conversationPersistence.readById("conversation-history")).thenReturn(conversation);
        when(messagePersistence.findByConversationIdOrderedDesc("conversation-history", 0, 20))
                .thenReturn(PageResult.<Message>builder()
                        .content(List.of(newestInPage, oldestInPage))
                        .page(0)
                        .size(20)
                        .hasNext(true)
                        .totalElements(12)
                        .build());

        var response = chatbotService.readConversationHistory(this.authenticatedUser, "conversation-history", null, null);

        assertThat(response.getConversationId()).isEqualTo("conversation-history");
        assertThat(response.getEngagementLetterId()).isEqualTo("EL-10");
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getHasMore()).isTrue();
        assertThat(response.getTotalMessages()).isEqualTo(12);
        assertThat(response.getMessages()).hasSize(2);
        assertThat(response.getMessages().get(0).getId()).isEqualTo("message-1");
        assertThat(response.getMessages().get(0).getSequenceNumber()).isEqualTo(1);
        assertThat(response.getMessages().get(1).getId()).isEqualTo("message-2");
        assertThat(response.getMessages().get(1).getSequenceNumber()).isEqualTo(2);
    }

    @Test
    void readConversationHistoryShouldDefaultNegativePageToFirstPage() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation conversation = Conversation.builder()
                .id("conversation-history")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 21, 8, 0))
                .build();
        when(conversationPersistence.readById("conversation-history")).thenReturn(conversation);
        when(messagePersistence.findByConversationIdOrderedDesc("conversation-history", 0, 10))
                .thenReturn(PageResult.<Message>builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .hasNext(false)
                        .totalElements(0)
                        .build());

        var response = chatbotService.readConversationHistory(this.authenticatedUser, "conversation-history", -1, 10);

        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
        verify(messagePersistence).findByConversationIdOrderedDesc("conversation-history", 0, 10);
    }

    @Test
    void readConversationHistoryShouldClampSizeToMaximum() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation conversation = Conversation.builder()
                .id("conversation-history")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 21, 8, 0))
                .build();
        when(conversationPersistence.readById("conversation-history")).thenReturn(conversation);
        when(messagePersistence.findByConversationIdOrderedDesc("conversation-history", 0, 100))
                .thenReturn(PageResult.<Message>builder()
                        .content(List.of())
                        .page(0)
                        .size(100)
                        .hasNext(false)
                        .totalElements(0)
                        .build());

        var response = chatbotService.readConversationHistory(this.authenticatedUser, "conversation-history", 0, 101);

        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(100);
        verify(messagePersistence).findByConversationIdOrderedDesc("conversation-history", 0, 100);
    }

    @Test
    void readConversationHistoryShouldDefaultSizeWhenBelowMinimum() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation conversation = Conversation.builder()
                .id("conversation-history")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 21, 8, 0))
                .build();
        when(conversationPersistence.readById("conversation-history")).thenReturn(conversation);
        when(messagePersistence.findByConversationIdOrderedDesc("conversation-history", 0, 20))
                .thenReturn(PageResult.<Message>builder()
                        .content(List.of())
                        .page(0)
                        .size(20)
                        .hasNext(false)
                        .totalElements(0)
                        .build());

        var response = chatbotService.readConversationHistory(this.authenticatedUser, "conversation-history", 0, 0);

        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        verify(messagePersistence).findByConversationIdOrderedDesc("conversation-history", 0, 20);
    }

    @Test
    void sendMessageShouldRejectBlankConversationId() {
        this.authenticate("professional-1", "ROLE_PROFESSIONAL");
        ChatbotMessageCommand request = new ChatbotMessageCommand("   ", "Hola");

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> chatbotService.sendMessage(this.authenticatedUser, request)
        );

        assertThat(exception).hasMessageContaining("conversationId es obligatorio");
        verify(conversationPersistence, never()).readById(any());
        verify(messagePersistence, never()).createAndReturnId(any(Message.class));
    }

    @Test
    void sendMessageShouldReturnCourtesyReplyWithoutCallingAiOrPlatformContext() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation conversation = Conversation.builder()
                .id("conversation-courtesy")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 30, 10, 0))
                .build();

        when(this.conversationPersistence.readById("conversation-courtesy")).thenReturn(conversation);
        when(this.conversationPersistence.reserveSequenceNumbers("conversation-courtesy", 2)).thenReturn(5);
        when(this.messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");

        ChatbotMessageResult response = this.chatbotService.sendMessage(
                this.authenticatedUser,
                new ChatbotMessageCommand("conversation-courtesy", "Muchas gracias por la ayuda")
        );

        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.CLIENT_COURTESY_REPLY);
        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.GENERAL);
        assertThat(response.getUsedPlatformData()).isFalse();
        assertThat(response.getSourcesSummary()).isEmpty();
        verify(this.chatbotAiClient, never()).generate(any());
        verify(this.chatbotPlatformContextService, never()).loadContext(any());

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(this.messagePersistence, times(2)).createAndReturnId(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(1).getContent())
                .isEqualTo(ChatbotResponseMessages.CLIENT_COURTESY_REPLY);
        assertThat(messageCaptor.getAllValues().get(1).getSequenceNumber()).isEqualTo(6);
        assertThat(messageCaptor.getAllValues().get(1).getParentMessageId()).isEqualTo("user-message-id");
    }

    @Test
    void sendMessageShouldReplyContextUnavailableWhenLegalTasksContextCannotBeLoaded() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation conversation = Conversation.builder()
                .id("conversation-contextual-unavailable")
                .userId("professional-1")
                .engagementLetterId("engagement-001")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, 4, 30, 10, 0))
                .build();

        when(this.conversationPersistence.readById("conversation-contextual-unavailable"))
                .thenReturn(conversation);
        when(this.conversationPersistence.reserveSequenceNumbers("conversation-contextual-unavailable", 2))
                .thenReturn(3);
        when(this.messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(this.chatbotQuestionClassifier.classify("Cuáles son las tareas legales de este encargo?"))
                .thenReturn(PlatformQuestionType.LEGAL_TASKS);
        when(this.chatbotPlatformContextService.loadContext("engagement-001"))
                .thenReturn(Optional.empty());
        when(this.chatbotAiSettings.isEnabled()).thenReturn(false);

        ChatbotMessageCommand request = new ChatbotMessageCommand(
                "conversation-contextual-unavailable",
                "Cuáles son las tareas legales de este encargo?"
        );

        ChatbotMessageResult response = this.chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getMessage()).contains("No he podido recuperar");
        assertThat(response.getMessage()).contains("Tareas Legales");
        assertThat(response.getUsedPlatformData()).isFalse();
    }

    @Test
    void sendMessageShouldReplyNoLegalTasksWhenContextHasNoTasks() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation conversation = Conversation.builder()
                .id("conversation-contextual-no-legal-tasks")
                .userId("professional-1")
                .engagementLetterId("engagement-001")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, 4, 30, 10, 0))
                .build();

        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId("engagement-001")
                .ownerDisplayName("Cliente Demo")
                .procedureTitles(List.of("Procedimiento de herencia"))
                .legalTaskSummaries(List.of())
                .recentEventSummaries(List.of())
                .sourcesSummary(List.of("Procedimiento: Procedimiento de herencia"))
                .build();

        when(this.conversationPersistence.readById("conversation-contextual-no-legal-tasks"))
                .thenReturn(conversation);
        when(this.conversationPersistence.reserveSequenceNumbers("conversation-contextual-no-legal-tasks", 2))
                .thenReturn(3);
        when(this.messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(this.chatbotQuestionClassifier.classify("Cuáles son las tareas legales?"))
                .thenReturn(PlatformQuestionType.LEGAL_TASKS);
        when(this.chatbotPlatformContextService.loadContext("engagement-001"))
                .thenReturn(Optional.of(platformContext));
        when(this.chatbotAiSettings.isEnabled()).thenReturn(false);

        ChatbotMessageCommand request = new ChatbotMessageCommand(
                "conversation-contextual-no-legal-tasks",
                "Cuáles son las tareas legales?"
        );

        ChatbotMessageResult response = this.chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getMessage()).contains("No se han encontrado");
        assertThat(response.getMessage()).contains("Tareas Legales");
        assertThat(response.getUsedPlatformData()).isTrue();
    }

    @Test
    void sendMessageShouldReplyWithLegalTasksWhenContextualConversationHasTasks() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation conversation = Conversation.builder()
                .id("conversation-contextual-legal-tasks")
                .userId("professional-1")
                .engagementLetterId("engagement-001")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, 4, 30, 10, 0))
                .build();

        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId("engagement-001")
                .ownerDisplayName("Cliente Demo")
                .procedureTitles(List.of("Procedimiento de herencia"))
                .legalTaskSummaries(List.of(
                        "Procedimiento de herencia: Estudio de antecedentes y documentación.",
                        "Procedimiento de herencia: Asesoramiento jurídico."
                ))
                .recentEventSummaries(List.of())
                .sourcesSummary(List.of("Legal Task: Procedimiento de herencia: Estudio de antecedentes y documentación."))
                .build();

        when(this.conversationPersistence.readById("conversation-contextual-legal-tasks"))
                .thenReturn(conversation);
        when(this.conversationPersistence.reserveSequenceNumbers("conversation-contextual-legal-tasks", 2))
                .thenReturn(3);
        when(this.messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(this.chatbotQuestionClassifier.classify("Cuáles son las tareas legales de este encargo?"))
                .thenReturn(PlatformQuestionType.LEGAL_TASKS);
        when(this.chatbotPlatformContextService.loadContext("engagement-001"))
                .thenReturn(Optional.of(platformContext));
        when(this.chatbotAiSettings.isEnabled()).thenReturn(false);

        ChatbotMessageCommand request = new ChatbotMessageCommand(
                "conversation-contextual-legal-tasks",
                "Cuáles son las tareas legales de este encargo?"
        );

        ChatbotMessageResult response = this.chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getMessage()).contains("Tareas Legales");
        assertThat(response.getMessage()).contains("Estudio de antecedentes y documentación");
        assertThat(response.getMessage()).contains("Asesoramiento jurídico");
        assertThat(response.getUsedPlatformData()).isTrue();
        assertThat(response.getSourcesSummary()).isNotEmpty();
    }

    @Test
    void sendMessageShouldMentionNoRecentEventsWhenContextHasNoVisibleEvents() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-100"))
                .thenReturn(Optional.of(
                        ChatbotPlatformContext.builder()
                                .engagementLetterId("EL-100")
                                .ownerDisplayName("Ana Ocaña")
                                .procedureTitles(List.of("Reclamación civil"))
                                .recentEventSummaries(List.of())
                                .sourcesSummary(List.of("Hoja de encargo"))
                                .build()
                ));

        ChatbotMessageCommand request = new ChatbotMessageCommand("conversation-ctx", "Dame contexto del caso");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(response.getUsedPlatformData()).isTrue();
        assertThat(response.getMessage()).contains("No se han encontrado hitos recientes visibles");
    }

    @Test
    void sendMessageShouldPersistSafeReplyWhenPolicyBlocksRequest() {
        this.authenticate("professional-1", "ROLE_PROFESSIONAL");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-99")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();
        when(conversationPersistence.readById("conversation-99")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-99", 2)).thenReturn(5);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Quiero una estrategia legal definitiva")))
                .thenReturn(ChatbotScopeDecision.reject(
                        ChatbotScopeViolationReason.LEGAL_BINDING_ADVICE_REQUESTED,
                        "safe reply",
                        true
                ));

        ChatbotMessageCommand request = new ChatbotMessageCommand(
                "conversation-99",
                "Quiero una estrategia legal definitiva"
        );

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

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
    void sendMessageShouldUseAiClientWhenAiConfigurationIsEnabled() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ai")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.conversationPersistence.readById("conversation-ai")).thenReturn(existingConversation);
        when(this.conversationPersistence.reserveSequenceNumbers("conversation-ai", 2)).thenReturn(3);
        when(this.messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(this.messagePersistence.findByConversationIdOrdered("conversation-ai"))
                .thenReturn(List.of());
        when(this.chatbotQuestionClassifier.classify("Explícame qué puedes hacer"))
                .thenReturn(PlatformQuestionType.GENERAL_CONTEXT);
        when(this.chatbotAiClient.generate(any()))
                .thenReturn(ChatbotAiResponse.builder()
                        .content("Respuesta generada por Ollama")
                        .provider("ollama")
                        .model("llama3.2:3b")
                        .finishReason("SUCCESS")
                        .build());

        ChatbotMessageCommand request = new ChatbotMessageCommand(
                "conversation-ai",
                "Explícame qué puedes hacer"
        );

        var response = this.chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getMessage()).isEqualTo("Respuesta generada por Ollama");
        verify(this.chatbotAiClient).generate(any());
    }

    @Test
    void sendMessageShouldNotCallAiClientWhenScopePolicyRejectsRequest() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-restricted")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(this.conversationPersistence.readById("conversation-restricted")).thenReturn(existingConversation);
        when(this.conversationPersistence.reserveSequenceNumbers("conversation-restricted", 2)).thenReturn(3);
        when(this.messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(this.chatbotScopePolicy.evaluate(eq(existingConversation), eq("Dame asesoría legal definitiva")))
                .thenReturn(ChatbotScopeDecision.reject(
                        ChatbotScopeViolationReason.LEGAL_BINDING_ADVICE_REQUESTED,
                        "No puedo ofrecer asesoramiento legal vinculante.",
                        true
                ));

        ChatbotMessageCommand request = new ChatbotMessageCommand(
                "conversation-restricted",
                "Dame asesoría legal definitiva"
        );

        var response = this.chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getMessage()).isEqualTo("No puedo ofrecer asesoramiento legal vinculante.");
        verify(this.chatbotAiClient, never()).generate(any());
    }

    @Test
    void sendMessageShouldRejectMessageLongerThanConfiguredLimit() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        when(this.chatbotAiSettings.maxInputCharacters()).thenReturn(5);

        ChatbotMessageCommand request = new ChatbotMessageCommand(
                "conversation-long-message",
                "mensaje demasiado largo"
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> this.chatbotService.sendMessage(this.authenticatedUser, request)
        );

        assertThat(exception).hasMessageContaining("limite maximo de caracteres");
        verify(this.conversationPersistence, never()).readById(any());
        verify(this.messagePersistence, never()).createAndReturnId(any(Message.class));
    }

    @Test
    void startGeneralConversationShouldRejectMessageLongerThanConfiguredLimit() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        when(this.chatbotAiSettings.maxInputCharacters()).thenReturn(5);

        ChatbotMessageCommand request = new ChatbotMessageCommand(
                null,
                "mensaje demasiado largo"
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> this.chatbotService.startGeneralConversation(this.authenticatedUser, request)
        );

        assertThat(exception).hasMessageContaining("limite maximo de caracteres");
        verify(this.conversationPersistence, never()).create(any(Conversation.class));
        verify(this.messagePersistence, never()).createAndReturnId(any(Message.class));
    }

    @Test
    void sendMessageShouldUseDocumentStubReplyWhenDocumentsIntegrationIsNotAvailable() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        ChatbotPlatformContext context = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-100")
                .ownerDisplayName("Ana Ocaña")
                .procedureTitles(List.of("Reclamación civil"))
                .recentEventSummaries(List.of())
                .sourcesSummary(List.of("Hoja de encargo"))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.of(context));
        when(chatbotQuestionClassifier.classify("Que documentos hay en el expediente"))
                .thenReturn(PlatformQuestionType.DOCUMENTS);
        when(chatbotDocumentContextService.loadDocumentContext(existingConversation))
                .thenReturn(
                        ChatbotDocumentContext.builder()
                                .available(false)
                                .authorizedSourceConfigured(false)
                                .visibleDocumentTitles(List.of())
                                .sourcesSummary(List.of())
                                .build()
                );

        ChatbotMessageCommand request = new ChatbotMessageCommand("conversation-ctx", "Que documentos hay en el expediente");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(response.getMessage()).contains("integración documental real aún no está disponible");
        assertThat(response.getMessage()).contains("Reclamación civil");
    }

    @Test
    void sendMessageShouldUsePlatformContextForContextualConversation() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-100"))
                .thenReturn(Optional.of(
                        ChatbotPlatformContext.builder()
                                .engagementLetterId("EL-100")
                                .ownerDisplayName("Ana Ocaña")
                                .procedureTitles(List.of("Reclamación civil"))
                                .recentEventSummaries(List.of(
                                        "Se registró escrito [MILESTONE] - OPEN",
                                        "Vista programada [EVENT] - SCHEDULED"
                                ))
                                .sourcesSummary(List.of(
                                        "Hoja de encargo",
                                        "Procedimiento: Reclamación civil",
                                        "Hito/evento: Se registró escrito [MILESTONE] - OPEN"
                                ))
                                .build()
                ));

        ChatbotMessageCommand request = new ChatbotMessageCommand("conversation-ctx", "Dame contexto del caso");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(response.getUsedPlatformData()).isTrue();
        assertThat(response.getSourcesSummary()).contains("Hoja de encargo");
        assertThat(response.getMessage()).contains("EL-100");
        assertThat(response.getMessage()).contains("Ana Ocaña");
        assertThat(response.getMessage()).contains("Reclamación civil");
        assertThat(response.getMessage()).contains("Se registró escrito");
        assertThat(response.getMessage()).contains("Vista programada");
    }

    @Test
    void sendMessageShouldReturnClientStatusFallbackWhenContextIsUnavailable() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.empty());
        when(chatbotQuestionClassifier.classify("Cual es el estado de mi caso"))
                .thenReturn(PlatformQuestionType.ENGAGEMENT_STATUS);

        ChatbotMessageCommand request = new ChatbotMessageCommand("conversation-ctx", "Cual es el estado de mi caso");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_RESTRICTED);
        assertThat(response.getUsedPlatformData()).isFalse();
        assertThat(response.getSourcesSummary()).isEmpty();
        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_STATUS_REPLY);
    }

    @Test
    void sendMessageShouldReturnProfessionalEventsFallbackWhenContextIsUnavailable() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.empty());
        when(chatbotQuestionClassifier.classify("Que hitos recientes tiene el caso"))
                .thenReturn(PlatformQuestionType.TIMELINE_EVENTS);

        ChatbotMessageCommand request = new ChatbotMessageCommand("conversation-ctx", "Que hitos recientes tiene el caso");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_RESTRICTED);
        assertThat(response.getUsedPlatformData()).isFalse();
        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_EVENTS_REPLY);
    }

    @Test
    void sendMessageShouldReturnProfessionalDocumentsFallbackWhenContextIsUnavailable() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.empty());
        when(chatbotQuestionClassifier.classify("Que documentos hay en el expediente"))
                .thenReturn(PlatformQuestionType.DOCUMENTS);

        ChatbotMessageCommand request = new ChatbotMessageCommand("conversation-ctx", "Que documentos hay en el expediente");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_RESTRICTED);
        assertThat(response.getUsedPlatformData()).isFalse();
        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_DOCUMENTS_STUB_REPLY);
    }

    @Test
    void sendMessageShouldReturnClientGeneralFallbackWhenContextIsUnavailable() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.empty());
        when(chatbotQuestionClassifier.classify("Dame un resumen del caso"))
                .thenReturn(PlatformQuestionType.GENERAL_CONTEXT);

        ChatbotMessageCommand request = new ChatbotMessageCommand("conversation-ctx", "Dame un resumen del caso");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_RESTRICTED);
        assertThat(response.getUsedPlatformData()).isFalse();
        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_GENERAL_REPLY);
    }

    @Test
    void sendMessageShouldReturnRestrictedContextReplyWhenPlatformDataIsUnavailable() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-100"))
                .thenReturn(Optional.empty());

        ChatbotMessageCommand request = new ChatbotMessageCommand("conversation-ctx", "Dame contexto del caso");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_RESTRICTED);
        assertThat(response.getUsedPlatformData()).isFalse();
        assertThat(response.getSourcesSummary()).isEmpty();
        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.CONTEXTUAL_PLATFORM_DATA_UNAVAILABLE_REPLY);
    }

    @Test
    void sendMessageShouldReturnStatusReplyForEngagementQuestions() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        ChatbotPlatformContext context = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-100")
                .ownerDisplayName("Ana Ocaña")
                .procedureTitles(List.of("Reclamación civil"))
                .recentEventSummaries(List.of("Vista programada [EVENT] - SCHEDULED"))
                .sourcesSummary(List.of("Hoja de encargo"))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.of(context));
        when(chatbotQuestionClassifier.classify("Cual es el estado del encargo"))
                .thenReturn(PlatformQuestionType.ENGAGEMENT_STATUS);

        ChatbotMessageCommand request = new ChatbotMessageCommand("conversation-ctx", "Cual es el estado del encargo");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(response.getMessage()).contains("EL-100");
        assertThat(response.getMessage()).contains("Ana Ocaña");
        assertThat(response.getMessage()).contains("Reclamación civil");
    }

    @Test
    void sendMessageShouldReturnTimelineReplyForEventQuestions() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        ChatbotPlatformContext context = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-100")
                .ownerDisplayName("Ana Ocaña")
                .procedureTitles(List.of("Reclamación civil"))
                .recentEventSummaries(List.of(
                        "Se registró escrito [MILESTONE] - OPEN",
                        "Vista programada [EVENT] - SCHEDULED"
                ))
                .sourcesSummary(List.of("Hoja de encargo"))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.of(context));
        when(chatbotQuestionClassifier.classify("Que hitos recientes tiene el caso"))
                .thenReturn(PlatformQuestionType.TIMELINE_EVENTS);

        ChatbotMessageCommand request = new ChatbotMessageCommand("conversation-ctx", "Que hitos recientes tiene el caso");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(response.getMessage()).contains("Se registró escrito");
        assertThat(response.getMessage()).contains("Vista programada");
    }

    @Test
    void sendMessageShouldReturnDocumentSafeReplyForDocumentQuestions() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        ChatbotPlatformContext context = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-100")
                .ownerDisplayName("Ana Ocaña")
                .procedureTitles(List.of("Reclamación civil"))
                .recentEventSummaries(List.of())
                .sourcesSummary(List.of("Hoja de encargo"))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.of(context));
        when(chatbotQuestionClassifier.classify("Que documentos hay en el caso"))
                .thenReturn(PlatformQuestionType.DOCUMENTS);

        ChatbotMessageCommand request = new ChatbotMessageCommand("conversation-ctx", "Que documentos hay en el caso");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(response.getMessage()).contains(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_DOCUMENTS_STUB_REPLY);
    }

    @Test
    void sendMessageShouldReturnClientFriendlyStatusReplyForContextualQuestion() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        ChatbotPlatformContext context = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-100")
                .ownerDisplayName("Ana Ocaña")
                .procedureTitles(List.of("Reclamación civil"))
                .recentEventSummaries(List.of("Vista programada [EVENT] - SCHEDULED"))
                .sourcesSummary(List.of("Hoja de encargo"))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.of(context));
        when(chatbotQuestionClassifier.classify("Cual es el estado de mi caso"))
                .thenReturn(PlatformQuestionType.ENGAGEMENT_STATUS);

        ChatbotMessageCommand request = new ChatbotMessageCommand("conversation-ctx", "Cual es el estado de mi caso");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(response.getMessage()).contains("puedo explicarte el caso");
        assertThat(response.getMessage()).contains("procedimientos visibles relacionados");
    }

    @Test
    void sendMessageShouldReturnProfessionalDocumentReplyForContextualQuestion() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        ChatbotPlatformContext context = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-100")
                .ownerDisplayName("Ana Ocaña")
                .procedureTitles(List.of("Reclamación civil"))
                .recentEventSummaries(List.of())
                .sourcesSummary(List.of("Hoja de encargo"))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.of(context));
        when(chatbotQuestionClassifier.classify("Que documentos hay en el expediente"))
                .thenReturn(PlatformQuestionType.DOCUMENTS);

        ChatbotMessageCommand request = new ChatbotMessageCommand("conversation-ctx", "Que documentos hay en el expediente");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(response.getMessage()).contains(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_DOCUMENTS_STUB_REPLY);
    }

    @Test
    void sendMessageShouldReturnGeneralStatusFaqForCustomer() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        String userMessage = "¿Cómo puedo consultar el estado de un encargo?";

        Conversation existingConversation = Conversation.builder()
                .id("conversation-general")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 21, 10, 0))
                .build();

        when(conversationPersistence.readById("conversation-general")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-general", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotQuestionClassifier.classify(userMessage))
                .thenReturn(PlatformQuestionType.ENGAGEMENT_STATUS);

        ChatbotMessageCommand request = new ChatbotMessageCommand(
                "conversation-general",
                userMessage
        );

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getConversationId()).isEqualTo("conversation-general");
        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.GENERAL);
        assertThat(response.getUsedPlatformData()).isFalse();
        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.CLIENT_GENERAL_STATUS_REPLY);
    }

    @Test
    void sendMessageShouldReturnGeneralTimelineFaqForProfessional() {
        this.authenticate("professional-1", "ROLE_ADMIN");
        String userMessage = "Que plazos o hitos tiene esto";

        Conversation existingConversation = Conversation.builder()
                .id("conversation-general")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 21, 10, 0))
                .build();

        when(conversationPersistence.readById("conversation-general")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-general", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotQuestionClassifier.classify(userMessage))
                .thenReturn(PlatformQuestionType.TIMELINE_EVENTS);

        ChatbotMessageCommand request = new ChatbotMessageCommand(
                "conversation-general",
                userMessage
        );

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getConversationId()).isEqualTo("conversation-general");
        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.GENERAL);
        assertThat(response.getUsedPlatformData()).isFalse();
        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_GENERAL_TIMELINE_EXAMPLE_REPLY);
    }

    @Test
    void sendMessageShouldReturnGeneralDocumentsFaqForProfessional() {
        this.authenticate("professional-1", "ROLE_ADMIN");
        String userMessage = "Que documentos aplican aqui";

        Conversation existingConversation = Conversation.builder()
                .id("conversation-general")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 21, 10, 0))
                .build();

        when(conversationPersistence.readById("conversation-general")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-general", 2)).thenReturn(5);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotQuestionClassifier.classify(userMessage))
                .thenReturn(PlatformQuestionType.DOCUMENTS);

        ChatbotMessageCommand request = new ChatbotMessageCommand(
                "conversation-general",
                userMessage
        );

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getConversationId()).isEqualTo("conversation-general");
        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.GENERAL);
        assertThat(response.getUsedPlatformData()).isFalse();
        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_GENERAL_DOCUMENTS_STUB_REPLY);
    }

    @Test
    void sendMessageShouldReturnGeneralContextFaqWhenClassifierReturnsNull() {
        this.authenticate("professional-1", "ROLE_ADMIN");
        String userMessage = "Dame una vision general";

        Conversation existingConversation = Conversation.builder()
                .id("conversation-general")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 21, 10, 0))
                .build();

        when(conversationPersistence.readById("conversation-general")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-general", 2)).thenReturn(7);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotQuestionClassifier.classify(userMessage))
                .thenReturn(null);

        ChatbotMessageCommand request = new ChatbotMessageCommand(
                "conversation-general",
                userMessage
        );

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getConversationId()).isEqualTo("conversation-general");
        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.GENERAL);
        assertThat(response.getUsedPlatformData()).isFalse();
        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_GENERAL_CONTEXT_REPLY);
    }

    @Test
    void sendMessageShouldReturnTimelineReplyWithEventsAndProceduresWhenContextExists() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx-events")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-200")
                .createdAt(LocalDateTime.of(2026, 4, 21, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx-events")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx-events", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-200"))
                .thenReturn(Optional.of(
                        ChatbotPlatformContext.builder()
                                .engagementLetterId("EL-200")
                                .ownerDisplayName("Ana Ocaña")
                                .procedureTitles(List.of("Reclamación civil"))
                                .recentEventSummaries(List.of("Presentación de escrito [PROCEDURE] - OPEN"))
                                .sourcesSummary(List.of("Hoja de encargo EL-200"))
                                .build()
                ));

        ChatbotMessageCommand request =
                new ChatbotMessageCommand("conversation-ctx-events", "¿Qué hitos recientes tiene este caso?");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(response.getUsedPlatformData()).isTrue();
        assertThat(response.getMessage()).contains("Presentación de escrito");
        assertThat(response.getMessage()).contains("Reclamación civil");
    }

    @Test
    void sendMessageShouldReturnClientNoEventsReplyWhenTimelineHasNoEventsOrProcedures() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx-empty-events")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-201")
                .createdAt(LocalDateTime.of(2026, 4, 21, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx-empty-events")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx-empty-events", 2)).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-201"))
                .thenReturn(Optional.of(
                        ChatbotPlatformContext.builder()
                                .engagementLetterId("EL-201")
                                .ownerDisplayName("Ana")
                                .procedureTitles(List.of())
                                .recentEventSummaries(List.of())
                                .sourcesSummary(List.of("Hoja de encargo EL-201"))
                                .build()
                ));
        when(chatbotQuestionClassifier.classify("Que hitos tiene mi caso"))
                .thenReturn(PlatformQuestionType.TIMELINE_EVENTS);

        ChatbotMessageCommand request =
                new ChatbotMessageCommand("conversation-ctx-empty-events", "Que hitos tiene mi caso");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(response.getUsedPlatformData()).isTrue();
        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.CLIENT_CONTEXTUAL_NO_EVENTS_REPLY);
    }

    @Test
    void sendMessageShouldReturnDocumentsReplyWithProceduresWhenContextExists() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx-docs")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-300")
                .createdAt(LocalDateTime.of(2026, 4, 21, 11, 0))
                .build();

        when(conversationPersistence.readById("conversation-ctx-docs")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx-docs", 2)).thenReturn(7);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-300"))
                .thenReturn(Optional.of(
                        ChatbotPlatformContext.builder()
                                .engagementLetterId("EL-300")
                                .ownerDisplayName("Ana Ocaña")
                                .procedureTitles(List.of("Procedimiento laboral"))
                                .recentEventSummaries(List.of())
                                .sourcesSummary(List.of("Hoja de encargo EL-300"))
                                .build()
                ));
        when(chatbotQuestionClassifier.classify("¿Qué documentos hay en este caso?"))
                .thenReturn(PlatformQuestionType.DOCUMENTS);

        ChatbotMessageCommand request =
                new ChatbotMessageCommand("conversation-ctx-docs", "¿Qué documentos hay en este caso?");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(response.getUsedPlatformData()).isTrue();
        assertThat(response.getMessage()).contains("documentación del caso");
        assertThat(response.getMessage()).contains("Procedimiento laboral");
    }

    @Test
    void sendMessageShouldAppendVisibleDocumentsForClientContextualConversation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx-docs-visible")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .engagementLetterId("EL-301")
                .createdAt(LocalDateTime.of(2026, 4, 21, 11, 0))
                .build();

        when(conversationPersistence.readById("conversation-ctx-docs-visible")).thenReturn(existingConversation);
        when(conversationPersistence.reserveSequenceNumbers("conversation-ctx-docs-visible", 2)).thenReturn(9);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotPlatformContextService.loadContext("EL-301"))
                .thenReturn(Optional.of(
                        ChatbotPlatformContext.builder()
                                .engagementLetterId("EL-301")
                                .ownerDisplayName("Ana")
                                .procedureTitles(List.of())
                                .recentEventSummaries(List.of())
                                .sourcesSummary(List.of("Hoja de encargo EL-301"))
                                .build()
                ));
        when(chatbotQuestionClassifier.classify("Que documentos veo en mi caso"))
                .thenReturn(PlatformQuestionType.DOCUMENTS);
        when(chatbotDocumentContextService.loadDocumentContext(existingConversation))
                .thenReturn(ChatbotDocumentContext.builder()
                        .available(true)
                        .authorizedSourceConfigured(true)
                        .visibleDocumentTitles(List.of("Contrato", "Poder"))
                        .sourcesSummary(List.of("Repositorio documental"))
                        .build());

        ChatbotMessageCommand request =
                new ChatbotMessageCommand("conversation-ctx-docs-visible", "Que documentos veo en mi caso");

        var response = chatbotService.sendMessage(this.authenticatedUser, request);

        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(response.getUsedPlatformData()).isTrue();
        assertThat(response.getMessage()).contains(ChatbotResponseMessages.CLIENT_CONTEXTUAL_DOCUMENTS_STUB_REPLY);
        assertThat(response.getMessage()).contains("Documentos visibles preparados");
        assertThat(response.getMessage()).contains("Contrato");
        assertThat(response.getMessage()).contains("Poder");
    }

    @Test
    void closeConversationShouldCloseOwnedActiveConversation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-1")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-1")).thenReturn(existingConversation);

        chatbotService.closeConversation(this.authenticatedUser, "conversation-1");

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
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-1")).thenReturn(existingConversation);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> chatbotService.closeConversation(this.authenticatedUser, "conversation-1")
        );

        assertThat(exception).hasMessageContaining("No tienes permisos sobre esta conversacion");
        verify(conversationPersistence, never()).update(any(Conversation.class));
    }

    @Test
    void closeConversationShouldIgnoreNonActiveConversation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-1")
                .userId("customer-1")
                .status(ConversationStatus.CLOSED)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-1")).thenReturn(existingConversation);

        chatbotService.closeConversation(this.authenticatedUser, "conversation-1");

        assertThat(existingConversation.getStatus()).isEqualTo(ConversationStatus.CLOSED);
        verify(conversationPersistence, never()).update(any(Conversation.class));
    }

    @Test
    void escalateConversationShouldArchiveConversationAndCreateEscalation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-escalate")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(this.conversationPersistence.readById("conversation-escalate")).thenReturn(existingConversation);

        this.chatbotService.escalateConversation(this.authenticatedUser, "conversation-escalate");

        assertThat(existingConversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        verify(this.conversationPersistence, never()).update(existingConversation);
        verify(this.escalationPersistence).createAndArchiveConversation(eq(existingConversation), any());
    }

    @Test
    void deleteConversationShouldDeleteOwnedConversationAndItsMessages() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-delete")
                .userId("customer-1")
                .status(ConversationStatus.CLOSED)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-delete")).thenReturn(existingConversation);

        chatbotService.deleteConversation(this.authenticatedUser, "conversation-delete");

        verify(messagePersistence).deleteByConversationId("conversation-delete");
        verify(conversationPersistence).delete("conversation-delete");
    }

    @Test
    void deleteConversationShouldRejectOtherUsersConversation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-delete")
                .userId("customer-2")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-delete")).thenReturn(existingConversation);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> chatbotService.deleteConversation(this.authenticatedUser, "conversation-delete")
        );

        assertThat(exception).hasMessageContaining("No tienes permisos sobre esta conversacion");
        verify(messagePersistence, never()).deleteByConversationId(any());
        verify(conversationPersistence, never()).delete(any());
    }

    @Test
    void sendMessageShouldRejectClosedConversation() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-closed")
                .userId("professional-1")
                .status(ConversationStatus.CLOSED)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 21, 11, 30))
                .build();
        when(conversationPersistence.readById("conversation-closed")).thenReturn(existingConversation);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> chatbotService.sendMessage(this.authenticatedUser, new ChatbotMessageCommand("conversation-closed", "Hola"))
        );

        assertThat(exception).hasMessageContaining("La conversacion no esta activa");
        verify(messagePersistence, never()).createAndReturnId(any(Message.class));
        verify(chatbotScopePolicy, never()).evaluate(any(), any());
    }

    @Test
    void reopenConversationShouldReopenClosedConversation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-closed")
                .userId("customer-1")
                .status(ConversationStatus.CLOSED)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-closed")).thenReturn(existingConversation);

        chatbotService.reopenConversation(this.authenticatedUser, "conversation-closed");

        assertThat(existingConversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        verify(conversationPersistence).update(existingConversation);
    }

    @Test
    void reopenConversationShouldNotUpdateWhenConversationIsAlreadyActive() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-active")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-active")).thenReturn(existingConversation);

        chatbotService.reopenConversation(this.authenticatedUser, "conversation-active");

        verify(conversationPersistence, never()).update(any(Conversation.class));
    }

    @Test
    void reopenConversationShouldRejectArchivedConversation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-archived")
                .userId("customer-1")
                .status(ConversationStatus.ARCHIVED)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-archived")).thenReturn(existingConversation);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> chatbotService.reopenConversation(this.authenticatedUser, "conversation-archived")
        );

        assertThat(exception).hasMessageContaining("La conversacion archivada no se puede reabrir");
        verify(conversationPersistence, never()).update(any(Conversation.class));
    }

    @Test
    void readConfigurationStatusShouldExposeConfiguredAiSettings() {
        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.chatbotAiSettings.provider()).thenReturn("openai");
        when(this.chatbotAiSettings.model()).thenReturn("gpt-4.1-mini");
        when(this.chatbotAiSettings.maxInputCharacters()).thenReturn(2048);
        when(this.chatbotAiSettings.maxOutputTokens()).thenReturn(800);
        when(this.chatbotAiSettings.maxContextMessages()).thenReturn(6);
        when(this.chatbotAiSettings.documentsAvailable()).thenReturn(true);

        ChatbotConfigurationResult response = this.chatbotService.readConfigurationStatus();

        assertThat(response.isEnabled()).isTrue();
        assertThat(response.getProvider()).isEqualTo("openai");
        assertThat(response.getModel()).isEqualTo("gpt-4.1-mini");
        assertThat(response.getMaxInputCharacters()).isEqualTo(2048);
        assertThat(response.getMaxOutputTokens()).isEqualTo(800);
        assertThat(response.getMaxContextMessages()).isEqualTo(6);
        assertThat(response.isDocumentsAvailable()).isTrue();
    }

    @Test
    void sendMessageShouldSanitizeAiReplyBeforePersistingAssistantMessage() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ai-table")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.conversationPersistence.readById("conversation-ai-table")).thenReturn(existingConversation);
        when(this.conversationPersistence.reserveSequenceNumbers("conversation-ai-table", 2)).thenReturn(3);
        when(this.messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(this.messagePersistence.findByConversationIdOrdered("conversation-ai-table"))
                .thenReturn(List.of());
        when(this.chatbotQuestionClassifier.classify("Muéstramelo en tabla"))
                .thenReturn(PlatformQuestionType.GENERAL_CONTEXT);
        when(this.chatbotAiClient.generate(any()))
                .thenReturn(ChatbotAiResponse.builder()
                        .content("""
                                | Documento | Estado |
                                | --- | --- |
                                | Contrato | Firmado |
                                | Poder | Pendiente |
                                """)
                        .provider("ollama")
                        .model("llama3.2:3b")
                        .finishReason("SUCCESS")
                        .build());

        ChatbotMessageResult response = this.chatbotService.sendMessage(this.authenticatedUser, 
                new ChatbotMessageCommand("conversation-ai-table", "Muéstramelo en tabla")
        );

        assertThat(response.getMessage()).isEqualTo(String.join(
                System.lineSeparator(),
                "- Documento: Estado",
                "- Contrato: Firmado",
                "- Poder: Pendiente"
        ));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(this.messagePersistence, times(2)).createAndReturnId(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(1).getContent()).isEqualTo(response.getMessage());
    }

    private void authenticate(String userId, String... authorities) {
        boolean isCustomer = List.of(authorities).stream()
                .anyMatch("ROLE_CUSTOMER"::equals);

        this.authenticatedUser = AuthenticatedUserContext.builder()
                .userId(userId)
                .profile(isCustomer ? ConversationProfileType.CLIENT : ConversationProfileType.PROFESSIONAL)
                .build();
    }
}








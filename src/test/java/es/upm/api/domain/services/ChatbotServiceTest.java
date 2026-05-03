package es.upm.api.domain.services;

import es.upm.api.domain.enums.*;
import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.exceptions.ConflictException;
import es.upm.api.domain.exceptions.ForbiddenException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.Escalation;
import es.upm.api.domain.model.Message;
import es.upm.api.domain.model.UserDto;
import es.upm.api.domain.model.platform.ChatbotDocumentContext;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.persistence.ConversationPersistence;
import es.upm.api.domain.persistence.EscalationPersistence;
import es.upm.api.domain.persistence.MessagePersistence;
import es.upm.api.domain.services.policies.ChatbotScopeDecision;
import es.upm.api.domain.services.policies.ChatbotScopePolicy;
import es.upm.api.domain.services.support.ChatbotResponseMessages;
import es.upm.api.domain.webclients.UserWebClient;
import es.upm.api.infrastructure.dtos.ChatbotContextualConversationRequestDto;
import es.upm.api.infrastructure.dtos.ChatbotMessageRequestDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    private EscalationPersistence escalationPersistence;

    @Mock
    private ChatbotScopePolicy chatbotScopePolicy;

    @Mock
    private ChatbotDocumentContextService chatbotDocumentContextService;

    @Mock
    private ChatbotPlatformContextService chatbotPlatformContextService;

    @Mock
    private ChatbotQuestionClassifier chatbotQuestionClassifier;

    @Mock
    private UserWebClient userWebClient;

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
    void startGeneralConversationShouldPersistConversationAndMessagesForProfessional() {
        this.authenticate("professional-1", "ROLE_ADMIN");
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(null, "Necesito soporte");

        var response = chatbotService.startGeneralConversation(request);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messagePersistence, times(2)).createAndReturnId(messageCaptor.capture());
        List<Message> savedMessages = messageCaptor.getAllValues();

        assertThat(savedMessages.get(1).getContent()).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_GENERAL_START_REPLY);
        assertThat(response.getResponseMode()).isEqualTo("GENERAL");
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
                .type("CONTEXTUAL")
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.findActiveContextualConversation("customer-42", "EL-1", "CONTEXTUAL"))
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
    void startContextualConversationShouldCreateConversationWhenNoActiveConversationExists() {
        this.authenticate("customer-77", "ROLE_CUSTOMER");
        when(conversationPersistence.findActiveContextualConversation("customer-77", "EL-77", "CONTEXTUAL"))
                .thenReturn(Optional.empty());

        ChatbotContextualConversationRequestDto request = new ChatbotContextualConversationRequestDto();
        request.setEngagementLetterId("EL-77");

        var response = chatbotService.startContextualConversation(request);

        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationPersistence).create(conversationCaptor.capture());
        Conversation savedConversation = conversationCaptor.getValue();

        assertThat(savedConversation.getUserId()).isEqualTo("customer-77");
        assertThat(savedConversation.getEngagementLetterId()).isEqualTo("EL-77");
        assertThat(savedConversation.getType()).isEqualTo("CONTEXTUAL");
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
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 21, 9, 0))
                .build();
        Conversation olderConversation = Conversation.builder()
                .id("conversation-2")
                .userId("professional-1")
                .status(ConversationStatus.CLOSED)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 20, 9, 0))
                .build();

        when(conversationPersistence.findByUserIdAndTypeOrderByCreatedAtDesc("professional-1", "GENERAL"))
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

        var response = chatbotService.readConversationHistoryList(" general ", null);

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
                .type("CONTEXTUAL")
                .createdAt(LocalDateTime.of(2026, 4, 21, 12, 0))
                .build();

        when(conversationPersistence.findByUserIdAndEngagementLetterIdAndTypeOrderByCreatedAtDesc(
                "customer-1",
                "EL-9",
                "CONTEXTUAL"
        )).thenReturn(List.of(conversation));
        when(messagePersistence.findLatestByConversationId("conversation-ctx-1")).thenReturn(Optional.empty());

        var response = chatbotService.readConversationHistoryList(" contextual ", "EL-9");

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getConversationId()).isEqualTo("conversation-ctx-1");
        verify(conversationPersistence, never()).findByUserIdAndTypeOrderByCreatedAtDesc(any(), eq("CONTEXTUAL"));
    }

    @Test
    void readConversationHistoryListShouldRejectUnsupportedConversationType() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> chatbotService.readConversationHistoryList("other", null)
        );

        assertThat(exception).hasMessageContaining("type debe ser GENERAL o CONTEXTUAL");
        verify(conversationPersistence, never()).findByUserIdAndTypeOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void readConversationHistoryListShouldRequireConversationType() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> chatbotService.readConversationHistoryList("   ", null)
        );

        assertThat(exception).hasMessageContaining("type es obligatorio");
        verify(conversationPersistence, never()).findByUserIdAndTypeOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void readConversationHistoryListShouldRequireEngagementLetterIdForContextualType() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> chatbotService.readConversationHistoryList("CONTEXTUAL", " ")
        );

        assertThat(exception).hasMessageContaining("engagementLetterId es obligatorio");
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
                .type("CONTEXTUAL")
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
        when(messagePersistence.findByConversationIdOrderedDesc("conversation-history", 0, 10))
                .thenReturn(new PageImpl<>(
                        List.of(newestInPage, oldestInPage),
                        PageRequest.of(0, 10),
                        12
                ));

        var response = chatbotService.readConversationHistory("conversation-history", null, null);

        assertThat(response.getConversationId()).isEqualTo("conversation-history");
        assertThat(response.getEngagementLetterId()).isEqualTo("EL-10");
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getHasMore()).isTrue();
        assertThat(response.getTotalMessages()).isEqualTo(12);
        assertThat(response.getMessages()).hasSize(2);
        assertThat(response.getMessages().get(0).getId()).isEqualTo("message-1");
        assertThat(response.getMessages().get(0).getSequenceNumber()).isEqualTo(1);
        assertThat(response.getMessages().get(1).getId()).isEqualTo("message-2");
        assertThat(response.getMessages().get(1).getSequenceNumber()).isEqualTo(2);
    }

    @Test
    void readConversationHistoryShouldRejectNegativePage() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation conversation = Conversation.builder()
                .id("conversation-history")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 21, 8, 0))
                .build();
        when(conversationPersistence.readById("conversation-history")).thenReturn(conversation);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> chatbotService.readConversationHistory("conversation-history", -1, 10)
        );

        assertThat(exception).hasMessageContaining("page debe ser mayor o igual que 0");
        verify(messagePersistence, never()).findByConversationIdOrderedDesc(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void readConversationHistoryShouldRejectSizeOutsideSupportedRange() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation conversation = Conversation.builder()
                .id("conversation-history")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 21, 8, 0))
                .build();
        when(conversationPersistence.readById("conversation-history")).thenReturn(conversation);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> chatbotService.readConversationHistory("conversation-history", 0, 101)
        );

        assertThat(exception).hasMessageContaining("size debe estar entre 1 y 100");
        verify(messagePersistence, never()).findByConversationIdOrderedDesc(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void readConversationHistoryShouldRejectSizeBelowMinimum() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation conversation = Conversation.builder()
                .id("conversation-history")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 21, 8, 0))
                .build();
        when(conversationPersistence.readById("conversation-history")).thenReturn(conversation);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> chatbotService.readConversationHistory("conversation-history", 0, 0)
        );

        assertThat(exception).hasMessageContaining("size debe estar entre 1 y 100");
        verify(messagePersistence, never()).findByConversationIdOrderedDesc(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
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
    void sendMessageShouldMentionNoRecentEventsWhenContextHasNoVisibleEvents() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type("CONTEXTUAL")
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-ctx")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Dame contexto del caso")))
                .thenReturn(ChatbotScopeDecision.allow());
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

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto("conversation-ctx", "Dame contexto del caso");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_PLATFORM_DATA");
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
    void sendMessageShouldUseDocumentStubReplyWhenDocumentsIntegrationIsNotAvailable() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type("CONTEXTUAL")
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
        when(messagePersistence.nextSequenceNumber("conversation-ctx")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Que documentos hay en el expediente")))
                .thenReturn(ChatbotScopeDecision.allow());
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

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto("conversation-ctx", "Que documentos hay en el expediente");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_PLATFORM_DATA");
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
                .type("CONTEXTUAL")
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-ctx")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Dame contexto del caso")))
                .thenReturn(ChatbotScopeDecision.allow());
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

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto("conversation-ctx", "Dame contexto del caso");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_PLATFORM_DATA");
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
                .type("CONTEXTUAL")
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-ctx")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Cual es el estado de mi caso")))
                .thenReturn(ChatbotScopeDecision.allow());
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.empty());
        when(chatbotQuestionClassifier.classify("Cual es el estado de mi caso"))
                .thenReturn(PlatformQuestionType.ENGAGEMENT_STATUS);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto("conversation-ctx", "Cual es el estado de mi caso");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_RESTRICTED");
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
                .type("CONTEXTUAL")
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-ctx")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Que hitos recientes tiene el caso")))
                .thenReturn(ChatbotScopeDecision.allow());
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.empty());
        when(chatbotQuestionClassifier.classify("Que hitos recientes tiene el caso"))
                .thenReturn(PlatformQuestionType.TIMELINE_EVENTS);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto("conversation-ctx", "Que hitos recientes tiene el caso");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_RESTRICTED");
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
                .type("CONTEXTUAL")
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-ctx")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Que documentos hay en el expediente")))
                .thenReturn(ChatbotScopeDecision.allow());
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.empty());
        when(chatbotQuestionClassifier.classify("Que documentos hay en el expediente"))
                .thenReturn(PlatformQuestionType.DOCUMENTS);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto("conversation-ctx", "Que documentos hay en el expediente");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_RESTRICTED");
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
                .type("CONTEXTUAL")
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-ctx")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Dame un resumen del caso")))
                .thenReturn(ChatbotScopeDecision.allow());
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.empty());
        when(chatbotQuestionClassifier.classify("Dame un resumen del caso"))
                .thenReturn(PlatformQuestionType.GENERAL_CONTEXT);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto("conversation-ctx", "Dame un resumen del caso");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_RESTRICTED");
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
                .type("CONTEXTUAL")
                .engagementLetterId("EL-100")
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-ctx")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Dame contexto del caso")))
                .thenReturn(ChatbotScopeDecision.allow());
        when(chatbotPlatformContextService.loadContext("EL-100"))
                .thenReturn(Optional.empty());

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto("conversation-ctx", "Dame contexto del caso");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_RESTRICTED");
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
                .type("CONTEXTUAL")
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
        when(messagePersistence.nextSequenceNumber("conversation-ctx")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Cual es el estado del encargo")))
                .thenReturn(ChatbotScopeDecision.allow());
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.of(context));
        when(chatbotQuestionClassifier.classify("Cual es el estado del encargo"))
                .thenReturn(PlatformQuestionType.ENGAGEMENT_STATUS);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto("conversation-ctx", "Cual es el estado del encargo");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_PLATFORM_DATA");
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
                .type("CONTEXTUAL")
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
        when(messagePersistence.nextSequenceNumber("conversation-ctx")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Que hitos recientes tiene el caso")))
                .thenReturn(ChatbotScopeDecision.allow());
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.of(context));
        when(chatbotQuestionClassifier.classify("Que hitos recientes tiene el caso"))
                .thenReturn(PlatformQuestionType.TIMELINE_EVENTS);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto("conversation-ctx", "Que hitos recientes tiene el caso");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_PLATFORM_DATA");
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
                .type("CONTEXTUAL")
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
        when(messagePersistence.nextSequenceNumber("conversation-ctx")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Que documentos hay en el caso")))
                .thenReturn(ChatbotScopeDecision.allow());
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.of(context));
        when(chatbotQuestionClassifier.classify("Que documentos hay en el caso"))
                .thenReturn(PlatformQuestionType.DOCUMENTS);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto("conversation-ctx", "Que documentos hay en el caso");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_PLATFORM_DATA");
        assertThat(response.getMessage()).contains(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_DOCUMENTS_STUB_REPLY);
    }

    @Test
    void sendMessageShouldReturnClientFriendlyStatusReplyForContextualQuestion() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type("CONTEXTUAL")
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
        when(messagePersistence.nextSequenceNumber("conversation-ctx")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Cual es el estado de mi caso")))
                .thenReturn(ChatbotScopeDecision.allow());
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.of(context));
        when(chatbotQuestionClassifier.classify("Cual es el estado de mi caso"))
                .thenReturn(PlatformQuestionType.ENGAGEMENT_STATUS);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto("conversation-ctx", "Cual es el estado de mi caso");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_PLATFORM_DATA");
        assertThat(response.getMessage()).contains("puedo darte una explicación más clara");
        assertThat(response.getMessage()).contains("procedimientos visibles relacionados");
    }

    @Test
    void sendMessageShouldReturnProfessionalDocumentReplyForContextualQuestion() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type("CONTEXTUAL")
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
        when(messagePersistence.nextSequenceNumber("conversation-ctx")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Que documentos hay en el expediente")))
                .thenReturn(ChatbotScopeDecision.allow());
        when(chatbotPlatformContextService.loadContext("EL-100")).thenReturn(Optional.of(context));
        when(chatbotQuestionClassifier.classify("Que documentos hay en el expediente"))
                .thenReturn(PlatformQuestionType.DOCUMENTS);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto("conversation-ctx", "Que documentos hay en el expediente");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_PLATFORM_DATA");
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
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 21, 10, 0))
                .build();

        when(conversationPersistence.readById("conversation-general")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-general")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq(userMessage)))
                .thenReturn(ChatbotScopeDecision.allow());
        when(chatbotQuestionClassifier.classify(userMessage))
                .thenReturn(PlatformQuestionType.ENGAGEMENT_STATUS);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(
                "conversation-general",
                userMessage
        );

        var response = chatbotService.sendMessage(request);

        assertThat(response.getConversationId()).isEqualTo("conversation-general");
        assertThat(response.getResponseMode()).isEqualTo("GENERAL");
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
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 21, 10, 0))
                .build();

        when(conversationPersistence.readById("conversation-general")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-general")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq(userMessage)))
                .thenReturn(ChatbotScopeDecision.allow());
        when(chatbotQuestionClassifier.classify(userMessage))
                .thenReturn(PlatformQuestionType.TIMELINE_EVENTS);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(
                "conversation-general",
                userMessage
        );

        var response = chatbotService.sendMessage(request);

        assertThat(response.getConversationId()).isEqualTo("conversation-general");
        assertThat(response.getResponseMode()).isEqualTo("GENERAL");
        assertThat(response.getUsedPlatformData()).isFalse();
        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_GENERAL_TIMELINE_REPLY);
    }

    @Test
    void sendMessageShouldReturnGeneralDocumentsFaqForProfessional() {
        this.authenticate("professional-1", "ROLE_ADMIN");
        String userMessage = "Que documentos aplican aqui";

        Conversation existingConversation = Conversation.builder()
                .id("conversation-general")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 21, 10, 0))
                .build();

        when(conversationPersistence.readById("conversation-general")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-general")).thenReturn(5);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq(userMessage)))
                .thenReturn(ChatbotScopeDecision.allow());
        when(chatbotQuestionClassifier.classify(userMessage))
                .thenReturn(PlatformQuestionType.DOCUMENTS);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(
                "conversation-general",
                userMessage
        );

        var response = chatbotService.sendMessage(request);

        assertThat(response.getConversationId()).isEqualTo("conversation-general");
        assertThat(response.getResponseMode()).isEqualTo("GENERAL");
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
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 21, 10, 0))
                .build();

        when(conversationPersistence.readById("conversation-general")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-general")).thenReturn(7);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq(userMessage)))
                .thenReturn(ChatbotScopeDecision.allow());
        when(chatbotQuestionClassifier.classify(userMessage))
                .thenReturn(null);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(
                "conversation-general",
                userMessage
        );

        var response = chatbotService.sendMessage(request);

        assertThat(response.getConversationId()).isEqualTo("conversation-general");
        assertThat(response.getResponseMode()).isEqualTo("GENERAL");
        assertThat(response.getUsedPlatformData()).isFalse();
        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_GENERAL_CONTEXT_REPLY);
    }

    @Test
    void sendMessageShouldReturnMissingCaseContextWhenCustomerAsksForOwnEngagementStatusInGeneralConversation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        String userMessage = "¿Cuál es el estado de mi encargo?";

        Conversation existingConversation = Conversation.builder()
                .id("conversation-general")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 21, 10, 0))
                .build();

        when(conversationPersistence.readById("conversation-general")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-general")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq(userMessage)))
                .thenReturn(ChatbotScopeDecision.reject(
                        ChatbotScopeViolationReason.MISSING_CASE_CONTEXT,
                        ChatbotResponseMessages.MISSING_CASE_CONTEXT_REPLY,
                        false
                ));

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(
                "conversation-general",
                userMessage
        );

        var response = chatbotService.sendMessage(request);

        assertThat(response.getConversationId()).isEqualTo("conversation-general");
        assertThat(response.getResponseMode()).isEqualTo("GENERAL");
        assertThat(response.getUsedPlatformData()).isFalse();
        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.MISSING_CASE_CONTEXT_REPLY);
        verify(chatbotQuestionClassifier, never()).classify(any());
    }

    @Test
    void sendMessageShouldReturnTimelineReplyWithEventsAndProceduresWhenContextExists() {
        this.authenticate("professional-1", "ROLE_ADMIN");

        Conversation existingConversation = Conversation.builder()
                .id("conversation-ctx-events")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type("CONTEXTUAL")
                .engagementLetterId("EL-200")
                .createdAt(LocalDateTime.of(2026, 4, 21, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx-events")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-ctx-events")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("¿Qué hitos recientes tiene este caso?")))
                .thenReturn(ChatbotScopeDecision.allow());
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

        ChatbotMessageRequestDto request =
                new ChatbotMessageRequestDto("conversation-ctx-events", "¿Qué hitos recientes tiene este caso?");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_PLATFORM_DATA");
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
                .type("CONTEXTUAL")
                .engagementLetterId("EL-201")
                .createdAt(LocalDateTime.of(2026, 4, 21, 10, 30))
                .build();

        when(conversationPersistence.readById("conversation-ctx-empty-events")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-ctx-empty-events")).thenReturn(3);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Que hitos tiene mi caso")))
                .thenReturn(ChatbotScopeDecision.allow());
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

        ChatbotMessageRequestDto request =
                new ChatbotMessageRequestDto("conversation-ctx-empty-events", "Que hitos tiene mi caso");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_PLATFORM_DATA");
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
                .type("CONTEXTUAL")
                .engagementLetterId("EL-300")
                .createdAt(LocalDateTime.of(2026, 4, 21, 11, 0))
                .build();

        when(conversationPersistence.readById("conversation-ctx-docs")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-ctx-docs")).thenReturn(7);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("¿Qué documentos hay en este caso?")))
                .thenReturn(ChatbotScopeDecision.allow());
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

        ChatbotMessageRequestDto request =
                new ChatbotMessageRequestDto("conversation-ctx-docs", "¿Qué documentos hay en este caso?");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_PLATFORM_DATA");
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
                .type("CONTEXTUAL")
                .engagementLetterId("EL-301")
                .createdAt(LocalDateTime.of(2026, 4, 21, 11, 0))
                .build();

        when(conversationPersistence.readById("conversation-ctx-docs-visible")).thenReturn(existingConversation);
        when(messagePersistence.nextSequenceNumber("conversation-ctx-docs-visible")).thenReturn(9);
        when(messagePersistence.createAndReturnId(any(Message.class)))
                .thenReturn("user-message-id", "assistant-message-id");
        when(chatbotScopePolicy.evaluate(eq(existingConversation), eq("Que documentos veo en mi caso")))
                .thenReturn(ChatbotScopeDecision.allow());
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

        ChatbotMessageRequestDto request =
                new ChatbotMessageRequestDto("conversation-ctx-docs-visible", "Que documentos veo en mi caso");

        var response = chatbotService.sendMessage(request);

        assertThat(response.getResponseMode()).isEqualTo("CONTEXTUAL_PLATFORM_DATA");
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
    void closeConversationShouldIgnoreNonActiveConversation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-1")
                .userId("customer-1")
                .status(ConversationStatus.CLOSED)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-1")).thenReturn(existingConversation);

        chatbotService.closeConversation("conversation-1");

        assertThat(existingConversation.getStatus()).isEqualTo(ConversationStatus.CLOSED);
        verify(conversationPersistence, never()).update(any(Conversation.class));
    }

    @Test
    void escalateConversationShouldArchiveOwnedActiveConversationAndCreateEscalation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-escalate")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-escalate")).thenReturn(existingConversation);
        when(userWebClient.readById("customer-1")).thenReturn(
                UserDto.builder()
                        .id(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"))
                        .mobile("+34600111222")
                        .email("customer1@example.com")
                        .firstName("Customer")
                        .familyName("One")
                        .build()
        );

        chatbotService.escalateConversation("conversation-escalate");

        assertThat(existingConversation.getStatus()).isEqualTo(ConversationStatus.ARCHIVED);
        verify(conversationPersistence).update(existingConversation);

        ArgumentCaptor<Escalation> escalationCaptor = ArgumentCaptor.forClass(Escalation.class);
        verify(escalationPersistence).create(escalationCaptor.capture());
        Escalation escalation = escalationCaptor.getValue();

        assertThat(escalation.getConversationId()).isEqualTo("conversation-escalate");
        assertThat(escalation.getUserId()).isEqualTo("customer-1");
        assertThat(escalation.getCreatedAt()).isNotNull();
        assertThat(escalation.getPhone()).isEqualTo("+34600111222");
        assertThat(escalation.getEmail()).isEqualTo("customer1@example.com");
    }

    @Test
    void escalateConversationShouldCreateEscalationWithoutContactDataWhenUserLookupFails() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-escalate")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-escalate")).thenReturn(existingConversation);
        when(userWebClient.readById("customer-1")).thenThrow(new RuntimeException("user service unavailable"));

        chatbotService.escalateConversation("conversation-escalate");

        ArgumentCaptor<Escalation> escalationCaptor = ArgumentCaptor.forClass(Escalation.class);
        verify(escalationPersistence).create(escalationCaptor.capture());
        Escalation escalation = escalationCaptor.getValue();

        assertThat(escalation.getPhone()).isNull();
        assertThat(escalation.getEmail()).isNull();
    }

    @Test
    void escalateConversationShouldRejectOtherUsersConversation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-escalate")
                .userId("customer-2")
                .status(ConversationStatus.ACTIVE)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-escalate")).thenReturn(existingConversation);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> chatbotService.escalateConversation("conversation-escalate")
        );

        assertThat(exception).hasMessageContaining("No tienes permisos sobre esta conversacion");
        verify(conversationPersistence, never()).update(any(Conversation.class));
        verify(escalationPersistence, never()).create(any(Escalation.class));
    }

    @Test
    void deleteConversationShouldDeleteOwnedConversationAndItsMessages() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-delete")
                .userId("customer-1")
                .status(ConversationStatus.CLOSED)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-delete")).thenReturn(existingConversation);

        chatbotService.deleteConversation("conversation-delete");

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
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-delete")).thenReturn(existingConversation);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> chatbotService.deleteConversation("conversation-delete")
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
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 21, 11, 30))
                .build();
        when(conversationPersistence.readById("conversation-closed")).thenReturn(existingConversation);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> chatbotService.sendMessage(new ChatbotMessageRequestDto("conversation-closed", "Hola"))
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
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-closed")).thenReturn(existingConversation);

        chatbotService.reopenConversation("conversation-closed");

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
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-active")).thenReturn(existingConversation);

        chatbotService.reopenConversation("conversation-active");

        verify(conversationPersistence, never()).update(any(Conversation.class));
    }

    @Test
    void reopenConversationShouldRejectArchivedConversation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation existingConversation = Conversation.builder()
                .id("conversation-archived")
                .userId("customer-1")
                .status(ConversationStatus.ARCHIVED)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 19, 13, 0))
                .build();
        when(conversationPersistence.readById("conversation-archived")).thenReturn(existingConversation);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> chatbotService.reopenConversation("conversation-archived")
        );

        assertThat(exception).hasMessageContaining("La conversacion archivada no se puede reabrir");
        verify(conversationPersistence, never()).update(any(Conversation.class));
    }

    private void authenticate(String userId, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(userId, "password", authorities)
        );
    }
}

package es.upm.api.domain.services;

import es.upm.api.domain.enums.*;
import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.exceptions.ConflictException;
import es.upm.api.domain.exceptions.ForbiddenException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.Message;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
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

    @Mock
    private ChatbotPlatformContextService chatbotPlatformContextService;

    @Mock
    private ChatbotQuestionClassifier chatbotQuestionClassifier;

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
        assertThat(response.getMessage()).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_DOCUMENTS_REPLY);
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
        assertThat(response.getMessage()).contains("documentación del caso");
        assertThat(response.getMessage()).contains("no debo inventar documentos");
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
        assertThat(response.getMessage()).contains("documentación del caso");
        assertThat(response.getMessage()).contains("sin inventar documentos");
    }

    @Test
    void readUserConversationsShouldReturnAuthenticatedUserConversations() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation firstConversation = Conversation.builder()
                .id("conversation-1")
                .userId("customer-1")
                .engagementLetterId("EL-1")
                .status(ConversationStatus.ACTIVE)
                .type("CONTEXTUAL")
                .createdAt(LocalDateTime.of(2026, 4, 20, 9, 0))
                .build();
        Conversation secondConversation = Conversation.builder()
                .id("conversation-2")
                .userId("customer-1")
                .status(ConversationStatus.CLOSED)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 19, 18, 30))
                .build();
        when(conversationPersistence.findByUserId("customer-1"))
                .thenReturn(List.of(firstConversation, secondConversation));

        var response = chatbotService.readUserConversations();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getConversationId()).isEqualTo("conversation-1");
        assertThat(response.get(0).getUserId()).isEqualTo("customer-1");
        assertThat(response.get(0).getEngagementLetterId()).isEqualTo("EL-1");
        assertThat(response.get(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(response.get(0).getType()).isEqualTo("CONTEXTUAL");
        assertThat(response.get(0).getCreatedAt()).isEqualTo("2026-04-20T09:00");
        assertThat(response.get(1).getConversationId()).isEqualTo("conversation-2");
        assertThat(response.get(1).getStatus()).isEqualTo("CLOSED");
    }

    @Test
    void readConversationShouldReturnOwnedConversationById() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .userId("customer-1")
                .engagementLetterId("EL-1")
                .status(ConversationStatus.CLOSED)
                .type("CONTEXTUAL")
                .createdAt(LocalDateTime.of(2026, 4, 20, 9, 0))
                .build();
        when(conversationPersistence.readById("conversation-1")).thenReturn(conversation);

        var response = chatbotService.readConversation("conversation-1");

        assertThat(response.getConversationId()).isEqualTo("conversation-1");
        assertThat(response.getUserId()).isEqualTo("customer-1");
        assertThat(response.getEngagementLetterId()).isEqualTo("EL-1");
        assertThat(response.getStatus()).isEqualTo("CLOSED");
        assertThat(response.getType()).isEqualTo("CONTEXTUAL");
        assertThat(response.getCreatedAt()).isEqualTo("2026-04-20T09:00");
    }

    @Test
    void readConversationShouldRejectOtherUsersConversation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .userId("customer-2")
                .status(ConversationStatus.ACTIVE)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 20, 9, 0))
                .build();
        when(conversationPersistence.readById("conversation-1")).thenReturn(conversation);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> chatbotService.readConversation("conversation-1")
        );

        assertThat(exception).hasMessageContaining("No tienes permisos sobre esta conversacion");
    }

    @Test
    void readConversationMessagesShouldReturnOrderedMessagesForOwnedConversation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .userId("customer-1")
                .status(ConversationStatus.CLOSED)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 20, 9, 0))
                .build();
        Message firstMessage = Message.builder()
                .id("message-1")
                .conversationId("conversation-1")
                .senderType(MessageSenderType.USER)
                .messageType(MessageType.REQUEST)
                .content("Hola")
                .timestamp(LocalDateTime.of(2026, 4, 20, 9, 1))
                .sequenceNumber(1)
                .build();
        Message secondMessage = Message.builder()
                .id("message-2")
                .conversationId("conversation-1")
                .senderType(MessageSenderType.ASSISTANT)
                .messageType(MessageType.RESPONSE)
                .content("Buenos dias")
                .timestamp(LocalDateTime.of(2026, 4, 20, 9, 2))
                .sequenceNumber(2)
                .parentMessageId("message-1")
                .build();
        when(conversationPersistence.readById("conversation-1")).thenReturn(conversation);
        when(messagePersistence.findByConversationId("conversation-1")).thenReturn(List.of(firstMessage, secondMessage));

        var response = chatbotService.readConversationMessages("conversation-1");

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getMessageId()).isEqualTo("message-1");
        assertThat(response.get(0).getSenderType()).isEqualTo("USER");
        assertThat(response.get(0).getMessageType()).isEqualTo("REQUEST");
        assertThat(response.get(0).getTimestamp()).isEqualTo("2026-04-20T09:01");
        assertThat(response.get(1).getMessageId()).isEqualTo("message-2");
        assertThat(response.get(1).getParentMessageId()).isEqualTo("message-1");
    }

    @Test
    void readConversationMessagesShouldRejectOtherUsersConversation() {
        this.authenticate("customer-1", "ROLE_CUSTOMER");
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .userId("customer-2")
                .status(ConversationStatus.ACTIVE)
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 4, 20, 9, 0))
                .build();
        when(conversationPersistence.readById("conversation-1")).thenReturn(conversation);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> chatbotService.readConversationMessages("conversation-1")
        );

        assertThat(exception).hasMessageContaining("No tienes permisos sobre esta conversacion");
        verify(messagePersistence, never()).findByConversationId(any());
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

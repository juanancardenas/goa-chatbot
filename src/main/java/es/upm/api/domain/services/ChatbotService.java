package es.upm.api.domain.services;

import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.Message;
import es.upm.api.domain.persistence.ConversationPersistence;
import es.upm.api.domain.persistence.MessagePersistence;
import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.infrastructure.dtos.ChatbotContextualConversationRequestDto;
import es.upm.api.infrastructure.dtos.ChatbotContextualConversationResponseDto;
import es.upm.api.infrastructure.dtos.ChatbotMessageRequestDto;
import es.upm.api.infrastructure.dtos.ChatbotMessageResponseDto;
import es.upm.api.domain.services.policies.ChatbotScopeDecision;
import es.upm.api.domain.services.policies.ChatbotScopePolicy;
import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.exceptions.ConflictException;
import es.upm.api.domain.exceptions.ForbiddenException;
import es.upm.api.domain.services.support.ChatbotResponseMessages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class ChatbotService {
    // Constants
    private static final String TYPE_CONTEXTUAL = "CONTEXTUAL";
    private static final String TYPE_GENERAL = "GENERAL";

    private final ChatbotScopePolicy chatbotScopePolicy;

    // Attributes
    private final ConversationPersistence conversationPersistence;
    private final MessagePersistence messagePersistence;

    // Constructores
    @Autowired
    public ChatbotService(ConversationPersistence conversationPersistence,
                          MessagePersistence messagePersistence,
                          ChatbotScopePolicy chatbotScopePolicy
      //                    MessageRepository messageRepository
    ) {
        this.conversationPersistence = conversationPersistence;
        this.messagePersistence = messagePersistence;
        this.chatbotScopePolicy = chatbotScopePolicy;
    }

    // Starts Contextual Conversation, this type of conversation is receiving an EngagementLetter ID
    public ChatbotContextualConversationResponseDto startContextualConversation(
            ChatbotContextualConversationRequestDto requestDto
    ) {
        String userId = this.authenticatedUserId();
        Conversation conversation = this.findOrCreateContextualConversation(userId, requestDto.getEngagementLetterId());

        return new ChatbotContextualConversationResponseDto(
                conversation.getId(),
                conversation.getEngagementLetterId(),
                conversation.getCreatedAt().toString(),
                null
        );
    }

    private Conversation findOrCreateContextualConversation(String userId, String engagementLetterId) {
        return this.conversationPersistence
                .findContextualConversation(userId, engagementLetterId, TYPE_CONTEXTUAL)
                .orElseGet(() -> {
                    Conversation conversation = Conversation.builder()
                            .id(UUID.randomUUID().toString())
                            .userId(userId)
                            .engagementLetterId(engagementLetterId)
                            .status(ConversationStatus.ACTIVE)
                            .type(TYPE_CONTEXTUAL)
                            .createdAt(LocalDateTime.now())
                            .build();

                    this.conversationPersistence.create(conversation);
                    return conversation;
                });
    }

    // Starts General Conversation, this type of conversation is not linked to other process or entity
    public ChatbotMessageResponseDto startGeneralConversation(
            ChatbotMessageRequestDto requestDto
    ) {
        // Se crea la conversación
        String userId = this.authenticatedUserId();
        LocalDateTime date = LocalDateTime.now();

        Conversation conversation = Conversation.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .status(ConversationStatus.ACTIVE)
                .type(TYPE_GENERAL)
                .createdAt(date)
                .build();

        // Se persiste la conversación
        this.conversationPersistence.create(conversation);

        // Se crea el mensaje inicial y se recupera su Id
        String messageId = this.saveMessage(
                conversation.getId(),
                MessageSenderType.USER,
                MessageType.REQUEST,
                requestDto.getMessage(),
                1,
                null,
                date
        );

        ConversationProfile profile = this.resolveConversationProfile();
        String assistantReply = this.generalStartReply(profile);

        this.saveMessage(
                conversation.getId(),
                MessageSenderType.ASSISTANT,
                MessageType.RESPONSE,
                assistantReply,
                2,
                messageId,
                date
        );

        return new ChatbotMessageResponseDto(
                conversation.getId(),
                assistantReply,
                null,
                date.toString()
        );
    }

    // Starts General Conversation, this type of conversation is not linked to other process or entity
    public ChatbotMessageResponseDto sendMessage(
            ChatbotMessageRequestDto requestDto
    ) {
        String userId = this.authenticatedUserId();
        LocalDateTime date = LocalDateTime.now();

        if (requestDto.getConversationId() == null || requestDto.getConversationId().isBlank()) {
            throw new BadRequestException("conversationId es obligatorio para enviar mensajes");
        }

        Conversation conversation = this.requireActiveOwnedConversation(
                requestDto.getConversationId(),
                userId
        );

        Integer nextSequence = this.nextSequenceNumber(conversation.getId());

        String messageId = this.saveMessage(
                conversation.getId(),
                MessageSenderType.USER,
                MessageType.REQUEST,
                requestDto.getMessage(),
                nextSequence,
                null,
                date
        );

        ChatbotScopeDecision scopeDecision = this.chatbotScopePolicy.evaluate(
                conversation,
                requestDto.getMessage()
        );

        String assistantReply;
        if (scopeDecision.isAllowed()) {
            ConversationProfile profile = this.resolveConversationProfile();
            assistantReply = this.messageReply(profile);
        } else {
            assistantReply = scopeDecision.getSafeMessage();
        }

        this.saveMessage(
                conversation.getId(),
                MessageSenderType.ASSISTANT,
                MessageType.RESPONSE,
                assistantReply,
                nextSequence + 1,
                messageId,
                date
        );

        return new ChatbotMessageResponseDto(
                conversation.getId(),
                assistantReply,
                null,
                date.toString()
        );
    }

    public void closeConversation(String conversationId) {
        Conversation conversation = this.requireActiveOwnedConversation(
                conversationId,
                this.authenticatedUserId()
        );
        conversation.setStatus(ConversationStatus.CLOSED);
        this.conversationPersistence.update(conversation);
    }

    // Crea un mensaje y devuelve su ID de BD
    private String saveMessage(
            String conversationId,
            MessageSenderType senderType,
            MessageType messageType,
            String content,
            Integer sequenceNumber,
            String parentMessageId,
            LocalDateTime timestamp
    ) {
        return this.messagePersistence.createAndReturnId(
                Message.builder()
                        .id(UUID.randomUUID().toString())
                        .conversationId(conversationId)
                        .senderType(senderType)
                        .messageType(messageType)
                        .content(content)
                        .timestamp(timestamp)
                        .sequenceNumber(sequenceNumber)
                        .parentMessageId(parentMessageId)
                        .build()
        );
    }

    private Conversation requireActiveOwnedConversation(
            String conversationId, String userId
    ) {
        Conversation conversation = this.conversationPersistence.readById(conversationId);

        if (!userId.equals(conversation.getUserId())) {
            throw new ForbiddenException("No tienes permisos sobre esta conversacion");
        }

        if (conversation.getStatus() != ConversationStatus.ACTIVE) {
            throw new ConflictException("La conversacion no esta activa");
        }

        return conversation;
    }

    // Devuelve el siguiente secuencial
    private Integer nextSequenceNumber(String conversationId) {
        return this.messagePersistence.nextSequenceNumber(conversationId);
    }

    private String authenticatedUserId() {
        return this.currentAuthentication().getName();
    }

    private ConversationProfile resolveConversationProfile() {
        Authentication authentication = this.currentAuthentication();

        boolean isCustomer = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::normalizeAuthority)
                .anyMatch("CUSTOMER"::equals);

        return isCustomer ? ConversationProfile.CLIENT : ConversationProfile.PROFESSIONAL;
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private String normalizeAuthority(String authority) {
        if (authority == null) {
            return "";
        }
        return authority.replace("ROLE_", "").toUpperCase(Locale.ROOT);
    }

    private String generalStartReply(ConversationProfile profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_START_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_START_REPLY;
        };
    }

    private String messageReply(ConversationProfile profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_MESSAGE_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_MESSAGE_REPLY;
        };
    }

    private enum ConversationProfile {
        CLIENT,
        PROFESSIONAL
    }
}

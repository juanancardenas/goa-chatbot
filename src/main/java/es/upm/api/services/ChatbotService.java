package es.upm.api.services;

import es.upm.api.data.daos.ConversationRepository;
import es.upm.api.data.daos.MessageRepository;
import es.upm.api.data.entities.ConversationEntity;
import es.upm.api.data.enums.ConversationStatus;
import es.upm.api.data.entities.MessageEntity;
import es.upm.api.data.enums.MessageSenderType;
import es.upm.api.data.enums.MessageType;
import es.upm.api.resources.dtos.ChatbotContextualConversationRequestDto;
import es.upm.api.resources.dtos.ChatbotContextualConversationResponseDto;
import es.upm.api.resources.dtos.ChatbotMessageRequestDto;
import es.upm.api.resources.dtos.ChatbotMessageResponseDto;
import es.upm.api.services.exceptions.BadRequestException;
import es.upm.api.services.exceptions.ConflictException;
import es.upm.api.services.exceptions.ForbiddenException;
import es.upm.api.services.exceptions.NotFoundException;
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

    private static final String GENERAL_START_REPLY =
            "Conversación iniciada correctamente. ¿En qué puedo ayudarte?";
    private static final String GENERIC_ASSISTANT_REPLY =
            "He recibido tu mensaje. La integración con el asistente aún es simulada.";

    // Attributes
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    // Constructor
    public ChatbotService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    // Starts Contextual Conversation, this type of conversation is receiving an EngagementLetter ID
    public ChatbotContextualConversationResponseDto startContextualConversation(
            ChatbotContextualConversationRequestDto requestDto
    ) {
        String userId = this.authenticatedUserId();

        ConversationEntity conversation = this.conversationRepository
                .findByUserIdAndEngagementLetterIdAndType(
                        userId,
                        requestDto.getEngagementLetterId(),
                        TYPE_CONTEXTUAL
                )
                .orElseGet(() -> this.conversationRepository.save(
                        new ConversationEntity(
                                UUID.randomUUID().toString(),
                                userId,
                                requestDto.getEngagementLetterId(),
                                ConversationStatus.ACTIVE,
                                TYPE_CONTEXTUAL,
                                LocalDateTime.now()
                        )
                ));

        return new ChatbotContextualConversationResponseDto(
                conversation.getId(),
                conversation.getEngagementLetterId(),
                conversation.getCreatedAt().toString(),
                null
        );
    }

    // Starts General Conversation, this type of conversation is not linked to other process or entity
    public ChatbotMessageResponseDto startGeneralConversation(
            ChatbotMessageRequestDto requestDto
    ) {
        String userId = this.authenticatedUserId();
        LocalDateTime date = LocalDateTime.now();

        ConversationEntity conversation = this.conversationRepository.save(
                new ConversationEntity(
                        UUID.randomUUID().toString(),
                        userId,
                        null,
                        ConversationStatus.ACTIVE,
                        TYPE_GENERAL,
                        date
                )
        );

        MessageEntity userMessage = this.saveMessage(
                conversation.getId(),
                MessageSenderType.USER,
                MessageType.REQUEST,
                requestDto.getMessage(),
                1,
                null,
                date
        );

        String assistantReply = GENERAL_START_REPLY;

        this.saveMessage(
                conversation.getId(),
                MessageSenderType.ASSISTANT,
                MessageType.RESPONSE,
                assistantReply,
                2,
                userMessage.getId(),
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

        ConversationEntity conversation = this.requireActiveOwnedConversation(
                requestDto.getConversationId(),
                userId
        );

        Integer nextSequence = this.nextSequenceNumber(conversation.getId());

        MessageEntity userMessage = this.saveMessage(
                conversation.getId(),
                MessageSenderType.USER,
                MessageType.REQUEST,
                requestDto.getMessage(),
                nextSequence,
                null,
                date
        );

        ConversationProfile profile = this.resolveConversationProfile();
        String assistantReply = GENERIC_ASSISTANT_REPLY;

        this.saveMessage(
                conversation.getId(),
                MessageSenderType.ASSISTANT,
                MessageType.RESPONSE,
                assistantReply,
                nextSequence + 1,
                userMessage.getId(),
                date
        );

        return new ChatbotMessageResponseDto(
                conversation.getId(),
                assistantReply,
                null,
                date.toString()
        );
    }

    private ConversationEntity requireActiveOwnedConversation(
            String conversationId, String userId
    ) {
        ConversationEntity conversation = this.conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("conversationId no corresponde a una conversacion existente"));

        if (!userId.equals(conversation.getUserId())) {
            throw new ForbiddenException("No tienes permisos sobre esta conversacion");
        }

        if (conversation.getStatus() != ConversationStatus.ACTIVE) {
            throw new ConflictException("La conversacion no esta activa");
        }

        return conversation;
    }

    private Integer nextSequenceNumber(String conversationId) {
        return this.messageRepository
                .findFirstByConversationIdOrderBySequenceNumberDesc(conversationId)
                .map(message -> message.getSequenceNumber() + 1)
                .orElse(1);
    }

    private MessageEntity saveMessage(
            String conversationId,
            MessageSenderType senderType,
            MessageType messageType,
            String content,
            Integer sequenceNumber,
            String parentMessageId,
            LocalDateTime timestamp
    ) {
        return this.messageRepository.save(
                MessageEntity.builder()
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

    private enum ConversationProfile {
        CLIENT,
        PROFESSIONAL
    }

}

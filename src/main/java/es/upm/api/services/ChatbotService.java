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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ChatbotService {
    // Constants
    private static final String TYPE_CONTEXTUAL = "CONTEXTUAL";
    private static final String TYPE_GENERAL = "GENERAL";

    // Attributes
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    // Constructor
    public ChatbotService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository
    ) {
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
    public ChatbotMessageResponseDto startGeneralConversation(ChatbotMessageRequestDto requestDto) {
        String userId = this.authenticatedUserId();
        String text = requestDto.getMessage();
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

        this.messageRepository.save(
                MessageEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .conversationId(conversation.getId())
                        .senderType(MessageSenderType.USER)
                        .messageType(MessageType.REQUEST)
                        .content(text)
                        .timestamp(date)
                        .sequenceNumber(1)
                        .build()
        );

        return new ChatbotMessageResponseDto(
                conversation.getId(),
                text,
                null,
                date.toString()
        );
    }

    // Starts General Conversation, this type of conversation is not linked to other process or entity
    public ChatbotMessageResponseDto sendMessage(ChatbotMessageRequestDto requestDto) {

        String userId = this.authenticatedUserId();
        LocalDateTime date = LocalDateTime.now();
        ConversationEntity conversation = this.resolveConversation(requestDto.getConversationId(), userId, date);

        Integer nextSequenceNumber = this.messageRepository
                .findFirstByConversationIdOrderBySequenceNumberDesc(conversation.getId())
                .map(message -> message.getSequenceNumber() + 1)
                .orElse(1);

        this.messageRepository.save(
                MessageEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .conversationId(conversation.getId())
                        .senderType(MessageSenderType.USER)
                        .messageType(MessageType.REQUEST)
                        .content(requestDto.getMessage())
                        .timestamp(date)
                        .sequenceNumber(nextSequenceNumber)
                        .build()
        );

        return new ChatbotMessageResponseDto(
                conversation.getId(),
                requestDto.getMessage(),
                null,
                date.toString()
        );
    }

    // Whether persists a new conversation or returns its Id if already exists
    private ConversationEntity resolveConversation(String conversationId, String userId, LocalDateTime date) {

        if (conversationId == null || conversationId.isBlank()) {
            return this.conversationRepository.save(
                    new ConversationEntity(
                            UUID.randomUUID().toString(),
                            userId,
                            null,
                            ConversationStatus.ACTIVE,
                            TYPE_GENERAL,
                            date
                    )
            );
        }

        return this.conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BadRequestException("conversationId no corresponde a una conversacion existente"));
    }

    // Returns the name of the user from Authentication
    private String authenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

}

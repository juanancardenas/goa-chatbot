package es.upm.api.services;

import es.upm.api.data.daos.ConversationRepository;
import es.upm.api.data.daos.MessageRepository;
import es.upm.api.data.entities.ConversationEntity;
import es.upm.api.data.entities.ConversationStatus;
import es.upm.api.data.entities.MessageEntity;
import es.upm.api.data.entities.MessageSenderType;
import es.upm.api.data.entities.MessageType;
import es.upm.api.resources.dtos.ChatbotContextualConversationRequestDto;
import es.upm.api.resources.dtos.ChatbotContextualConversationResponseDto;
import es.upm.api.resources.dtos.ChatbotMessageRequestDto;
import es.upm.api.resources.dtos.ChatbotMessageResponseDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ChatbotService {

    private static final String TYPE_CONTEXTUAL = "CONTEXTUAL";
    private static final String TYPE_GENERAL = "GENERAL";

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ChatbotService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

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

        MessageEntity message = this.messageRepository.save(
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
                message.getContent(),
                null,
                message.getTimestamp().toString()
        );
    }

    public ChatbotMessageResponseDto sendMessage(ChatbotMessageRequestDto requestDto) {
        String conversationId = requestDto.getConversationId() != null && !requestDto.getConversationId().isBlank()
                ? requestDto.getConversationId()
                : UUID.randomUUID().toString();

        return new ChatbotMessageResponseDto(
                conversationId,
                "Respuesta simulada del asistente externo - testing",
                null,
                LocalDateTime.now().toString()
        );
    }

    private String authenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

}

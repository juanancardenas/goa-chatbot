package es.upm.api.services;

import es.upm.api.data.daos.ConversationRepository;
import es.upm.api.data.entities.ConversationEntity;
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

    private static final String CONTEXTUAL = "CONTEXTUAL";

    private final ConversationRepository conversationRepository;

    public ChatbotService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public ChatbotContextualConversationResponseDto startContextualConversation(
            ChatbotContextualConversationRequestDto requestDto
    ) {
        String userId = this.authenticatedUserId();

        ConversationEntity conversation = this.conversationRepository
                .findByUserIdAndEngagementLetterIdAndType(
                        userId,
                        requestDto.getEngagementLetterId(),
                        CONTEXTUAL
                )
                .orElseGet(() -> this.conversationRepository.save(
                        new ConversationEntity(
                                UUID.randomUUID().toString(),
                                userId,
                                requestDto.getEngagementLetterId(),
                                CONTEXTUAL,
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

    public ChatbotMessageResponseDto sendMessage(ChatbotMessageRequestDto requestDto) {
        String conversationId = requestDto.getConversationId() != null && !requestDto.getConversationId().isBlank()
                ? requestDto.getConversationId()
                : UUID.randomUUID().toString();

        return new ChatbotMessageResponseDto(
                conversationId,
                "Respuesta simulada del asistente externo",
                null,
                LocalDateTime.now().toString()
        );
    }

    private String authenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

}

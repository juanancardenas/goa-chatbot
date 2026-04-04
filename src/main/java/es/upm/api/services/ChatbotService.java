package es.upm.api.services;

import es.upm.api.resources.dtos.ChatbotMessageRequestDto;
import es.upm.api.resources.dtos.ChatbotMessageResponseDto;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ChatbotService {

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
}

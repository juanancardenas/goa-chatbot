package es.upm.api.infrastructure.dtos;

import es.upm.api.adapter.in.rest.dto.ChatbotMessageResponseDto;
import es.upm.api.domain.enums.ChatbotResponseMode;
import es.upm.api.domain.model.chatbot.result.ChatbotMessageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotMessageResponseDtoTest {

    @Test
    void fromDomainShouldExposeResponseModeAsText() {
        ChatbotMessageResult result = ChatbotMessageResult.builder()
                .conversationId("conversation-1")
                .message("Respuesta")
                .error(null)
                .createdAt("2026-05-16T15:00:00")
                .responseMode(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA)
                .usedPlatformData(true)
                .sourcesSummary(List.of("Hoja de encargo"))
                .build();

        ChatbotMessageResponseDto responseDto = ChatbotMessageResponseDto.fromDomain(result);

        assertThat(responseDto.getResponseMode()).isEqualTo("CONTEXTUAL_PLATFORM_DATA");
    }

    @Test
    void fromDomainShouldMapAllFields() {
        ChatbotMessageResult result = ChatbotMessageResult.builder()
                .conversationId("conversation-2")
                .message("Respuesta final")
                .error("warning")
                .createdAt("2026-05-16T16:00:00")
                .responseMode(ChatbotResponseMode.GENERAL)
                .usedPlatformData(false)
                .sourcesSummary(List.of("Fuente 1", "Fuente 2"))
                .build();

        ChatbotMessageResponseDto responseDto = ChatbotMessageResponseDto.fromDomain(result);

        assertThat(responseDto.getConversationId()).isEqualTo("conversation-2");
        assertThat(responseDto.getMessage()).isEqualTo("Respuesta final");
        assertThat(responseDto.getError()).isEqualTo("warning");
        assertThat(responseDto.getCreatedAt()).isEqualTo("2026-05-16T16:00:00");
        assertThat(responseDto.getResponseMode()).isEqualTo("GENERAL");
        assertThat(responseDto.getUsedPlatformData()).isFalse();
        assertThat(responseDto.getSourcesSummary()).containsExactly("Fuente 1", "Fuente 2");
    }

    @Test
    void fromDomainShouldKeepResponseModeNullWhenDomainModeIsNull() {
        ChatbotMessageResult result = ChatbotMessageResult.builder()
                .conversationId("conversation-3")
                .message("Respuesta sin modo")
                .responseMode(null)
                .build();

        ChatbotMessageResponseDto responseDto = ChatbotMessageResponseDto.fromDomain(result);

        assertThat(responseDto.getResponseMode()).isNull();
    }
}

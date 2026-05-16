package es.upm.api.infrastructure.dtos;

import es.upm.api.domain.model.configuration.ChatbotContextualConversationCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotContextualConversationRequestDtoTest {

    @Test
    void setEngagementLetterIdShouldKeepNonBlankValueAndMapToCommand() {
        ChatbotContextualConversationRequestDto requestDto = new ChatbotContextualConversationRequestDto();

        requestDto.setEngagementLetterId("EL-123");
        ChatbotContextualConversationCommand command = requestDto.toCommand();

        assertThat(requestDto.getEngagementLetterId()).isEqualTo("EL-123");
        assertThat(command.getEngagementLetterId()).isEqualTo("EL-123");
    }

    @Test
    void setEngagementLetterIdShouldNormalizeBlankValueToNull() {
        ChatbotContextualConversationRequestDto requestDto = new ChatbotContextualConversationRequestDto();

        requestDto.setEngagementLetterId("   ");

        assertThat(requestDto.getEngagementLetterId()).isNull();
        assertThat(requestDto.toCommand().getEngagementLetterId()).isNull();
    }

    @Test
    void setEngagementLetterIdShouldKeepNullValueAsNull() {
        ChatbotContextualConversationRequestDto requestDto = new ChatbotContextualConversationRequestDto();

        requestDto.setEngagementLetterId(null);

        assertThat(requestDto.getEngagementLetterId()).isNull();
        assertThat(requestDto.toCommand().getEngagementLetterId()).isNull();
    }
}

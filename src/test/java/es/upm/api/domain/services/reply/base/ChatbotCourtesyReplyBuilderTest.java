package es.upm.api.domain.services.reply.base;

import es.upm.api.domain.common.ChatbotResponseMessages;
import es.upm.api.domain.enums.ConversationProfileType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotCourtesyReplyBuilderTest {

    private final ChatbotCourtesyReplyBuilder chatbotCourtesyReplyBuilder = new ChatbotCourtesyReplyBuilder();

    @Test
    void courtesyReplyShouldReturnReplyByProfile() {
        assertThat(this.chatbotCourtesyReplyBuilder.courtesyReply(ConversationProfileType.CLIENT))
                .isEqualTo(ChatbotResponseMessages.CLIENT_COURTESY_REPLY);
        assertThat(this.chatbotCourtesyReplyBuilder.courtesyReply(ConversationProfileType.PROFESSIONAL))
                .isEqualTo(ChatbotResponseMessages.PROFESSIONAL_COURTESY_REPLY);
    }

    @ParameterizedTest
    @MethodSource("courtesyMessages")
    void isCourtesyMessageShouldDetectSupportedCourtesyExpressions(String message) {
        assertThat(this.chatbotCourtesyReplyBuilder.isCourtesyMessage(message)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("nonCourtesyMessages")
    void isCourtesyMessageShouldReturnFalseWhenMessageIsNotCourtesy(String message) {
        assertThat(this.chatbotCourtesyReplyBuilder.isCourtesyMessage(message)).isFalse();
    }

    @Test
    void isCourtesyMessageShouldReturnFalseWhenMessageIsNullOrBlank() {
        assertThat(this.chatbotCourtesyReplyBuilder.isCourtesyMessage(null)).isFalse();
        assertThat(this.chatbotCourtesyReplyBuilder.isCourtesyMessage("   ")).isFalse();
    }

    private static Stream<String> courtesyMessages() {
        return Stream.of(
                "Muchas gracias por todo",
                "por favor revisalo",
                "te quiero ayudar",
                "te amo por ayudar",
                "buen dia",
                "buen d\u00eda",
                "BUENAS tardes",
                "buenas tardes",
                "hola",
                "hasta luego",
                "nos vemos"
        );
    }

    private static Stream<String> nonCourtesyMessages() {
        return Stream.of(
                "Necesito ayuda con mi expediente",
                "Cual es el estado del caso",
                "agradeceria informacion del caso"
        );
    }
}

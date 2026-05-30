package es.upm.api.domain.model.chatbot.reply;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotAiReplyResultTest {

    @Test
    void withAiShouldMarkReplyAsGeneratedWithAi() {
        ChatbotAiReplyResult result = ChatbotAiReplyResult.withAi("Respuesta IA");

        assertThat(result.getAssistantReply()).isEqualTo("Respuesta IA");
        assertThat(result.isUsedAi()).isTrue();
    }

    @Test
    void withoutAiShouldMarkReplyAsFallbackOrBaseReply() {
        ChatbotAiReplyResult result = ChatbotAiReplyResult.withoutAi("Respuesta base");

        assertThat(result.getAssistantReply()).isEqualTo("Respuesta base");
        assertThat(result.isUsedAi()).isFalse();
    }
}

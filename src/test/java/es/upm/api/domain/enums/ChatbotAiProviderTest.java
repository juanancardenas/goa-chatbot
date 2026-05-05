package es.upm.api.domain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotAiProviderTest {

    @Test
    void isSupportedShouldReturnTrueForTrimmedCaseInsensitiveValues() {
        assertThat(ChatbotAiProvider.isSupported(" ollama ")).isTrue();
        assertThat(ChatbotAiProvider.isSupported("OPENAI")).isTrue();
        assertThat(ChatbotAiProvider.isSupported("gEmInI")).isTrue();
    }

    @Test
    void isSupportedShouldReturnFalseForNullBlankOrUnknownValues() {
        assertThat(ChatbotAiProvider.isSupported(null)).isFalse();
        assertThat(ChatbotAiProvider.isSupported("   ")).isFalse();
        assertThat(ChatbotAiProvider.isSupported("claude")).isFalse();
    }
}

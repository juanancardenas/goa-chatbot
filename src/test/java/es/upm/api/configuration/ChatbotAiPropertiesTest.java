package es.upm.api.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatbotAiPropertiesTest {

    @Test
    void shouldHaveOllamaDefaults() {
        ChatbotAiProperties properties = new ChatbotAiProperties();
        properties.setBasePrompt("Prompt base de pruebas");

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getProvider()).isEqualTo("ollama");
        assertThat(properties.normalizedProvider()).isEqualTo("ollama");
        assertThat(properties.getModel()).isEqualTo("llama3.1:8b");
        assertThat(properties.getMaxInputCharacters()).isEqualTo(1000);
        assertThat(properties.getMaxOutputTokens()).isEqualTo(500);
        assertThat(properties.getMaxContextMessages()).isEqualTo(10);
        assertThat(properties.getTimeoutSeconds()).isEqualTo(20);
        assertThat(properties.isDocumentsAvailable()).isFalse();
        assertThat(properties.getTemperature()).isEqualTo(0.2);
    }

    @Test
    void shouldAcceptSupportedProviders() {
        ChatbotAiProperties properties = new ChatbotAiProperties();
        properties.setBasePrompt("Prompt base de pruebas");

        properties.setProvider("ollama");
        assertDoesNotThrow(properties::validateProvider);

        properties.setProvider("openai");
        assertDoesNotThrow(properties::validateProvider);

        properties.setProvider("gemini");
        assertDoesNotThrow(properties::validateProvider);
    }

    @Test
    void shouldAcceptProviderIgnoringCaseAndSpaces() {
        ChatbotAiProperties properties = new ChatbotAiProperties();
        properties.setBasePrompt("Prompt base de pruebas");
        properties.setProvider("  OpenAI  ");

        assertDoesNotThrow(properties::validateProvider);
        assertThat(properties.normalizedProvider()).isEqualTo("openai");
    }

    @Test
    void shouldRejectUnsupportedProvider() {
        ChatbotAiProperties properties = new ChatbotAiProperties();
        properties.setBasePrompt("Prompt base de pruebas");
        properties.setProvider("anthropic");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                properties::validateProvider
        );

        assertThat(exception).hasMessageContaining("ollama, openai o gemini");
    }

    @Test
    void shouldAllowChangingFunctionalLimits() {
        ChatbotAiProperties properties = new ChatbotAiProperties();
        properties.setBasePrompt("Prompt base de pruebas");

        properties.setProvider("gemini");
        properties.setModel("gemini-2.0-flash");
        properties.setMaxInputCharacters(1500);
        properties.setMaxOutputTokens(700);
        properties.setMaxContextMessages(6);
        properties.setTimeoutSeconds(15);
        properties.setDocumentsAvailable(true);
        properties.setTemperature(0.4);

        assertThat(properties.normalizedProvider()).isEqualTo("gemini");
        assertThat(properties.getModel()).isEqualTo("gemini-2.0-flash");
        assertThat(properties.getMaxInputCharacters()).isEqualTo(1500);
        assertThat(properties.getMaxOutputTokens()).isEqualTo(700);
        assertThat(properties.getMaxContextMessages()).isEqualTo(6);
        assertThat(properties.getTimeoutSeconds()).isEqualTo(15);
        assertThat(properties.isDocumentsAvailable()).isTrue();
        assertThat(properties.getTemperature()).isEqualTo(0.4);
    }
}

package es.upm.api.adapter.out.ai;

import es.upm.api.configuration.ChatbotAiProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotAiSettingsAdapterTest {

    @Test
    void shouldExposeAiPropertiesThroughDomainSettingsPort() {
        ChatbotAiProperties properties = new ChatbotAiProperties();
        properties.setEnabled(false);
        properties.setProvider(" OpenAI ");
        properties.setModel("gpt-4.1-mini");
        properties.setMaxInputCharacters(2048);
        properties.setMaxOutputTokens(800);
        properties.setMaxContextMessages(6);
        properties.setDocumentsAvailable(true);
        properties.setBasePrompt("Prompt base");
        properties.setTemperature(0.4);

        ChatbotAiSettingsAdapter adapter = new ChatbotAiSettingsAdapter(properties);

        assertThat(adapter.isEnabled()).isFalse();
        assertThat(adapter.provider()).isEqualTo("openai");
        assertThat(adapter.model()).isEqualTo("gpt-4.1-mini");
        assertThat(adapter.maxInputCharacters()).isEqualTo(2048);
        assertThat(adapter.maxOutputTokens()).isEqualTo(800);
        assertThat(adapter.maxContextMessages()).isEqualTo(6);
        assertThat(adapter.documentsAvailable()).isTrue();
        assertThat(adapter.basePrompt()).isEqualTo("Prompt base");
        assertThat(adapter.temperature()).isEqualTo(0.4);
    }
}

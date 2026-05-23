package es.upm.api.adapter.out.ai;

import es.upm.api.configurations.ChatbotAiProperties;
import es.upm.api.domain.ports.out.ChatbotAiSettings;
import org.springframework.stereotype.Component;

@Component
public class ChatbotAiSettingsAdapter implements ChatbotAiSettings {

    private final ChatbotAiProperties properties;

    public ChatbotAiSettingsAdapter(ChatbotAiProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isEnabled() {
        return this.properties.isEnabled();
    }

    @Override
    public String provider() {
        return this.properties.normalizedProvider();
    }

    @Override
    public String model() {
        return this.properties.getModel();
    }

    @Override
    public int maxInputCharacters() {
        return this.properties.getMaxInputCharacters();
    }

    @Override
    public int maxOutputTokens() {
        return this.properties.getMaxOutputTokens();
    }

    @Override
    public int maxContextMessages() {
        return this.properties.getMaxContextMessages();
    }

    @Override
    public boolean documentsAvailable() {
        return this.properties.isDocumentsAvailable();
    }

    @Override
    public String basePrompt() {
        return this.properties.getBasePrompt();
    }

    @Override
    public double temperature() {
        return this.properties.getTemperature();
    }
}

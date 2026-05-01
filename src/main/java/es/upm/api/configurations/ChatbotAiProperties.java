package es.upm.api.configurations;

import es.upm.api.domain.enums.ChatbotAiProvider;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "chatbot.ai")
public class ChatbotAiProperties {
    private boolean enabled = true;

    @NotBlank
    private String provider = "ollama";

    @NotBlank
    private String model = "llama3.1:8b";

    @Min(1)
    private int maxInputCharacters = 1000;

    @Min(1)
    private int maxOutputTokens = 500;

    @Min(0)
    private int maxContextMessages = 10;

    @Min(1)
    private int timeoutSeconds = 20;

    private boolean documentsAvailable = false;

    @NotBlank
    private String basePrompt;

    @Min(0)
    @Max(1)
    private double temperature = 0.2;

    @PostConstruct
    void validateProvider() {
        if (!ChatbotAiProvider.isSupported(this.provider)) {
            throw new IllegalStateException(
                    "chatbot.ai.provider debe ser uno de: ollama, openai o gemini"
            );
        }
    }

    public String normalizedProvider() {
        return this.provider.trim().toLowerCase();
    }
}

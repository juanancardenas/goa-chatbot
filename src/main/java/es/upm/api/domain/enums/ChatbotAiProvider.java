package es.upm.api.domain.enums;

import java.util.Arrays;

public enum ChatbotAiProvider {
    OLLAMA,
    OPENAI,
    GEMINI;

    public static boolean isSupported(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        return Arrays.stream(values())
                .anyMatch(provider -> provider.name().equalsIgnoreCase(value.trim()));
    }
}

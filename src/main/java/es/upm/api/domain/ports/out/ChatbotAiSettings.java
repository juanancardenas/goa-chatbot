package es.upm.api.domain.ports.out;

public interface ChatbotAiSettings {

    boolean isEnabled();

    String provider();

    String model();

    int maxInputCharacters();

    int maxOutputTokens();

    int maxContextMessages();

    boolean documentsAvailable();

    String basePrompt();

    double temperature();

    int timeoutSeconds();
}

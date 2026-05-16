package es.upm.api.domain.model.chatbot.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotConfigurationResult {

    private boolean enabled;
    private String provider;
    private String model;
    private int maxInputCharacters;
    private int maxOutputTokens;
    private int maxContextMessages;
    private boolean documentsAvailable;
}
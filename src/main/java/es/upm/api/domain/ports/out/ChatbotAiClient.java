package es.upm.api.domain.ports.out;

import es.upm.api.domain.model.ai.ChatbotAiRequest;
import es.upm.api.domain.model.ai.ChatbotAiResponse;

public interface ChatbotAiClient {
    ChatbotAiResponse generate(ChatbotAiRequest request);
}

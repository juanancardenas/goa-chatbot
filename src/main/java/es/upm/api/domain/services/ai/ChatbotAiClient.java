package es.upm.api.domain.services.ai;

import es.upm.api.domain.model.ai.ChatbotAiRequest;
import es.upm.api.domain.model.ai.ChatbotAiResponse;

public interface ChatbotAiClient {
    ChatbotAiResponse generate(ChatbotAiRequest request);
}

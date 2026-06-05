package es.upm.api.domain.services.safety;

import es.upm.api.domain.model.safety.ChatbotModerationDecision;
import es.upm.api.domain.model.safety.ChatbotModerationReason;
import es.upm.api.domain.model.safety.ChatbotPiiDetectionResult;
import org.springframework.stereotype.Service;

@Service
public class ChatbotModerationService {

    private static final String MODERATION_VALIDATION_ERROR_REPLY =
            "No he podido validar tu mensaje de forma segura. "
                    + "Por favor, inténtalo de nuevo más tarde o reformula la consulta.";

    private final ChatbotPiiDetector chatbotPiiDetector;
    private final ChatbotModerationPolicy chatbotModerationPolicy;

    public ChatbotModerationService() {
        this(
                new ChatbotPiiDetector(),
                new ChatbotModerationPolicy()
        );
    }

    public ChatbotModerationService(
            ChatbotPiiDetector chatbotPiiDetector,
            ChatbotModerationPolicy chatbotModerationPolicy
    ) {
        this.chatbotPiiDetector = chatbotPiiDetector;
        this.chatbotModerationPolicy = chatbotModerationPolicy;
    }

    public ChatbotModerationDecision moderate(String message) {
        try {
            ChatbotPiiDetectionResult piiDetectionResult = this.chatbotPiiDetector.detect(message);
            return this.chatbotModerationPolicy.evaluate(piiDetectionResult);
        } catch (RuntimeException exception) {
            return ChatbotModerationDecision.block(
                    ChatbotModerationReason.OUT_OF_POLICY,
                    MODERATION_VALIDATION_ERROR_REPLY,
                    false
            );
        }
    }
}
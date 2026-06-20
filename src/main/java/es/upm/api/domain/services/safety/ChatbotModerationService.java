package es.upm.api.domain.services.safety;

import es.upm.api.domain.model.metrics.ChatbotModerationMetric;
import es.upm.api.domain.model.safety.ChatbotModerationDecision;
import es.upm.api.domain.model.safety.ChatbotModerationReason;
import es.upm.api.domain.model.safety.ChatbotPiiDetectionResult;
import es.upm.api.domain.ports.out.ChatbotMetricsRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
public class ChatbotModerationService {

    private static final String MODERATION_VALIDATION_ERROR_REPLY =
            "No he podido validar tu mensaje de forma segura. "
                    + "Por favor, inténtalo de nuevo más tarde o reformula la consulta.";

    private final ChatbotPiiDetector chatbotPiiDetector;
    private final ChatbotModerationPolicy chatbotModerationPolicy;
    private final ChatbotMetricsRecorder chatbotMetricsRecorder;
    private final Clock clock;

    @Autowired
    public ChatbotModerationService(ChatbotMetricsRecorder chatbotMetricsRecorder, Clock clock) {
        this(
                new ChatbotPiiDetector(),
                new ChatbotModerationPolicy(),
                chatbotMetricsRecorder,
                clock
        );
    }

    public ChatbotModerationService(
            ChatbotPiiDetector chatbotPiiDetector,
            ChatbotModerationPolicy chatbotModerationPolicy
    ) {
        this(
                chatbotPiiDetector,
                chatbotModerationPolicy,
                null,
                Clock.systemUTC()
        );
    }

    public ChatbotModerationService(
            ChatbotPiiDetector chatbotPiiDetector,
            ChatbotModerationPolicy chatbotModerationPolicy,
            ChatbotMetricsRecorder chatbotMetricsRecorder,
            Clock clock
    ) {
        this.chatbotPiiDetector = chatbotPiiDetector;
        this.chatbotModerationPolicy = chatbotModerationPolicy;
        this.chatbotMetricsRecorder = chatbotMetricsRecorder;
        this.clock = clock;
    }

    public ChatbotModerationDecision moderate(String message) {
        return this.moderate(message, null, null);
    }

    public ChatbotModerationDecision moderate(
            String message,
            String conversationId,
            String userId
    ) {
        try {
            ChatbotPiiDetectionResult piiDetectionResult = this.chatbotPiiDetector.detect(message);
            ChatbotModerationDecision decision = this.chatbotModerationPolicy.evaluate(piiDetectionResult);

            this.recordModerationSafely(
                    conversationId,
                    userId,
                    decision,
                    decision.isBlocked() ? Boolean.FALSE : null
            );

            return decision;
        } catch (RuntimeException exception) {
            ChatbotModerationDecision decision = ChatbotModerationDecision.block(
                    ChatbotModerationReason.OUT_OF_POLICY,
                    MODERATION_VALIDATION_ERROR_REPLY,
                    false
            );

            this.recordModerationSafely(
                    conversationId,
                    userId,
                    decision,
                    Boolean.FALSE
            );

            return decision;
        }
    }

    private void recordModerationSafely(
            String conversationId,
            String userId,
            ChatbotModerationDecision decision,
            Boolean usedAi
    ) {
        if (this.chatbotMetricsRecorder == null) {
            return;
        }

        try {
            this.chatbotMetricsRecorder.recordModeration(
                    ChatbotModerationMetric.builder()
                            .conversationId(conversationId)
                            .userId(userId)
                            .action(decision.getAction())
                            .reason(decision.getReason())
                            .containsPii(decision.isContainsPii())
                            .blocked(decision.isBlocked())
                            .usedAi(usedAi)
                            .createdAt(LocalDateTime.now(this.clock))
                            .build()
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "chatbot_moderation_event status=failed conversationId={} userId={} reason={}",
                    conversationId,
                    userId,
                    exception.getClass().getSimpleName()
            );
        }
    }
}

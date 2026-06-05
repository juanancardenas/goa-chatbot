package es.upm.api.domain.services.safety;

import es.upm.api.domain.model.metrics.ChatbotModerationMetric;
import es.upm.api.domain.model.safety.ChatbotModerationAction;
import es.upm.api.domain.model.safety.ChatbotModerationDecision;
import es.upm.api.domain.model.safety.ChatbotModerationReason;
import es.upm.api.domain.ports.out.ChatbotMetricsRecorder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

class ChatbotModerationServiceTest {

    private final ChatbotModerationService chatbotModerationService = new ChatbotModerationService(
            new ChatbotPiiDetector(),
            new ChatbotModerationPolicy()
    );

    @Test
    void moderateShouldAllowMessageWithoutPii() {
        ChatbotModerationDecision decision = this.chatbotModerationService.moderate(
                "Quiero saber el estado de mi encargo"
        );

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.ALLOW);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.NONE);
        assertThat(decision.isAllowed()).isTrue();
        assertThat(decision.isContainsPii()).isFalse();
    }

    @Test
    void moderateShouldWarnWhenEmailIsDetected() {
        ChatbotModerationDecision decision = this.chatbotModerationService.moderate(
                "Mi correo es usuario@example.com"
        );

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.WARN);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_EMAIL);
        assertThat(decision.isWarning()).isTrue();
        assertThat(decision.isContainsPii()).isTrue();
        assertThat(decision.getSafeReply()).isNotBlank();
    }

    @Test
    void moderateShouldWarnWhenPhoneIsDetected() {
        ChatbotModerationDecision decision = this.chatbotModerationService.moderate(
                "Mi teléfono es +34 612 345 678"
        );

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.WARN);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_PHONE);
        assertThat(decision.isWarning()).isTrue();
        assertThat(decision.isContainsPii()).isTrue();
    }

    @Test
    void moderateShouldBlockWhenCardIsDetected() {
        ChatbotModerationDecision decision = this.chatbotModerationService.moderate(
                "Mi tarjeta es 4111 1111 1111 1111"
        );

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.BLOCK);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_CARD);
        assertThat(decision.isBlocked()).isTrue();
        assertThat(decision.isContainsPii()).isTrue();
        assertThat(decision.getSafeReply()).isNotBlank();
        assertThat(decision.getSafeReply()).doesNotContain("4111 1111 1111 1111");
    }

    @Test
    void moderateShouldReturnSafeBlockWhenDetectorFails() {
        ChatbotPiiDetector failingDetector = new ChatbotPiiDetector() {
            @Override
            public es.upm.api.domain.model.safety.ChatbotPiiDetectionResult detect(String message) {
                throw new IllegalStateException("Forced detector failure");
            }
        };

        ChatbotModerationService service = new ChatbotModerationService(
                failingDetector,
                new ChatbotModerationPolicy()
        );

        ChatbotModerationDecision decision = service.moderate("mensaje cualquiera");

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.BLOCK);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.OUT_OF_POLICY);
        assertThat(decision.isBlocked()).isTrue();
        assertThat(decision.getSafeReply()).isNotBlank();
    }

    private static class CapturingChatbotMetricsRecorder implements ChatbotMetricsRecorder {

        private final List<ChatbotModerationMetric> moderationMetrics = new ArrayList<>();

        @Override
        public void recordMessageHandled(es.upm.api.domain.model.metrics.ChatbotMessageMetric metric) {
            // Not needed in this test
        }

        @Override
        public void recordAiCall(es.upm.api.domain.model.metrics.ChatbotAiMetric metric) {
            // Not needed in this test
        }

        @Override
        public void recordEscalation(es.upm.api.domain.model.metrics.ChatbotEscalationMetric metric) {
            // Not needed in this test
        }

        @Override
        public void recordFallback(es.upm.api.domain.model.metrics.ChatbotFallbackMetric metric) {
            // Not needed in this test
        }

        @Override
        public void recordModeration(ChatbotModerationMetric metric) {
            this.moderationMetrics.add(metric);
        }
    }

    @Test
    void moderateShouldRecordBlockedModerationMetricWithoutSensitiveContent() {
        CapturingChatbotMetricsRecorder recorder = new CapturingChatbotMetricsRecorder();

        ChatbotModerationService service = new ChatbotModerationService(
                new ChatbotPiiDetector(),
                new ChatbotModerationPolicy(),
                recorder
        );

        ChatbotModerationDecision decision = service.moderate(
                "Mi tarjeta es 4111 1111 1111 1111",
                "conversation-1",
                "user-1"
        );

        assertThat(decision.isBlocked()).isTrue();

        assertThat(recorder.moderationMetrics).hasSize(1);

        ChatbotModerationMetric metric = recorder.moderationMetrics.get(0);

        assertThat(metric.getConversationId()).isEqualTo("conversation-1");
        assertThat(metric.getUserId()).isEqualTo("user-1");
        assertThat(metric.getAction()).isEqualTo(ChatbotModerationAction.BLOCK);
        assertThat(metric.getReason()).isEqualTo(ChatbotModerationReason.PII_CARD);
        assertThat(metric.isContainsPii()).isTrue();
        assertThat(metric.isBlocked()).isTrue();
        assertThat(metric.getUsedAi()).isFalse();
        assertThat(metric.getCreatedAt()).isNotNull();

        assertThat(metric.toString()).doesNotContain("4111 1111 1111 1111");
    }

    @Test
    void moderateShouldRecordWarningModerationMetricWithoutAssumingAiUsage() {
        CapturingChatbotMetricsRecorder recorder = new CapturingChatbotMetricsRecorder();

        ChatbotModerationService service = new ChatbotModerationService(
                new ChatbotPiiDetector(),
                new ChatbotModerationPolicy(),
                recorder
        );

        ChatbotModerationDecision decision = service.moderate(
                "Mi email es usuario@example.com",
                "conversation-2",
                "user-2"
        );

        assertThat(decision.isWarning()).isTrue();

        assertThat(recorder.moderationMetrics).hasSize(1);

        ChatbotModerationMetric metric = recorder.moderationMetrics.get(0);

        assertThat(metric.getConversationId()).isEqualTo("conversation-2");
        assertThat(metric.getUserId()).isEqualTo("user-2");
        assertThat(metric.getAction()).isEqualTo(ChatbotModerationAction.WARN);
        assertThat(metric.getReason()).isEqualTo(ChatbotModerationReason.PII_EMAIL);
        assertThat(metric.isContainsPii()).isTrue();
        assertThat(metric.isBlocked()).isFalse();
        assertThat(metric.getUsedAi()).isNull();
        assertThat(metric.getCreatedAt()).isNotNull();

        assertThat(metric.toString()).doesNotContain("usuario@example.com");
    }

    @Test
    void moderateShouldNotFailWhenModerationMetricRecordingFails() {
        ChatbotMetricsRecorder failingRecorder = new CapturingChatbotMetricsRecorder() {
            @Override
            public void recordModeration(ChatbotModerationMetric metric) {
                throw new IllegalStateException("Forced metric failure");
            }
        };

        ChatbotModerationService service = new ChatbotModerationService(
                new ChatbotPiiDetector(),
                new ChatbotModerationPolicy(),
                failingRecorder
        );

        ChatbotModerationDecision decision = service.moderate(
                "Mi tarjeta es 4111 1111 1111 1111",
                "conversation-3",
                "user-3"
        );

        assertThat(decision.isBlocked()).isTrue();
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_CARD);
    }

    @Test
    void moderateShouldWarnWhenSpanishIbanIsDetectedWithoutBlockingAsCard() {
        ChatbotModerationDecision decision = this.chatbotModerationService.moderate(
                "Mi IBAN es ES91 2100 0418 4502 0005 1332"
        );

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.WARN);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_IBAN);
        assertThat(decision.isWarning()).isTrue();
        assertThat(decision.isBlocked()).isFalse();
        assertThat(decision.isContainsPii()).isTrue();
    }
}
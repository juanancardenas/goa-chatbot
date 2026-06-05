package es.upm.api.domain.services.safety;

import es.upm.api.domain.model.safety.ChatbotModerationAction;
import es.upm.api.domain.model.safety.ChatbotModerationDecision;
import es.upm.api.domain.model.safety.ChatbotModerationReason;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
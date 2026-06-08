package es.upm.api.domain.services.safety;

import es.upm.api.domain.model.safety.ChatbotModerationAction;
import es.upm.api.domain.model.safety.ChatbotModerationDecision;
import es.upm.api.domain.model.safety.ChatbotModerationReason;
import es.upm.api.domain.model.safety.ChatbotPiiDetectionResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotModerationPolicyTest {
    private final ChatbotModerationPolicy policy = new ChatbotModerationPolicy();

    @Test
    void evaluateShouldAllowWhenThereIsNoPii() {
        ChatbotModerationDecision decision = this.policy.evaluate(ChatbotPiiDetectionResult.empty());

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.ALLOW);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.NONE);
        assertThat(decision.getSafeReply()).isNull();
        assertThat(decision.isContainsPii()).isFalse();
        assertThat(decision.isAllowed()).isTrue();
        assertThat(decision.isBlocked()).isFalse();
    }

    @Test
    void evaluateShouldAllowWhenPiiDetectionResultIsNull() {
        ChatbotModerationDecision decision = this.policy.evaluate(null);

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.ALLOW);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.NONE);
        assertThat(decision.isContainsPii()).isFalse();
        assertThat(decision.isAllowed()).isTrue();
    }

    @Test
    void evaluateShouldWarnWhenEmailIsDetected() {
        ChatbotPiiDetectionResult piiDetectionResult = ChatbotPiiDetectionResult.of(
                true,
                false,
                false,
                false,
                false
        );

        ChatbotModerationDecision decision = this.policy.evaluate(piiDetectionResult);

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.WARN);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_EMAIL);
        assertThat(decision.getSafeReply()).isNotBlank();
        assertThat(decision.isContainsPii()).isTrue();
        assertThat(decision.isWarning()).isTrue();
        assertThat(decision.isBlocked()).isFalse();
    }

    @Test
    void evaluateShouldWarnWhenPhoneIsDetected() {
        ChatbotPiiDetectionResult piiDetectionResult = ChatbotPiiDetectionResult.of(
                false,
                true,
                false,
                false,
                false
        );

        ChatbotModerationDecision decision = this.policy.evaluate(piiDetectionResult);

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.WARN);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_PHONE);
        assertThat(decision.getSafeReply()).isNotBlank();
        assertThat(decision.isContainsPii()).isTrue();
        assertThat(decision.isWarning()).isTrue();
    }

    @Test
    void evaluateShouldWarnWhenDniNieIsDetected() {
        ChatbotPiiDetectionResult piiDetectionResult = ChatbotPiiDetectionResult.of(
                false,
                false,
                true,
                false,
                false
        );

        ChatbotModerationDecision decision = this.policy.evaluate(piiDetectionResult);

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.WARN);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_DNI_NIE);
        assertThat(decision.getSafeReply()).isNotBlank();
        assertThat(decision.isContainsPii()).isTrue();
        assertThat(decision.isWarning()).isTrue();
    }

    @Test
    void evaluateShouldWarnWhenIbanIsDetected() {
        ChatbotPiiDetectionResult piiDetectionResult = ChatbotPiiDetectionResult.of(
                false,
                false,
                false,
                false,
                true
        );

        ChatbotModerationDecision decision = this.policy.evaluate(piiDetectionResult);

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.WARN);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_IBAN);
        assertThat(decision.getSafeReply()).isNotBlank();
        assertThat(decision.isContainsPii()).isTrue();
        assertThat(decision.isWarning()).isTrue();
        assertThat(decision.isBlocked()).isFalse();
    }

    @Test
    void evaluateShouldBlockWhenCardIsDetected() {
        ChatbotPiiDetectionResult piiDetectionResult = ChatbotPiiDetectionResult.of(
                false,
                false,
                false,
                true,
                false
        );

        ChatbotModerationDecision decision = this.policy.evaluate(piiDetectionResult);

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.BLOCK);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_CARD);
        assertThat(decision.getSafeReply()).isNotBlank();
        assertThat(decision.isContainsPii()).isTrue();
        assertThat(decision.isBlocked()).isTrue();
    }

    @Test
    void evaluateShouldBlockWhenUnsafeRequestIsDetected() {
        ChatbotModerationDecision decision = this.policy.evaluate(
                ChatbotPiiDetectionResult.empty(),
                true,
                false
        );

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.BLOCK);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.UNSAFE_REQUEST);
        assertThat(decision.getSafeReply()).isNotBlank();
        assertThat(decision.isContainsPii()).isFalse();
        assertThat(decision.isBlocked()).isTrue();
    }

    @Test
    void evaluateShouldBlockUnsafeRequestAndPreservePiiFlagWhenPiiIsDetected() {
        ChatbotModerationDecision decision = this.policy.evaluate(
                ChatbotPiiDetectionResult.of(
                        true,
                        false,
                        false,
                        false,
                        false
                ),
                true,
                false
        );

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.BLOCK);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.UNSAFE_REQUEST);
        assertThat(decision.isContainsPii()).isTrue();
        assertThat(decision.isBlocked()).isTrue();
    }

    @Test
    void evaluateShouldBlockWhenOutOfPolicyIsDetected() {
        ChatbotModerationDecision decision = this.policy.evaluate(
                ChatbotPiiDetectionResult.empty(),
                false,
                true
        );

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.BLOCK);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.OUT_OF_POLICY);
        assertThat(decision.getSafeReply()).isNotBlank();
        assertThat(decision.isContainsPii()).isFalse();
        assertThat(decision.isBlocked()).isTrue();
    }

    @Test
    void evaluateShouldBlockOutOfPolicyRequestAndPreservePiiFlagWhenPiiIsDetected() {
        ChatbotModerationDecision decision = this.policy.evaluate(
                ChatbotPiiDetectionResult.of(
                        false,
                        true,
                        false,
                        false,
                        false
                ),
                false,
                true
        );

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.BLOCK);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.OUT_OF_POLICY);
        assertThat(decision.isContainsPii()).isTrue();
        assertThat(decision.isBlocked()).isTrue();
    }

    @Test
    void evaluateShouldApplyMostRestrictiveRuleWhenEmailAndCardAreDetected() {
        ChatbotPiiDetectionResult piiDetectionResult = ChatbotPiiDetectionResult.of(
                true,
                false,
                false,
                true,
                false
        );

        ChatbotModerationDecision decision = this.policy.evaluate(piiDetectionResult);

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.BLOCK);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_CARD);
        assertThat(decision.isContainsPii()).isTrue();
        assertThat(decision.isBlocked()).isTrue();
    }

    @Test
    void evaluateShouldPrioritizeCardOverUnsafeRequest() {
        ChatbotPiiDetectionResult piiDetectionResult = ChatbotPiiDetectionResult.of(
                false,
                false,
                false,
                true,
                false
        );

        ChatbotModerationDecision decision = this.policy.evaluate(
                piiDetectionResult,
                true,
                false
        );

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.BLOCK);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_CARD);
        assertThat(decision.isContainsPii()).isTrue();
        assertThat(decision.isBlocked()).isTrue();
    }

    @Test
    void evaluateShouldPrioritizeUnsafeRequestOverOutOfPolicy() {
        ChatbotModerationDecision decision = this.policy.evaluate(
                ChatbotPiiDetectionResult.empty(),
                true,
                true
        );

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.BLOCK);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.UNSAFE_REQUEST);
        assertThat(decision.isBlocked()).isTrue();
    }

    @Test
    void evaluateShouldPrioritizeIbanOverDniNiePhoneAndEmailForWarnings() {
        ChatbotPiiDetectionResult piiDetectionResult = ChatbotPiiDetectionResult.of(
                true,
                true,
                true,
                false,
                true
        );

        ChatbotModerationDecision decision = this.policy.evaluate(piiDetectionResult);

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.WARN);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_IBAN);
        assertThat(decision.isContainsPii()).isTrue();
        assertThat(decision.isWarning()).isTrue();
    }

    @Test
    void evaluateShouldPrioritizeDniNieOverPhoneAndEmailForWarnings() {
        ChatbotPiiDetectionResult piiDetectionResult = ChatbotPiiDetectionResult.of(
                true,
                true,
                true,
                false,
                false
        );

        ChatbotModerationDecision decision = this.policy.evaluate(piiDetectionResult);

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.WARN);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_DNI_NIE);
        assertThat(decision.isContainsPii()).isTrue();
        assertThat(decision.isWarning()).isTrue();
    }

    @Test
    void evaluateShouldPrioritizePhoneOverEmailForWarnings() {
        ChatbotPiiDetectionResult piiDetectionResult = ChatbotPiiDetectionResult.of(
                true,
                true,
                false,
                false,
                false
        );

        ChatbotModerationDecision decision = this.policy.evaluate(piiDetectionResult);

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.WARN);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_PHONE);
        assertThat(decision.isContainsPii()).isTrue();
        assertThat(decision.isWarning()).isTrue();
    }

    @Test
    void evaluateShouldNotExposePiiValuesInSafeReply() {
        String sensitiveValue = "4111 1111 1111 1111";

        ChatbotPiiDetectionResult piiDetectionResult = ChatbotPiiDetectionResult.of(
                false,
                false,
                false,
                true,
                false
        );

        ChatbotModerationDecision decision = this.policy.evaluate(piiDetectionResult);

        assertThat(decision.getSafeReply()).doesNotContain(sensitiveValue);
        assertThat(decision.getSafeReply()).doesNotContain("usuario@example.com");
        assertThat(decision.getSafeReply()).doesNotContain("612345678");
        assertThat(decision.getSafeReply()).doesNotContain("12345678Z");
        assertThat(decision.getSafeReply()).doesNotContain("ES9121000418450200051332");
    }
}

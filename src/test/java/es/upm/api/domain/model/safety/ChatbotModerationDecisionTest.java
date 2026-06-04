package es.upm.api.domain.model.safety;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotModerationDecisionTest {

    private static final String SAFE_WARNING_REPLY =
            "He detectado que tu mensaje puede contener datos personales. "
                    + "Evita compartir información sensible si no es estrictamente necesario.";

    private static final String SAFE_BLOCKED_REPLY =
            "No puedo procesar mensajes que contengan datos bancarios sensibles. "
                    + "Por favor, elimina esa información y vuelve a intentarlo.";

    @Test
    void allowShouldCreateAllowedDecisionWithoutPii() {
        ChatbotModerationDecision decision = ChatbotModerationDecision.allow();

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.ALLOW);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.NONE);
        assertThat(decision.getSafeReply()).isNull();
        assertThat(decision.isContainsPii()).isFalse();

        assertThat(decision.isAllowed()).isTrue();
        assertThat(decision.isWarning()).isFalse();
        assertThat(decision.isBlocked()).isFalse();
    }

    @Test
    void warnShouldCreateWarningDecisionWithSafeReply() {
        ChatbotModerationDecision decision = ChatbotModerationDecision.warn(
                ChatbotModerationReason.PII_EMAIL,
                SAFE_WARNING_REPLY,
                true
        );

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.WARN);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_EMAIL);
        assertThat(decision.getSafeReply()).isEqualTo(SAFE_WARNING_REPLY);
        assertThat(decision.isContainsPii()).isTrue();

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.isWarning()).isTrue();
        assertThat(decision.isBlocked()).isFalse();
    }

    @Test
    void blockShouldCreateBlockedDecisionWithSafeReply() {
        ChatbotModerationDecision decision = ChatbotModerationDecision.block(
                ChatbotModerationReason.PII_CARD,
                SAFE_BLOCKED_REPLY,
                true
        );

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.BLOCK);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.PII_CARD);
        assertThat(decision.getSafeReply()).isEqualTo(SAFE_BLOCKED_REPLY);
        assertThat(decision.isContainsPii()).isTrue();

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.isWarning()).isFalse();
        assertThat(decision.isBlocked()).isTrue();
    }

    @Test
    void warnShouldAllowNonPiiWarningsForFutureRules() {
        ChatbotModerationDecision decision = ChatbotModerationDecision.warn(
                ChatbotModerationReason.OUT_OF_POLICY,
                "No puedo procesar esa solicitud porque está fuera del alcance permitido del asistente.",
                false
        );

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.WARN);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.OUT_OF_POLICY);
        assertThat(decision.isContainsPii()).isFalse();
        assertThat(decision.isWarning()).isTrue();
        assertThat(decision.isBlocked()).isFalse();
    }

    @Test
    void blockShouldAllowUnsafeRequestWithoutPii() {
        ChatbotModerationDecision decision = ChatbotModerationDecision.block(
                ChatbotModerationReason.UNSAFE_REQUEST,
                "No puedo ayudar con esa solicitud.",
                false
        );

        assertThat(decision.getAction()).isEqualTo(ChatbotModerationAction.BLOCK);
        assertThat(decision.getReason()).isEqualTo(ChatbotModerationReason.UNSAFE_REQUEST);
        assertThat(decision.isContainsPii()).isFalse();
        assertThat(decision.isBlocked()).isTrue();
    }
}

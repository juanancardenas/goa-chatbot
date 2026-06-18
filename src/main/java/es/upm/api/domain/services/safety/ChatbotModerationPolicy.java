package es.upm.api.domain.services.safety;

import es.upm.api.domain.model.safety.ChatbotModerationDecision;
import es.upm.api.domain.model.safety.ChatbotModerationReason;
import es.upm.api.domain.model.safety.ChatbotPiiDetectionResult;

public class ChatbotModerationPolicy {
    private static final String GENERIC_PII_WARNING_REPLY =
            "He detectado que tu mensaje puede contener datos personales. "
                    + "Evita compartir información sensible si no es estrictamente necesario.";

    private static final String EMAIL_WARNING_REPLY =
            "He detectado que tu mensaje puede contener un email. "
                    + "Evita compartir datos personales si no es necesario.";

    private static final String PHONE_WARNING_REPLY =
            "He detectado que tu mensaje puede contener un teléfono. "
                    + "Evita compartir datos personales si no es necesario.";

    private static final String IBAN_WARNING_REPLY =
            "He detectado que tu mensaje puede contener datos bancarios. "
                    + "Evita compartir información financiera sensible si no es estrictamente necesario.";

    private static final String PASSPORT_WARNING_REPLY =
            "He detectado que tu mensaje puede contener un pasaporte. "
                    + "Evita compartir documentos personales si no es estrictamente necesario.";

    private static final String CARD_BLOCKED_REPLY =
            "No puedo procesar mensajes que contengan datos bancarios sensibles. "
                    + "Por favor, elimina esa información y vuelve a intentarlo.";

    private static final String UNSAFE_REQUEST_BLOCKED_REPLY =
            "No puedo ayudar con esa solicitud. "
                    + "Puedo ayudarte con dudas relacionadas con tus encargos o con el funcionamiento del servicio.";

    private static final String OUT_OF_POLICY_BLOCKED_REPLY =
            "No puedo procesar esa solicitud porque está fuera del alcance permitido del asistente.";

    public ChatbotModerationDecision evaluate(ChatbotPiiDetectionResult piiDetectionResult) {
        return this.evaluate(piiDetectionResult, false, false);
    }

    public ChatbotModerationDecision evaluate(
            ChatbotPiiDetectionResult piiDetectionResult,
            boolean unsafeRequestDetected,
            boolean outOfPolicyDetected
    ) {
        if (this.containsCard(piiDetectionResult)) {
            return ChatbotModerationDecision.block(
                    ChatbotModerationReason.PII_CARD,
                    CARD_BLOCKED_REPLY,
                    true
            );
        }

        if (unsafeRequestDetected) {
            return ChatbotModerationDecision.block(
                    ChatbotModerationReason.UNSAFE_REQUEST,
                    UNSAFE_REQUEST_BLOCKED_REPLY,
                    this.containsPii(piiDetectionResult)
            );
        }

        if (outOfPolicyDetected) {
            return ChatbotModerationDecision.block(
                    ChatbotModerationReason.OUT_OF_POLICY,
                    OUT_OF_POLICY_BLOCKED_REPLY,
                    this.containsPii(piiDetectionResult)
            );
        }

        if (this.containsIban(piiDetectionResult)) {
            return ChatbotModerationDecision.warn(
                    ChatbotModerationReason.PII_IBAN,
                    IBAN_WARNING_REPLY,
                    true
            );
        }

        if (this.containsPassport(piiDetectionResult)) {
            return ChatbotModerationDecision.warn(
                    ChatbotModerationReason.PII_PASSPORT,
                    PASSPORT_WARNING_REPLY,
                    true
            );
        }

        if (this.containsDniNie(piiDetectionResult)) {
            return ChatbotModerationDecision.warn(
                    ChatbotModerationReason.PII_DNI_NIE,
                    GENERIC_PII_WARNING_REPLY,
                    true
            );
        }

        if (this.containsPhone(piiDetectionResult)) {
            return ChatbotModerationDecision.warn(
                    ChatbotModerationReason.PII_PHONE,
                    PHONE_WARNING_REPLY,
                    true
            );
        }

        if (this.containsEmail(piiDetectionResult)) {
            return ChatbotModerationDecision.warn(
                    ChatbotModerationReason.PII_EMAIL,
                    EMAIL_WARNING_REPLY,
                    true
            );
        }

        return ChatbotModerationDecision.allow();
    }

    private boolean containsPii(ChatbotPiiDetectionResult piiDetectionResult) {
        return piiDetectionResult != null && piiDetectionResult.containsPii();
    }

    private boolean containsEmail(ChatbotPiiDetectionResult piiDetectionResult) {
        return piiDetectionResult != null && piiDetectionResult.isContainsEmail();
    }

    private boolean containsPhone(ChatbotPiiDetectionResult piiDetectionResult) {
        return piiDetectionResult != null && piiDetectionResult.isContainsPhone();
    }

    private boolean containsDniNie(ChatbotPiiDetectionResult piiDetectionResult) {
        return piiDetectionResult != null && piiDetectionResult.isContainsDniNie();
    }

    private boolean containsPassport(ChatbotPiiDetectionResult piiDetectionResult) {
        return piiDetectionResult != null && piiDetectionResult.isContainsPassport();
    }

    private boolean containsCard(ChatbotPiiDetectionResult piiDetectionResult) {
        return piiDetectionResult != null && piiDetectionResult.isContainsCard();
    }

    private boolean containsIban(ChatbotPiiDetectionResult piiDetectionResult) {
        return piiDetectionResult != null && piiDetectionResult.isContainsIban();
    }
}

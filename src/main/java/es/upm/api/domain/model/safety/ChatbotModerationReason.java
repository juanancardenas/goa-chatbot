package es.upm.api.domain.model.safety;

public enum ChatbotModerationReason {
    NONE,
    PII_EMAIL,
    PII_PHONE,
    PII_DNI_NIE,
    PII_PASSPORT,
    PII_CARD,
    PII_IBAN,
    UNSAFE_REQUEST,
    OUT_OF_POLICY
}

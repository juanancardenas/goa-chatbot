package es.upm.api.domain.enums;

public enum ChatbotScopeViolationReason {
    OUT_OF_CASE_SCOPE,
    MISSING_CASE_CONTEXT,
    LEGAL_BINDING_ADVICE_REQUESTED,
    UNSUPPORTED_FACTUAL_ASSERTION,
    AMBIGUOUS_CONTEXT
}

package es.upm.api.domain.services.policies;

import es.upm.api.domain.enums.ChatbotScopeViolationReason;

public class ChatbotScopeDecision {
    private final boolean allowed;
    private final ChatbotScopeViolationReason reason;
    private final String safeMessage;
    private final boolean requiresHuman;

    private ChatbotScopeDecision(
            boolean allowed,
            ChatbotScopeViolationReason reason,
            String safeMessage,
            boolean requiresHuman
    ) {
        this.allowed = allowed;
        this.reason = reason;
        this.safeMessage = safeMessage;
        this.requiresHuman = requiresHuman;
    }

    public static ChatbotScopeDecision allow() {
        return new ChatbotScopeDecision(true, null, null, false);
    }

    public static ChatbotScopeDecision reject(
            ChatbotScopeViolationReason reason,
            String safeMessage,
            boolean requiresHuman
    ) {
        return new ChatbotScopeDecision(false, reason, safeMessage, requiresHuman);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public ChatbotScopeViolationReason getReason() {
        return reason;
    }

    public String getSafeMessage() {
        return safeMessage;
    }

    public boolean isRequiresHuman() {
        return requiresHuman;
    }
}

package es.upm.api.domain.model.safety;

import lombok.Getter;

@Getter
public class ChatbotModerationDecision {

    private final ChatbotModerationAction action;
    private final ChatbotModerationReason reason;
    private final String safeReply;
    private final boolean containsPii;

    private ChatbotModerationDecision(
            ChatbotModerationAction action,
            ChatbotModerationReason reason,
            String safeReply,
            boolean containsPii
    ) {
        this.action = action;
        this.reason = reason;
        this.safeReply = safeReply;
        this.containsPii = containsPii;
    }

    public static ChatbotModerationDecision allow() {
        return new ChatbotModerationDecision(
                ChatbotModerationAction.ALLOW,
                ChatbotModerationReason.NONE,
                null,
                false
        );
    }

    public static ChatbotModerationDecision warn(
            ChatbotModerationReason reason,
            String safeReply,
            boolean containsPii
    ) {
        return new ChatbotModerationDecision(
                ChatbotModerationAction.WARN,
                reason,
                safeReply,
                containsPii
        );
    }

    public static ChatbotModerationDecision block(
            ChatbotModerationReason reason,
            String safeReply,
            boolean containsPii
    ) {
        return new ChatbotModerationDecision(
                ChatbotModerationAction.BLOCK,
                reason,
                safeReply,
                containsPii
        );
    }

    public boolean isAllowed() {
        return ChatbotModerationAction.ALLOW.equals(this.action);
    }

    public boolean isWarning() {
        return ChatbotModerationAction.WARN.equals(this.action);
    }

    public boolean isBlocked() {
        return ChatbotModerationAction.BLOCK.equals(this.action);
    }
}

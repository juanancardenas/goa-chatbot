package es.upm.api.domain.model.chatbot.reply;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ChatbotAiReplyResult {

    private final String assistantReply;
    private final boolean usedAi;

    public static ChatbotAiReplyResult withAi(String assistantReply) {
        return new ChatbotAiReplyResult(assistantReply, true);
    }

    public static ChatbotAiReplyResult withoutAi(String assistantReply) {
        return new ChatbotAiReplyResult(assistantReply, false);
    }
}

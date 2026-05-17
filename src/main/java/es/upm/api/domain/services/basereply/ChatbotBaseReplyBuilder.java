package es.upm.api.domain.services.basereply;

import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.PlatformQuestionType;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.services.classification.ChatbotQuestionClassifier;
import org.springframework.stereotype.Service;

import static es.upm.api.domain.services.classification.ChatbotQuestionTypes.classifyOrGeneralContext;

@Service
public class ChatbotBaseReplyBuilder {

    // Attributes
    private final ChatbotCourtesyReplyBuilder courtesyReplyBuilder;
    private final ChatbotGeneralReplyBuilder generalReplyBuilder;
    private final ChatbotContextualReplyBuilder contextualReplyBuilder;
    private final ChatbotPlatformReplyBuilder platformReplyBuilder;
    private final ChatbotQuestionClassifier chatbotQuestionClassifier;

    // Constructor
    public ChatbotBaseReplyBuilder(
            ChatbotCourtesyReplyBuilder courtesyReplyBuilder,
            ChatbotGeneralReplyBuilder generalReplyBuilder,
            ChatbotContextualReplyBuilder contextualReplyBuilder,
            ChatbotPlatformReplyBuilder platformReplyBuilder,
            ChatbotQuestionClassifier chatbotQuestionClassifier
    ) {
        this.courtesyReplyBuilder = courtesyReplyBuilder;
        this.generalReplyBuilder = generalReplyBuilder;
        this.contextualReplyBuilder = contextualReplyBuilder;
        this.platformReplyBuilder = platformReplyBuilder;
        this.chatbotQuestionClassifier = chatbotQuestionClassifier;
    }

    /* --- COURTESY REPLY -------------------------------------------- */

    public boolean isCourtesyMessage(String message) {
        return this.courtesyReplyBuilder.isCourtesyMessage(message);
    }

    public String courtesyReply(ConversationProfileType profile) {
        return this.courtesyReplyBuilder.courtesyReply(profile);
    }

    /* --- GENERAL REPLY --------------------------------------------- */

    public String generalStartReply(ConversationProfileType profile) {
        return this.generalReplyBuilder.generalStartReply(profile);
    }

    public String generalFaqReply(ConversationProfileType profile, String userMessage) {
        return this.generalReplyBuilder.generalFaqReply(profile, userMessage);
    }

    /* --- CONTEXTUAL REPLY ------------------------------------------ */

    public String contextualReply(
            ConversationProfileType profile,
            String userMessage
    ) {
        return this.contextualReplyBuilder.contextualReply(profile, userMessage);
    }

    /* --- PLATFORM CONTEXT ------------------------------------------ */

    public String contextualPlatformReply(
            ConversationProfileType profile,
            String userMessage,
            Conversation conversation,
            ChatbotPlatformContext platformContext
    ) {
        PlatformQuestionType questionType = classifyOrGeneralContext(this.chatbotQuestionClassifier, userMessage);

        return this.platformReplyBuilder.contextualPlatformReply(
                profile,
                conversation,
                platformContext,
                questionType
        );
    }
}

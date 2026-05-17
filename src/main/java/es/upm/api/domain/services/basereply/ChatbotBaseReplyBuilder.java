package es.upm.api.domain.services.basereply;

import es.upm.api.domain.common.ChatbotResponseMessages;
import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.PlatformQuestionType;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.services.classification.ChatbotQuestionClassifier;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ChatbotBaseReplyBuilder {

    // Attributes
    private final ChatbotCourtesyReplyBuilder courtesyReplyBuilder;
    private final ChatbotGeneralReplyBuilder generalReplyBuilder;
    private final ChatbotPlatformReplyBuilder platformReplyBuilder;
    private final ChatbotQuestionClassifier chatbotQuestionClassifier;
    private final ChatbotDocumentContextService chatbotDocumentContextService;

    // Constructor
    public ChatbotBaseReplyBuilder(
            ChatbotCourtesyReplyBuilder courtesyReplyBuilder,
            ChatbotGeneralReplyBuilder generalReplyBuilder,
            ChatbotPlatformReplyBuilder platformReplyBuilder,
            ChatbotQuestionClassifier chatbotQuestionClassifier,
            ChatbotDocumentContextService chatbotDocumentContextService
    ) {
        this.courtesyReplyBuilder = courtesyReplyBuilder;
        this.generalReplyBuilder = generalReplyBuilder;
        this.platformReplyBuilder = platformReplyBuilder;
        this.chatbotQuestionClassifier = chatbotQuestionClassifier;
        this.chatbotDocumentContextService = chatbotDocumentContextService;
    }

    /* --- MANAGEMENT OF COURTESY MESSAGE ---------------------------- */

    public boolean isCourtesyMessage(String message) {
        return this.courtesyReplyBuilder.isCourtesyMessage(message);
    }

    public String courtesyReply(ConversationProfileType profile) {
        return this.courtesyReplyBuilder.courtesyReply(profile);
    }

    /* --- MANAGEMENT OF GENERAL REPLY ------------------------------- */

    public String generalStartReply(ConversationProfileType profile) {
        return this.generalReplyBuilder.generalStartReply(profile);
    }

    public String generalFaqReply(ConversationProfileType profile, String userMessage) {
        return this.generalReplyBuilder.generalFaqReply(profile, userMessage);
    }

    /* PLATFORM CONTEXT ---------------------------------------------- */

    public String contextualPlatformReply(
            ConversationProfileType profile,
            String userMessage,
            Conversation conversation,
            ChatbotPlatformContext platformContext
    ) {
        PlatformQuestionType questionType = this.classifyQuestion(userMessage);

        return this.platformReplyBuilder.contextualPlatformReply(
                profile,
                conversation,
                platformContext,
                questionType,
                this.chatbotDocumentContextService
        );
    }

    private PlatformQuestionType classifyQuestion(String userMessage) {
        return Optional.ofNullable(this.chatbotQuestionClassifier.classify(userMessage))
                .orElse(PlatformQuestionType.GENERAL_CONTEXT);
    }


    /* INTERNAL MANAGEMENT OF BASE REPLY */
    private String buildContextUnavailableStatusReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_STATUS_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_STATUS_REPLY;
        };
    }

    private String buildContextUnavailableEventsReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_EVENTS_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_EVENTS_REPLY;
        };
    }

    private String buildContextUnavailableDocumentsReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_DOCUMENTS_STUB_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_DOCUMENTS_STUB_REPLY;
        };
    }

    private String buildContextUnavailableLegalTasksReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_LEGAL_TASKS_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_LEGAL_TASKS_REPLY;
        };
    }

    private String buildContextUnavailableGeneralReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_GENERAL_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_GENERAL_REPLY;
        };
    }

    public String contextualFallbackReply(
            ConversationProfileType profile,
            String userMessage
    ) {
        PlatformQuestionType questionType = this.chatbotQuestionClassifier.classify(userMessage);

        if (questionType == null) {
            return ChatbotResponseMessages.CONTEXTUAL_PLATFORM_DATA_UNAVAILABLE_REPLY;
        }

        return switch (questionType) {
            case ENGAGEMENT_STATUS -> this.buildContextUnavailableStatusReply(profile);
            case TIMELINE_EVENTS -> this.buildContextUnavailableEventsReply(profile);
            case DOCUMENTS -> this.buildContextUnavailableDocumentsReply(profile);
            case LEGAL_TASKS -> this.buildContextUnavailableLegalTasksReply(profile);
            case GENERAL_CONTEXT -> this.buildContextUnavailableGeneralReply(profile);
        };
    }
}

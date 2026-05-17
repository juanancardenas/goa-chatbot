package es.upm.api.domain.services.basereply;

import es.upm.api.domain.common.ChatbotResponseMessages;
import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.PlatformQuestionType;
import es.upm.api.domain.services.classification.ChatbotQuestionClassifier;
import org.springframework.stereotype.Service;

@Service
public class ChatbotContextualReplyBuilder {

    private final ChatbotQuestionClassifier chatbotQuestionClassifier;

    public ChatbotContextualReplyBuilder(ChatbotQuestionClassifier chatbotQuestionClassifier) {
        this.chatbotQuestionClassifier = chatbotQuestionClassifier;
    }

    public String contextualReply(
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
}

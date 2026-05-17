package es.upm.api.domain.services.reply.base;

import es.upm.api.domain.common.ChatbotResponseMessages;
import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.PlatformQuestionType;
import es.upm.api.domain.services.classification.ChatbotQuestionClassifier;
import org.springframework.stereotype.Service;

import java.util.Locale;

import static es.upm.api.domain.services.classification.ChatbotQuestionTypes.classifyOrGeneralContext;

@Service
public class ChatbotGeneralReplyBuilder {

    private final ChatbotQuestionClassifier chatbotQuestionClassifier;

    public ChatbotGeneralReplyBuilder(ChatbotQuestionClassifier chatbotQuestionClassifier) {
        this.chatbotQuestionClassifier = chatbotQuestionClassifier;
    }

    public String generalStartReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_START_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_START_REPLY;
        };
    }

    public String generalFaqReply(
            ConversationProfileType profile,
            String userMessage
    ) {
        PlatformQuestionType questionType = classifyOrGeneralContext(this.chatbotQuestionClassifier, userMessage);

        return switch (questionType) {
            case ENGAGEMENT_STATUS -> this.buildGeneralStatusReply(profile, userMessage);
            case LEGAL_TASKS -> this.buildGeneralLegalTasksReply(profile, userMessage);
            case TIMELINE_EVENTS -> this.buildGeneralTimelineReply(profile, userMessage);
            case DOCUMENTS -> this.buildGeneralDocumentsReply(profile);
            case GENERAL_CONTEXT -> this.buildGeneralContextReply(profile);
        };
    }

    private String buildGeneralStatusReply(ConversationProfileType profile, String message) {
        if (this.asksForSpecificEngagementData(message)) {
            return switch (profile) {
                case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_STATUS_REPLY;
                case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_STATUS_REPLY;
            };
        }

        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_STATUS_EXAMPLE_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_STATUS_EXAMPLE_REPLY;
        };
    }

    private String buildGeneralTimelineReply(ConversationProfileType profile, String message) {
        if (this.asksForSpecificEngagementData(message)) {
            return switch (profile) {
                case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_TIMELINE_REPLY;
                case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_TIMELINE_REPLY;
            };
        }

        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_TIMELINE_EXAMPLE_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_TIMELINE_EXAMPLE_REPLY;
        };
    }

    private String buildGeneralDocumentsReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_DOCUMENTS_STUB_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_DOCUMENTS_STUB_REPLY;
        };
    }

    private String buildGeneralLegalTasksReply(ConversationProfileType profile, String message) {
        if (this.asksForSpecificEngagementData(message)) {
            return switch (profile) {
                case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_LEGAL_TASKS_REPLY;
                case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_LEGAL_TASKS_REPLY;
            };
        }

        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_LEGAL_TASKS_EXAMPLE_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_LEGAL_TASKS_EXAMPLE_REPLY;
        };
    }

    private String buildGeneralContextReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_CONTEXT_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_CONTEXT_REPLY;
        };
    }

    private boolean asksForSpecificEngagementData(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String normalized = message.toLowerCase(Locale.ROOT);

        return normalized.contains("este encargo")
                || normalized.contains("mi encargo")
                || normalized.contains("del encargo")
                || normalized.contains("de un encargo")
                || normalized.contains("este caso")
                || normalized.contains("mi caso")
                || normalized.contains("del caso")
                || normalized.contains("esta hoja de encargo")
                || normalized.contains("mi hoja de encargo")
                || normalized.contains("del expediente")
                || normalized.contains("mi expediente")
                || normalized.contains("este expediente");
    }
}

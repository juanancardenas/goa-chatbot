package es.upm.api.domain.services.basereply;

import es.upm.api.domain.common.ChatbotResponseMessages;
import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.PlatformQuestionType;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.services.classification.ChatbotQuestionClassifier;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatbotBaseReplyBuilder {

    private final ChatbotCourtesyReplyBuilder courtesyReplyBuilder;
    private final ChatbotQuestionClassifier chatbotQuestionClassifier;
    private final ChatbotDocumentContextService chatbotDocumentContextService;

    /**
     * Constructor
     */
    public ChatbotBaseReplyBuilder(
            ChatbotCourtesyReplyBuilder courtesyReplyBuilder,
            ChatbotQuestionClassifier chatbotQuestionClassifier,
            ChatbotDocumentContextService chatbotDocumentContextService
    ) {
        this.courtesyReplyBuilder = courtesyReplyBuilder;
        this.chatbotQuestionClassifier = chatbotQuestionClassifier;
        this.chatbotDocumentContextService = chatbotDocumentContextService;
    }

    // MANAGEMENT OF COURTESY MESSAGE ///////////////////////////////////

    public boolean isCourtesyMessage(String message) {
        return this.courtesyReplyBuilder.isCourtesyMessage(message);
    }

    public String courtesyReply(ConversationProfileType profile) {
        return this.courtesyReplyBuilder.courtesyReply(profile);
    }

    // MANAGEMENT OF GENERAL REPLY //////////////////////////////////////

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
        PlatformQuestionType questionType = this.classifyQuestion(userMessage);

        return switch (questionType) {
            case ENGAGEMENT_STATUS -> this.buildGeneralStatusReply(profile, userMessage);
            case LEGAL_TASKS -> this.buildGeneralLegalTasksReply(profile, userMessage);
            case TIMELINE_EVENTS -> this.buildGeneralTimelineReply(profile, userMessage);
            case DOCUMENTS -> this.buildGeneralDocumentsReply(profile);
            case GENERAL_CONTEXT -> this.buildGeneralContextReply(profile);
        };
    }


    /* TERCER BLOQUE: PLATFORM CONTEXT ********************************************************************/



    public String contextualPlatformReply(
            ConversationProfileType profile,
            String userMessage,
            Conversation conversation,
            ChatbotPlatformContext platformContext
    ) {
        PlatformQuestionType questionType = this.classifyQuestion(userMessage);

        return switch (questionType) {
            case ENGAGEMENT_STATUS -> this.buildEngagementStatusReply(profile, platformContext);
            case LEGAL_TASKS -> this.buildLegalTasksReply(profile, platformContext);
            case TIMELINE_EVENTS -> this.buildTimelineReply(profile, platformContext);
            case DOCUMENTS -> this.buildDocumentsReply(profile, conversation, platformContext);
            case GENERAL_CONTEXT -> this.buildGeneralContextReply(profile, platformContext);
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


    private String buildEngagementStatusReply(
            ConversationProfileType profile,
            ChatbotPlatformContext platformContext
    ) {
        String base = switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_STATUS_REPLY_TEMPLATE.formatted(
                    platformContext.getEngagementLetterId(),
                    platformContext.getOwnerDisplayName()
            );
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_STATUS_REPLY_TEMPLATE.formatted(
                    platformContext.getEngagementLetterId(),
                    platformContext.getOwnerDisplayName()
            );
        };

        StringBuilder reply = new StringBuilder(base);

        if (platformContext.getProcedureTitles() != null && !platformContext.getProcedureTitles().isEmpty()) {
            String procedures = String.join(", ", platformContext.getProcedureTitles());
            String proceduresReply = switch (profile) {
                case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_PROCEDURES_REPLY_TEMPLATE.formatted(procedures);
                case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_PROCEDURES_REPLY_TEMPLATE.formatted(procedures);
            };
            reply.append(" ").append(proceduresReply);
        }

        return reply.toString();
    }

    private String buildTimelineReply(
            ConversationProfileType profile,
            ChatbotPlatformContext platformContext
    ) {
        boolean hasRecentEvents = platformContext.getRecentEventSummaries() != null
                && !platformContext.getRecentEventSummaries().isEmpty();

        boolean hasProcedures = platformContext.getProcedureTitles() != null
                && !platformContext.getProcedureTitles().isEmpty();

        StringBuilder reply = new StringBuilder();

        if (hasRecentEvents) {
            String recentEvents = String.join(", ", platformContext.getRecentEventSummaries());
            reply.append(
                    switch (profile) {
                        case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_EVENTS_REPLY_TEMPLATE.formatted(recentEvents);
                        case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_EVENTS_REPLY_TEMPLATE.formatted(recentEvents);
                    }
            );
        } else {
            reply.append(
                    switch (profile) {
                        case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_NO_EVENTS_REPLY;
                        case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_NO_EVENTS_REPLY;
                    }
            );
        }

        if (hasProcedures) {
            String procedures = String.join(", ", platformContext.getProcedureTitles());
            reply.append(" ").append(
                    switch (profile) {
                        case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_PROCEDURES_REPLY_TEMPLATE.formatted(procedures);
                        case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_PROCEDURES_REPLY_TEMPLATE.formatted(procedures);
                    }
            );
        }

        return reply.toString();
    }

    private String buildDocumentsReply(
            ConversationProfileType profile,
            Conversation conversation,
            ChatbotPlatformContext platformContext
    ) {
        var documentContext = this.chatbotDocumentContextService.loadDocumentContext(conversation);

        StringBuilder reply = new StringBuilder(
                switch (profile) {
                    case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_DOCUMENTS_STUB_REPLY;
                    case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_DOCUMENTS_STUB_REPLY;
                }
        );

        if (platformContext.getProcedureTitles() != null && !platformContext.getProcedureTitles().isEmpty()) {
            String procedures = String.join(", ", platformContext.getProcedureTitles());
            reply.append(" ").append(
                    switch (profile) {
                        case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_PROCEDURES_REPLY_TEMPLATE.formatted(procedures);
                        case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_PROCEDURES_REPLY_TEMPLATE.formatted(procedures);
                    }
            );
        }

        if (documentContext != null
                && documentContext.getVisibleDocumentTitles() != null
                && !documentContext.getVisibleDocumentTitles().isEmpty()) {
            reply.append(" Documentos visibles preparados para futura integración: ")
                    .append(String.join(", ", documentContext.getVisibleDocumentTitles()))
                    .append(".");
        }

        return reply.toString();
    }

    private String buildLegalTasksReply(
            ConversationProfileType profile,
            ChatbotPlatformContext platformContext
    ) {
        boolean hasLegalTasks = platformContext.getLegalTaskSummaries() != null
                && !platformContext.getLegalTaskSummaries().isEmpty();

        if (!hasLegalTasks) {
            return switch (profile) {
                case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_NO_LEGAL_TASKS_REPLY;
                case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_NO_LEGAL_TASKS_REPLY;
            };
        }

        String legalTasks = platformContext.getLegalTaskSummaries().stream()
                .map(task -> "- " + task)
                .collect(Collectors.joining(System.lineSeparator()));

        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_LEGAL_TASKS_REPLY_TEMPLATE.formatted(legalTasks);
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_LEGAL_TASKS_REPLY_TEMPLATE.formatted(legalTasks);
        };
    }

    private String buildGeneralContextReply(
            ConversationProfileType profile,
            ChatbotPlatformContext platformContext
    ) {
        StringBuilder reply = new StringBuilder(
                switch (profile) {
                    case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_GENERAL_SUMMARY_REPLY;
                    case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_GENERAL_SUMMARY_REPLY;
                }
        );

        reply.append(" ").append(this.buildEngagementStatusReply(profile, platformContext));

        if (platformContext.getRecentEventSummaries() != null && !platformContext.getRecentEventSummaries().isEmpty()) {
            String recentEvents = String.join(", ", platformContext.getRecentEventSummaries());
            String eventsReply = switch (profile) {
                case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_EVENTS_REPLY_TEMPLATE.formatted(recentEvents);
                case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_EVENTS_REPLY_TEMPLATE.formatted(recentEvents);
            };
            reply.append(" ").append(eventsReply);
        } else {
            String noEventsReply = switch (profile) {
                case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_NO_EVENTS_REPLY;
                case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_NO_EVENTS_REPLY;
            };
            reply.append(" ").append(noEventsReply);
        }

        if (platformContext.getLegalTaskSummaries() != null && !platformContext.getLegalTaskSummaries().isEmpty()) {
            reply.append(" ").append(this.buildLegalTasksReply(profile, platformContext));
        }

        return reply.toString();
    }







    /* VER LUEGO !!! *********/


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

    private PlatformQuestionType classifyQuestion(String userMessage) {
        return Optional.ofNullable(this.chatbotQuestionClassifier.classify(userMessage))
                .orElse(PlatformQuestionType.GENERAL_CONTEXT);
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

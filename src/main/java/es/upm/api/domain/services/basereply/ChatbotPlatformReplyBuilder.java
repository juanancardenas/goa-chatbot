package es.upm.api.domain.services.basereply;

import es.upm.api.domain.common.ChatbotResponseMessages;
import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.PlatformQuestionType;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class ChatbotPlatformReplyBuilder {

    public String contextualPlatformReply(
            ConversationProfileType profile,
            Conversation conversation,
            ChatbotPlatformContext platformContext,
            PlatformQuestionType questionType,
            ChatbotDocumentContextService documentContextService
    ) {

        return switch (questionType) {
            case ENGAGEMENT_STATUS -> this.buildEngagementStatusReply(profile, platformContext);
            case LEGAL_TASKS -> this.buildLegalTasksReply(profile, platformContext);
            case TIMELINE_EVENTS -> this.buildTimelineReply(profile, platformContext);
            case DOCUMENTS -> this.buildDocumentsReply(profile, conversation, platformContext, documentContextService);
            case GENERAL_CONTEXT -> this.buildGeneralContextReply(profile, platformContext);
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
            ChatbotPlatformContext platformContext,
            ChatbotDocumentContextService documentContextService
    ) {
        var documentContext = documentContextService.loadDocumentContext(conversation);

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
            reply.append("Documentos visibles preparados para futura integración: ")
                    .append(String.join(", ", documentContext.getVisibleDocumentTitles()))
                    .append(".");
        }

        return reply.toString();
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
}

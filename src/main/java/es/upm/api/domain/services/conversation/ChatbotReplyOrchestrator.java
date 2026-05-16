package es.upm.api.domain.services.conversation;

import es.upm.api.domain.common.ChatbotResponseMessages;
import es.upm.api.domain.enums.ChatbotResponseMode;
import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.chatbot.reply.ChatbotReplyDecision;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.services.aireply.ChatbotAiReplyService;
import es.upm.api.domain.services.basereply.ChatbotBaseReplyBuilder;
import es.upm.api.domain.services.basereply.ChatbotPlatformContextService;
import es.upm.api.domain.services.policies.ChatbotScopeDecision;
import es.upm.api.domain.services.policies.ChatbotScopePolicy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatbotReplyOrchestrator {

    private static final Pattern ENGAGEMENT_ID_PATTERN = Pattern.compile("\\bEL-\\d+\\b", Pattern.CASE_INSENSITIVE);

    private final ChatbotBaseReplyBuilder chatbotBaseReplyBuilder;
    private final ChatbotAiReplyService chatbotAiReplyService;
    private final ChatbotPlatformContextService chatbotPlatformContextService;
    private final ChatbotScopePolicy chatbotScopePolicy;

    public ChatbotReplyOrchestrator(
            ChatbotBaseReplyBuilder chatbotBaseReplyBuilder,
            ChatbotAiReplyService chatbotAiReplyService,
            ChatbotPlatformContextService chatbotPlatformContextService,
            ChatbotScopePolicy chatbotScopePolicy
    ) {
        this.chatbotBaseReplyBuilder = chatbotBaseReplyBuilder;
        this.chatbotAiReplyService = chatbotAiReplyService;
        this.chatbotPlatformContextService = chatbotPlatformContextService;
        this.chatbotScopePolicy = chatbotScopePolicy;
    }

    public ChatbotReplyDecision resolveReply(
            Conversation conversation,
            ConversationProfileType profile,
            String userMessage
    ) {
        if (this.chatbotBaseReplyBuilder.isCourtesyMessage(userMessage)) {
            return this.replyDecision(
                    this.chatbotBaseReplyBuilder.courtesyReply(profile),
                    ChatbotResponseMode.GENERAL,
                    false,
                    List.of()
            );
        }

        if (this.referencesAnotherEngagement(conversation, userMessage)) {
            return this.replyDecision(
                    ChatbotResponseMessages.OUT_OF_CASE_SCOPE_REPLY,
                    ChatbotResponseMode.CONTEXTUAL_RESTRICTED,
                    false,
                    List.of()
            );
        }

        ChatbotScopeDecision scopeDecision = this.chatbotScopePolicy.evaluate(
                conversation,
                userMessage
        );

        if (!scopeDecision.isAllowed()) {
            return this.replyDecision(
                    scopeDecision.getSafeMessage(),
                    this.restrictedResponseMode(conversation),
                    false,
                    List.of()
            );
        }

        if (this.isContextualConversation(conversation)) {
            return this.resolveContextualReply(
                    conversation,
                    profile,
                    userMessage
            );
        }

        return this.resolveGeneralReply(
                conversation,
                profile,
                userMessage
        );
    }

    public ChatbotReplyDecision resolveGeneralStartReply(
            Conversation conversation,
            ConversationProfileType profile,
            String userMessage
    ) {
        String baseReply = this.chatbotBaseReplyBuilder.generalStartReply(profile);

        String assistantReply = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                profile,
                userMessage,
                baseReply,
                Optional.empty()
        );

        return this.replyDecision(
                assistantReply,
                ChatbotResponseMode.GENERAL,
                false,
                List.of()
        );
    }

    private ChatbotReplyDecision resolveContextualReply(
            Conversation conversation,
            ConversationProfileType profile,
            String userMessage
    ) {
        Optional<ChatbotPlatformContext> platformContext = this.chatbotPlatformContextService
                .loadContext(conversation.getEngagementLetterId());

        if (platformContext.isEmpty()) {
            String baseReply = this.chatbotBaseReplyBuilder.contextualFallbackReply(
                    profile,
                    userMessage
            );

            String assistantReply = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                    conversation,
                    profile,
                    userMessage,
                    baseReply,
                    Optional.empty()
            );

            return this.replyDecision(
                    assistantReply,
                    ChatbotResponseMode.CONTEXTUAL_RESTRICTED,
                    false,
                    List.of()
            );
        }

        ChatbotPlatformContext context = platformContext.get();
        String baseReply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                profile,
                userMessage,
                conversation,
                context
        );

        String assistantReply = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                profile,
                userMessage,
                baseReply,
                platformContext
        );

        return this.replyDecision(
                assistantReply,
                ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA,
                true,
                context.getSourcesSummary()
        );
    }

    private ChatbotReplyDecision resolveGeneralReply(
            Conversation conversation,
            ConversationProfileType profile,
            String userMessage
    ) {
        String baseReply = this.chatbotBaseReplyBuilder.generalFaqReply(
                profile,
                userMessage
        );

        String assistantReply = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                profile,
                userMessage,
                baseReply,
                Optional.empty()
        );

        return this.replyDecision(
                assistantReply,
                ChatbotResponseMode.GENERAL,
                false,
                List.of()
        );
    }

    private ChatbotReplyDecision replyDecision(
            String assistantReply,
            ChatbotResponseMode responseMode,
            boolean usedPlatformData,
            List<String> sourcesSummary
    ) {
        return ChatbotReplyDecision.builder()
                .assistantReply(assistantReply)
                .responseMode(responseMode)
                .usedPlatformData(usedPlatformData)
                .sourcesSummary(this.safeSourcesSummary(sourcesSummary))
                .build();
    }

    private List<String> safeSourcesSummary(List<String> sourcesSummary) {
        if (sourcesSummary == null) {
            return List.of();
        }

        return sourcesSummary;
    }

    private ChatbotResponseMode restrictedResponseMode(Conversation conversation) {
        if (this.isContextualConversation(conversation)) {
            return ChatbotResponseMode.CONTEXTUAL_RESTRICTED;
        }

        return ChatbotResponseMode.GENERAL;
    }

    private boolean isContextualConversation(Conversation conversation) {
        return ConversationType.CONTEXTUAL == conversation.getType()
                && conversation.getEngagementLetterId() != null;
    }

    private boolean referencesAnotherEngagement(Conversation conversation, String message) {
        if (ConversationType.CONTEXTUAL != conversation.getType() || message == null || message.isBlank()) {
            return false;
        }

        String activeEngagementId = this.safeText(conversation.getEngagementLetterId(), "");
        Matcher matcher = ENGAGEMENT_ID_PATTERN.matcher(message);

        while (matcher.find()) {
            String candidate = matcher.group();

            if (!candidate.equalsIgnoreCase(activeEngagementId)) {
                return true;
            }
        }

        return false;
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }
}

package es.upm.api.domain.services;

import es.upm.api.configurations.ChatbotAiProperties;
import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.enums.ChatbotResponseMode;
import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.domain.enums.PlatformQuestionType;
import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.services.aireply.ChatbotAiReplyService;
import es.upm.api.domain.services.basereply.ChatbotBaseReplyBuilder;
import es.upm.api.domain.services.basereply.ChatbotPlatformContextService;
import es.upm.api.domain.services.classification.ChatbotQuestionClassifier;
import es.upm.api.domain.services.conversation.*;
import es.upm.api.domain.services.policies.ChatbotScopeDecision;
import es.upm.api.domain.services.policies.ChatbotScopePolicy;
import es.upm.api.domain.common.ChatbotResponseMessages;
import es.upm.api.domain.model.configuration.ChatbotConfigurationStatus;
import es.upm.api.domain.model.configuration.ChatbotMessageCommand;
import es.upm.api.domain.model.configuration.ChatbotMessageResult;
import es.upm.api.domain.model.configuration.ChatbotContextualConversationCommand;
import es.upm.api.domain.model.configuration.ChatbotContextualConversationResult;
import es.upm.api.domain.model.configuration.ChatbotConversationHistoryResult;
import es.upm.api.domain.model.configuration.ChatbotConversationSummaryResult;
import es.upm.api.domain.model.configuration.AuthenticatedUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatbotService {

    // Constants
    private static final Pattern ENGAGEMENT_ID_PATTERN = Pattern.compile("\\bEL-\\d+\\b", Pattern.CASE_INSENSITIVE);

    // Attributes
    private final ChatbotMessageService chatbotMessageService;
    private final ChatbotResponseSanitizer chatbotResponseSanitizer;
    private final ChatbotConversationService chatbotConversationService;
    private final ChatbotHistoryService chatbotHistoryService;
    private final ChatbotEscalationService chatbotEscalationService;
    private final ChatbotBaseReplyBuilder chatbotBaseReplyBuilder;
    private final ChatbotAiReplyService chatbotAiReplyService;
    private final ChatbotPlatformContextService chatbotPlatformContextService;
    private final ChatbotQuestionClassifier chatbotQuestionClassifier;
    private final ChatbotScopePolicy chatbotScopePolicy;
    private final ChatbotAiProperties chatbotAiProperties;

    // Constructors
    @Autowired
    public ChatbotService(ChatbotMessageService chatbotMessageService,
                          ChatbotResponseSanitizer chatbotResponseSanitizer,
                          ChatbotConversationService chatbotConversationService,
                          ChatbotHistoryService chatbotHistoryService,
                          ChatbotEscalationService chatbotEscalationService,
                          ChatbotBaseReplyBuilder chatbotBaseReplyBuilder,
                          ChatbotAiReplyService chatbotAiReplyService,
                          ChatbotPlatformContextService chatbotPlatformContextService,
                          ChatbotQuestionClassifier chatbotQuestionClassifier,
                          ChatbotScopePolicy chatbotScopePolicy,
                          ChatbotAiProperties chatbotAiProperties
    ) {
        this.chatbotMessageService = chatbotMessageService;
        this.chatbotResponseSanitizer = chatbotResponseSanitizer;
        this.chatbotConversationService = chatbotConversationService;
        this.chatbotHistoryService = chatbotHistoryService;
        this.chatbotEscalationService = chatbotEscalationService;
        this.chatbotBaseReplyBuilder = chatbotBaseReplyBuilder;
        this.chatbotAiReplyService = chatbotAiReplyService;
        this.chatbotPlatformContextService = chatbotPlatformContextService;
        this.chatbotQuestionClassifier = chatbotQuestionClassifier;
        this.chatbotScopePolicy = chatbotScopePolicy;
        this.chatbotAiProperties = chatbotAiProperties;
    }

    /**
     * Starts Contextual Conversation, this type of conversation is receiving an EngagementLetter ID
     */
    public ChatbotContextualConversationResult startContextualConversation(
            AuthenticatedUserContext authenticatedUser,
            ChatbotContextualConversationCommand command
    ) {
        Conversation conversation = this.chatbotConversationService.findOrCreateContextualConversation(
                authenticatedUser.getUserId(),
                command.getEngagementLetterId()
        );

        return ChatbotContextualConversationResult.builder()
                .conversationId(conversation.getId())
                .engagementLetterId(conversation.getEngagementLetterId())
                .createdAt(conversation.getCreatedAt().toString())
                .error(null)
                .build();
    }

    /**
     * Reading Conversation History List to display historic of conversations
     */
    public List<ChatbotConversationSummaryResult> readConversationHistoryList(
            AuthenticatedUserContext authenticatedUser,
            String type,
            String engagementLetterId
    ) {
        return this.chatbotHistoryService.readConversationHistoryList(
                authenticatedUser.getUserId(),
                type,
                engagementLetterId
        );
    }

    /**
     * Reading the messages of a conversation
     */
    public ChatbotConversationHistoryResult readConversationHistory(
            AuthenticatedUserContext authenticatedUser,
            String conversationId,
            Integer page,
            Integer size
    ) {
        return this.chatbotHistoryService.readConversationHistory(
                authenticatedUser.getUserId(),
                conversationId,
                page,
                size
        );
    }

    /**
     * Starts General Conversation, this type of conversation is not linked to other process or entity
     */
    public ChatbotMessageResult startGeneralConversation(
            AuthenticatedUserContext authenticatedUser,
            ChatbotMessageCommand command
    ) {
        String userMessage = command.getMessage();

        this.validateUserMessageLength(userMessage);

        LocalDateTime date = LocalDateTime.now();

        Conversation conversation = this.chatbotConversationService.createGeneralConversation(
                authenticatedUser.getUserId(),
                date
        );

        Integer firstSequence = this.chatbotMessageService.reserveSequenceNumbers(conversation.getId(), 2);

        String messageId = this.chatbotMessageService.saveMessage(
                conversation.getId(),
                MessageSenderType.USER,
                MessageType.REQUEST,
                userMessage,
                firstSequence,
                null,
                date
        );

        ConversationProfileType profile = authenticatedUser.getProfile();
        String baseReply = this.chatbotBaseReplyBuilder.generalStartReply(profile);

        String assistantReply = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                profile,
                userMessage,
                baseReply,
                Optional.empty()
        );

        this.chatbotMessageService.saveMessage(
                conversation.getId(),
                MessageSenderType.ASSISTANT,
                MessageType.RESPONSE,
                assistantReply,
                firstSequence + 1,
                messageId,
                date
        );

        return ChatbotMessageResult.builder()
                .conversationId(conversation.getId())
                .message(assistantReply)
                .error(null)
                .createdAt(date.toString())
                .responseMode(ChatbotResponseMode.GENERAL)
                .usedPlatformData(false)
                .sourcesSummary(List.of())
                .build();
    }

    private ChatbotMessageResult buildMessageResult(
            String conversationId,
            String message,
            String error,
            LocalDateTime createdAt,
            ChatbotResponseMode responseMode,
            boolean usedPlatformData,
            List<String> sourcesSummary
    ) {
        return ChatbotMessageResult.builder()
                .conversationId(conversationId)
                .message(message)
                .error(error)
                .createdAt(createdAt.toString())
                .responseMode(responseMode)
                .usedPlatformData(usedPlatformData)
                .sourcesSummary(sourcesSummary)
                .build();
    }

    // Send Message: method called each time that user clicks on Sent button in the front-end
    public ChatbotMessageResult sendMessage(
            AuthenticatedUserContext authenticatedUser,
            ChatbotMessageCommand command
    ) {
        String userId = authenticatedUser.getUserId();
        LocalDateTime date = LocalDateTime.now();
        String userMessage = command.getMessage();

        if (command.getConversationId() == null || command.getConversationId().isBlank()) {
            throw new BadRequestException("conversationId es obligatorio para enviar mensajes");
        }

        this.validateUserMessageLength(userMessage);

        Conversation conversation = this.chatbotConversationService.requireActiveOwnedConversation(
                command.getConversationId(),
                userId
        );

        Integer nextSequence = this.chatbotMessageService.reserveSequenceNumbers(conversation.getId(), 2);

        String messageId = this.chatbotMessageService.saveMessage(
                conversation.getId(),
                MessageSenderType.USER,
                MessageType.REQUEST,
                userMessage,
                nextSequence,
                null,
                date
        );

        ChatbotScopeDecision scopeDecision = this.chatbotScopePolicy.evaluate(
                conversation,
                userMessage
        );

        String assistantReply;
        ChatbotResponseMode responseMode;
        boolean usedPlatformData;
        List<String> sourcesSummary;
        ConversationProfileType profile = authenticatedUser.getProfile();

        if (this.chatbotBaseReplyBuilder.isCourtesyMessage(userMessage)) {
            assistantReply = this.chatbotBaseReplyBuilder.courtesyReply(profile);
            responseMode = ChatbotResponseMode.GENERAL;
            usedPlatformData = false;
            sourcesSummary = List.of();

            this.chatbotMessageService.saveMessage(
                    conversation.getId(),
                    MessageSenderType.ASSISTANT,
                    MessageType.RESPONSE,
                    assistantReply,
                    nextSequence + 1,
                    messageId,
                    date
            );

            return this.buildMessageResult(
                    conversation.getId(),
                    assistantReply,
                    null,
                    date,
                    responseMode,
                    usedPlatformData,
                    sourcesSummary
            );
        }

        if (this.referencesAnotherEngagement(conversation, userMessage)) {
            assistantReply = ChatbotResponseMessages.OUT_OF_CASE_SCOPE_REPLY;
            responseMode = ChatbotResponseMode.CONTEXTUAL_RESTRICTED;
            usedPlatformData = false;
            sourcesSummary = List.of();

            this.chatbotMessageService.saveMessage(
                    conversation.getId(),
                    MessageSenderType.ASSISTANT,
                    MessageType.RESPONSE,
                    assistantReply,
                    nextSequence + 1,
                    messageId,
                    date
            );

            return this.buildMessageResult(
                    conversation.getId(),
                    assistantReply,
                    null,
                    date,
                    responseMode,
                    usedPlatformData,
                    sourcesSummary
            );
        }

        if (scopeDecision.isAllowed()) {
            if (ConversationType.CONTEXTUAL.name().equals(conversation.getType()) && conversation.getEngagementLetterId() != null) {
                PlatformQuestionType questionType = this.chatbotQuestionClassifier.classify(userMessage);

                if (this.requiresPlatformContext(questionType)) {
                    Optional<ChatbotPlatformContext> platformContext = this.chatbotPlatformContextService
                            .loadContext(conversation.getEngagementLetterId());

                    if (platformContext.isPresent()) {
                        String baseReply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                                profile,
                                userMessage,
                                conversation,
                                platformContext.get()
                        );

                        assistantReply = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                                conversation,
                                profile,
                                userMessage,
                                baseReply,
                                platformContext
                        );

                        responseMode = ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA;
                        usedPlatformData = true;
                        sourcesSummary = platformContext.get().getSourcesSummary();
                    } else {
                        String baseReply = this.chatbotBaseReplyBuilder.contextualFallbackReply(
                                profile,
                                userMessage
                        );

                        assistantReply = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                                conversation,
                                profile,
                                userMessage,
                                baseReply,
                                Optional.empty()
                        );

                        responseMode = ChatbotResponseMode.CONTEXTUAL_RESTRICTED;
                        usedPlatformData = false;
                        sourcesSummary = List.of();
                    }
                } else {
                    String baseReply = this.chatbotBaseReplyBuilder.generalFaqReply(
                            profile,
                            userMessage
                    );

                    assistantReply = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                            conversation,
                            profile,
                            userMessage,
                            baseReply,
                            Optional.empty()
                    );

                    responseMode = ChatbotResponseMode.GENERAL;
                    usedPlatformData = false;
                    sourcesSummary = List.of();
                }
            } else {
                String baseReply = this.chatbotBaseReplyBuilder.generalFaqReply(
                        profile,
                        userMessage
                );

                assistantReply = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                        conversation,
                        profile,
                        userMessage,
                        baseReply,
                        Optional.empty()
                );

                responseMode = ChatbotResponseMode.GENERAL;
                usedPlatformData = false;
                sourcesSummary = List.of();
            }
        } else {
            assistantReply = scopeDecision.getSafeMessage();
            responseMode = ConversationType.CONTEXTUAL.name().equals(conversation.getType())
                    ? ChatbotResponseMode.CONTEXTUAL_RESTRICTED
                    : ChatbotResponseMode.GENERAL;
            usedPlatformData = false;
            sourcesSummary = List.of();
        }

        assistantReply = this.chatbotResponseSanitizer.normalizeReplyForFrontend(assistantReply);

        this.chatbotMessageService.saveMessage(
                conversation.getId(),
                MessageSenderType.ASSISTANT,
                MessageType.RESPONSE,
                assistantReply,
                nextSequence + 1,
                messageId,
                date
        );

        return this.buildMessageResult(
                conversation.getId(),
                assistantReply,
                null,
                date,
                responseMode,
                usedPlatformData,
                sourcesSummary
        );
    }

    public ChatbotConfigurationStatus readConfigurationStatus() {
        return ChatbotConfigurationStatus.builder()
                .enabled(this.chatbotAiProperties.isEnabled())
                .provider(this.chatbotAiProperties.normalizedProvider())
                .model(this.chatbotAiProperties.getModel())
                .maxInputCharacters(this.chatbotAiProperties.getMaxInputCharacters())
                .maxOutputTokens(this.chatbotAiProperties.getMaxOutputTokens())
                .maxContextMessages(this.chatbotAiProperties.getMaxContextMessages())
                .documentsAvailable(this.chatbotAiProperties.isDocumentsAvailable())
                .build();
    }

    /**
     * Close a conversation
     * It will be triggered once the user leaves the chatbot or selects a
     * different conversation to reopen (previous would be closed)
     */
    public void closeConversation(
            AuthenticatedUserContext authenticatedUser,
            String conversationId
    ) {
        this.chatbotConversationService.closeConversation(
                conversationId,
                authenticatedUser.getUserId()
        );
    }

    /**
     * Escalate a conversation
     * It will be triggered once the user selects this option, creating a new
     * entry in the table escalations and locking the conversation
     */
    public void escalateConversation(
            AuthenticatedUserContext authenticatedUser,
            String conversationId
    ) {
        this.chatbotEscalationService.escalateConversation(
                conversationId,
                authenticatedUser.getUserId()
        );
    }

    /**
     * Delete a conversation
     * It will be triggered from list of conversations, if user runs delete action
     */
    public void deleteConversation(
            AuthenticatedUserContext authenticatedUser,
            String conversationId
    ) {
        this.chatbotConversationService.deleteConversation(
                conversationId,
                authenticatedUser.getUserId()
        );
    }

    /**
     * Reopen a conversation
     * It will be triggered from list of conversations, once user selects one
     */
    public void reopenConversation(
            AuthenticatedUserContext authenticatedUser,
            String conversationId
    ) {
        this.chatbotConversationService.reopenConversation(
                conversationId,
                authenticatedUser.getUserId()
        );
    }

    private boolean requiresPlatformContext(PlatformQuestionType questionType) {
        if (questionType == null) {
            return true;
        }

        return switch (questionType) {
            case ENGAGEMENT_STATUS, LEGAL_TASKS, TIMELINE_EVENTS, DOCUMENTS, GENERAL_CONTEXT -> true;
        };
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private void validateUserMessageLength(String message) {
        int maxInputCharacters = this.chatbotAiProperties.getMaxInputCharacters();

        if (maxInputCharacters > 0 && message != null && message.length() > maxInputCharacters) {
            throw new BadRequestException("message supera el limite maximo de caracteres configurado");
        }
    }

    private boolean referencesAnotherEngagement(Conversation conversation, String message) {
        if (!ConversationType.CONTEXTUAL.name().equals(conversation.getType()) || message == null || message.isBlank()) {
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
}

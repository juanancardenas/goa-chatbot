package es.upm.api.domain.services;

import es.upm.api.domain.enums.ChatbotResponseMode;
import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.chatbot.command.ChatbotContextualConversationCommand;
import es.upm.api.domain.model.chatbot.command.ChatbotMessageCommand;
import es.upm.api.domain.model.chatbot.reply.ChatbotReplyDecision;
import es.upm.api.domain.model.chatbot.result.ChatbotConfigurationResult;
import es.upm.api.domain.model.chatbot.result.ChatbotContextualConversationResult;
import es.upm.api.domain.model.chatbot.result.ChatbotConversationHistoryResult;
import es.upm.api.domain.model.chatbot.result.ChatbotConversationSummaryResult;
import es.upm.api.domain.model.chatbot.result.ChatbotMessageResult;
import es.upm.api.domain.model.metrics.ChatbotMessageMetric;
import es.upm.api.domain.model.safety.ChatbotModerationDecision;
import es.upm.api.domain.model.security.AuthenticatedUserContext;
import es.upm.api.domain.ports.in.CloseConversationUseCase;
import es.upm.api.domain.ports.in.DeleteConversationUseCase;
import es.upm.api.domain.ports.in.EscalateConversationUseCase;
import es.upm.api.domain.ports.in.ReadChatbotConfigurationUseCase;
import es.upm.api.domain.ports.in.ReadConversationHistoryListUseCase;
import es.upm.api.domain.ports.in.ReadConversationHistoryUseCase;
import es.upm.api.domain.ports.in.ReopenConversationUseCase;
import es.upm.api.domain.ports.in.SendChatbotMessageUseCase;
import es.upm.api.domain.ports.in.StartContextualConversationUseCase;
import es.upm.api.domain.ports.in.StartGeneralConversationUseCase;
import es.upm.api.domain.ports.out.ChatbotAiSettings;
import es.upm.api.domain.ports.out.ChatbotMetricsRecorder;
import es.upm.api.domain.services.conversation.ChatbotConversationService;
import es.upm.api.domain.services.conversation.ChatbotEscalationService;
import es.upm.api.domain.services.conversation.ChatbotHistoryService;
import es.upm.api.domain.services.conversation.ChatbotMessageService;
import es.upm.api.domain.services.conversation.ChatbotResponseSanitizer;
import es.upm.api.domain.services.reply.ChatbotReplyOrchestrator;
import es.upm.api.domain.services.safety.ChatbotModerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

@Slf4j
@Service
public class ChatbotService implements
        ReadConversationHistoryListUseCase,
        ReadConversationHistoryUseCase,
        StartContextualConversationUseCase,
        StartGeneralConversationUseCase,
        SendChatbotMessageUseCase,
        CloseConversationUseCase,
        EscalateConversationUseCase,
        DeleteConversationUseCase,
        ReopenConversationUseCase,
        ReadChatbotConfigurationUseCase {

    // Attributes
    private final ChatbotMessageService chatbotMessageService;
    private final ChatbotResponseSanitizer chatbotResponseSanitizer;
    private final ChatbotConversationService chatbotConversationService;
    private final ChatbotHistoryService chatbotHistoryService;
    private final ChatbotEscalationService chatbotEscalationService;
    private final ChatbotReplyOrchestrator chatbotReplyOrchestrator;
    private final ChatbotModerationService chatbotModerationService;
    private final ChatbotAiSettings chatbotAiSettings;
    private final ChatbotMetricsRecorder chatbotMetricsRecorder;
    private final Clock clock;

    // Constructor
    public ChatbotService(ChatbotMessageService chatbotMessageService,
                          ChatbotResponseSanitizer chatbotResponseSanitizer,
                          ChatbotConversationService chatbotConversationService,
                          ChatbotHistoryService chatbotHistoryService,
                          ChatbotEscalationService chatbotEscalationService,
                          ChatbotReplyOrchestrator chatbotReplyOrchestrator,
                          ChatbotModerationService chatbotModerationService,
                          ChatbotAiSettings chatbotAiSettings,
                          ChatbotMetricsRecorder chatbotMetricsRecorder,
                          Clock clock
    ) {
        this.chatbotMessageService = chatbotMessageService;
        this.chatbotResponseSanitizer = chatbotResponseSanitizer;
        this.chatbotConversationService = chatbotConversationService;
        this.chatbotHistoryService = chatbotHistoryService;
        this.chatbotEscalationService = chatbotEscalationService;
        this.chatbotReplyOrchestrator = chatbotReplyOrchestrator;
        this.chatbotModerationService = chatbotModerationService;
        this.chatbotAiSettings = chatbotAiSettings;
        this.chatbotMetricsRecorder = chatbotMetricsRecorder;
        this.clock = clock;
    }

    /**
     * Reading Conversation History List to display historic of conversations
     */
    @Override
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
    @Override
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
     * Starts Contextual Conversation, this type of conversation is receiving an EngagementLetter ID
     */
    @Override
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
     * Starts General Conversation, this type of conversation is not linked to other process or entity
     */
    @Override
    public ChatbotMessageResult startGeneralConversation(
            AuthenticatedUserContext authenticatedUser,
            ChatbotMessageCommand command
    ) {
        long startTime = this.clock.millis();
        ChatbotMessageMetric metric = ChatbotMessageMetric.builder()
                .success(false)
                .build();

        try {
            String userId = authenticatedUser.getUserId();
            metric.setUserId(userId);

            String userMessage = command.getMessage();

            this.validateUserMessageLength(userMessage);

            LocalDateTime date = LocalDateTime.now(this.clock);

            Conversation conversation = this.chatbotConversationService.createGeneralConversation(
                    userId,
                    date
            );

            return this.handleConversationMessage(
                    userMessage,
                    conversation,
                    date,
                    metric,
                    (conversationToResolve, messageToResolve) -> this.chatbotReplyOrchestrator.resolveGeneralStartReply(
                            conversationToResolve,
                            authenticatedUser.getProfile(),
                            messageToResolve
                    ),
                    reply -> reply
            );

        } finally {
            this.completeAndRecordMessageMetric(metric, startTime);
        }
    }

    /**
     * Send Message
     * Method called each time that user clicks on Sent button in the front-end
     */
    @Override
    public ChatbotMessageResult sendMessage(
            AuthenticatedUserContext authenticatedUser,
            ChatbotMessageCommand command
    ) {
        long startTime = this.clock.millis();
        ChatbotMessageMetric metric = ChatbotMessageMetric.builder()
                .success(false)
                .build();

        try {
            String userId = authenticatedUser.getUserId();
            metric.setUserId(userId);

            LocalDateTime date = LocalDateTime.now(this.clock);
            String userMessage = command.getMessage();

            if (command.getConversationId() == null || command.getConversationId().isBlank()) {
                throw new BadRequestException("conversationId es obligatorio para enviar mensajes");
            }

            this.validateUserMessageLength(userMessage);

            Conversation conversation = this.chatbotConversationService.requireActiveOwnedConversation(
                    command.getConversationId(),
                    userId
            );

            return this.handleConversationMessage(
                    userMessage,
                    conversation,
                    date,
                    metric,
                    (conversationToResolve, messageToResolve) -> this.chatbotReplyOrchestrator.resolveReply(
                            conversationToResolve,
                            authenticatedUser.getProfile(),
                            messageToResolve
                    ),
                    this.chatbotResponseSanitizer::normalizeReplyForFrontend
            );

        } finally {
            this.completeAndRecordMessageMetric(metric, startTime);
        }
    }

    private ChatbotMessageResult handleConversationMessage(
            String userMessage,
            Conversation conversation,
            LocalDateTime date,
            ChatbotMessageMetric metric,
            BiFunction<Conversation, String, ChatbotReplyDecision> replyResolver,
            UnaryOperator<String> assistantReplyNormalizer
    ) {
        metric.setConversationId(conversation.getId());
        metric.setConversationType(conversation.getType());

        ChatbotModerationDecision moderationDecision = this.chatbotModerationService.moderate(
                userMessage,
                conversation.getId(),
                metric.getUserId()
        );

        if (moderationDecision.isBlocked()) {
            return this.handleBlockedMessage(
                    conversation,
                    date,
                    metric,
                    moderationDecision,
                    assistantReplyNormalizer
            );
        }

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
        metric.setRequestMessageId(messageId);

        ChatbotReplyDecision decision = replyResolver.apply(conversation, userMessage);
        metric.setResponseMode(decision.getResponseMode());
        metric.setUsedAi(decision.isUsedAi());
        metric.setUsedPlatformData(decision.isUsedPlatformData());

        String assistantReply = assistantReplyNormalizer.apply(decision.getAssistantReply());

        this.chatbotMessageService.saveMessage(
                conversation.getId(),
                MessageSenderType.ASSISTANT,
                MessageType.RESPONSE,
                assistantReply,
                firstSequence + 1,
                messageId,
                date
        );

        metric.setSuccess(true);

        return buildMessageResult(
                conversation.getId(),
                assistantReply,
                null,
                date,
                decision.getResponseMode(),
                decision.isUsedPlatformData(),
                decision.getSourcesSummary()
        );
    }

    private ChatbotMessageResult handleBlockedMessage(
            Conversation conversation,
            LocalDateTime date,
            ChatbotMessageMetric metric,
            ChatbotModerationDecision moderationDecision,
            UnaryOperator<String> assistantReplyNormalizer
    ) {
        metric.setResponseMode(this.moderationResponseMode(conversation));
        metric.setUsedAi(false);
        metric.setUsedPlatformData(false);

        String assistantReply = assistantReplyNormalizer.apply(
                this.safeModerationReply(moderationDecision)
        );

        Integer sequence = this.chatbotMessageService.reserveSequenceNumbers(conversation.getId(), 1);

        this.chatbotMessageService.saveMessage(
                conversation.getId(),
                MessageSenderType.ASSISTANT,
                MessageType.RESPONSE,
                assistantReply,
                sequence,
                null,
                date
        );

        metric.setSuccess(true);

        return buildMessageResult(
                conversation.getId(),
                assistantReply,
                null,
                date,
                this.moderationResponseMode(conversation),
                false,
                List.of()
        );
    }

    private String safeModerationReply(ChatbotModerationDecision moderationDecision) {
        if (moderationDecision.getSafeReply() == null || moderationDecision.getSafeReply().isBlank()) {
            return "No puedo procesar este mensaje porque puede contener información sensible "
                    + "o una solicitud no permitida. Por favor, revisa el contenido y vuelve a intentarlo.";
        }

        return moderationDecision.getSafeReply();
    }

    private ChatbotResponseMode moderationResponseMode(Conversation conversation) {
        if (conversation.getType() != null && conversation.getEngagementLetterId() != null) {
            return ChatbotResponseMode.CONTEXTUAL_RESTRICTED;
        }

        return ChatbotResponseMode.GENERAL;
    }

    private void completeAndRecordMessageMetric(ChatbotMessageMetric metric, long startTime) {
        metric.setDurationMs(this.clock.millis() - startTime);
        metric.setCreatedAt(LocalDateTime.now(this.clock));
        this.recordMessageMetricSafely(metric);
    }

    private void recordMessageMetricSafely(ChatbotMessageMetric metric) {
        try {
            this.chatbotMetricsRecorder.recordMessageHandled(metric);
        } catch (RuntimeException exception) {
            log.warn(
                    "Message metric recording failed. conversationId={}, reason={}",
                    metric.getConversationId(),
                    exception.getClass().getSimpleName()
            );
        }
    }

    private static ChatbotMessageResult buildMessageResult(
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

    /**
     * Close a conversation
     * It will be triggered once the user leaves the chatbot or selects a
     * different conversation to reopen (previous would be closed)
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
    public void reopenConversation(
            AuthenticatedUserContext authenticatedUser,
            String conversationId
    ) {
        this.chatbotConversationService.reopenConversation(
                conversationId,
                authenticatedUser.getUserId()
        );
    }

    /**
     * Reads the AI configuration set in the model
     */
    @Override
    public ChatbotConfigurationResult readConfigurationStatus() {
        return ChatbotConfigurationResult.builder()
                .enabled(this.chatbotAiSettings.isEnabled())
                .provider(this.chatbotAiSettings.provider())
                .model(this.chatbotAiSettings.model())
                .maxInputCharacters(this.chatbotAiSettings.maxInputCharacters())
                .maxOutputTokens(this.chatbotAiSettings.maxOutputTokens())
                .maxContextMessages(this.chatbotAiSettings.maxContextMessages())
                .documentsAvailable(this.chatbotAiSettings.documentsAvailable())
                .build();
    }

    /**
     * Checks the message entered by user has not reached the configured limit
     */
    private void validateUserMessageLength(String message) {
        int maxInputCharacters = this.chatbotAiSettings.maxInputCharacters();

        if (maxInputCharacters > 0 && message != null && message.length() > maxInputCharacters) {
            throw new BadRequestException("message supera el limite maximo de caracteres configurado");
        }
    }
}

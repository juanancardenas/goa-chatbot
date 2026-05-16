package es.upm.api.domain.services;

import es.upm.api.domain.enums.ChatbotResponseMode;
import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.ports.out.ChatbotAiSettings;
import es.upm.api.domain.services.conversation.ChatbotConversationService;
import es.upm.api.domain.services.conversation.ChatbotEscalationService;
import es.upm.api.domain.services.conversation.ChatbotHistoryService;
import es.upm.api.domain.services.conversation.ChatbotMessageService;
import es.upm.api.domain.services.conversation.ChatbotReplyOrchestrator;
import es.upm.api.domain.services.conversation.ChatbotResponseSanitizer;
import es.upm.api.domain.model.chatbot.result.ChatbotConfigurationResult;
import es.upm.api.domain.model.chatbot.command.ChatbotMessageCommand;
import es.upm.api.domain.model.chatbot.result.ChatbotMessageResult;
import es.upm.api.domain.model.chatbot.command.ChatbotContextualConversationCommand;
import es.upm.api.domain.model.chatbot.result.ChatbotContextualConversationResult;
import es.upm.api.domain.model.chatbot.result.ChatbotConversationHistoryResult;
import es.upm.api.domain.model.chatbot.result.ChatbotConversationSummaryResult;
import es.upm.api.domain.model.security.AuthenticatedUserContext;
import es.upm.api.domain.model.chatbot.reply.ChatbotReplyDecision;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatbotService {

    // Attributes
    private final ChatbotMessageService chatbotMessageService;
    private final ChatbotResponseSanitizer chatbotResponseSanitizer;
    private final ChatbotConversationService chatbotConversationService;
    private final ChatbotHistoryService chatbotHistoryService;
    private final ChatbotEscalationService chatbotEscalationService;
    private final ChatbotReplyOrchestrator chatbotReplyOrchestrator;
    private final ChatbotAiSettings chatbotAiSettings;

    // Constructor
    public ChatbotService(ChatbotMessageService chatbotMessageService,
                          ChatbotResponseSanitizer chatbotResponseSanitizer,
                          ChatbotConversationService chatbotConversationService,
                          ChatbotHistoryService chatbotHistoryService,
                          ChatbotEscalationService chatbotEscalationService,
                          ChatbotReplyOrchestrator chatbotReplyOrchestrator,
                          ChatbotAiSettings chatbotAiSettings
    ) {
        this.chatbotMessageService = chatbotMessageService;
        this.chatbotResponseSanitizer = chatbotResponseSanitizer;
        this.chatbotConversationService = chatbotConversationService;
        this.chatbotHistoryService = chatbotHistoryService;
        this.chatbotEscalationService = chatbotEscalationService;
        this.chatbotReplyOrchestrator = chatbotReplyOrchestrator;
        this.chatbotAiSettings = chatbotAiSettings;
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

        ChatbotReplyDecision decision = this.chatbotReplyOrchestrator.resolveGeneralStartReply(
                conversation,
                authenticatedUser.getProfile(),
                userMessage
        );
        String assistantReply = decision.getAssistantReply();

        this.chatbotMessageService.saveMessage(
                conversation.getId(),
                MessageSenderType.ASSISTANT,
                MessageType.RESPONSE,
                assistantReply,
                firstSequence + 1,
                messageId,
                date
        );

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

    /**
     * Send Message
     * Method called each time that user clicks on Sent button in the front-end
     */
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

        ChatbotReplyDecision decision = this.chatbotReplyOrchestrator.resolveReply(
                conversation,
                authenticatedUser.getProfile(),
                userMessage
        );

        String assistantReply = this.chatbotResponseSanitizer.normalizeReplyForFrontend(
                decision.getAssistantReply()
        );

        this.chatbotMessageService.saveMessage(
                conversation.getId(),
                MessageSenderType.ASSISTANT,
                MessageType.RESPONSE,
                assistantReply,
                nextSequence + 1,
                messageId,
                date
        );

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

    /**
     * Reads the AI configuration set in the model
     */
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

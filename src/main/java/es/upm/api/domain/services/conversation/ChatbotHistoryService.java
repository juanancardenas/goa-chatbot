package es.upm.api.domain.services.conversation;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.Message;
import es.upm.api.domain.model.chatbot.result.ChatbotConversationHistoryResult;
import es.upm.api.domain.model.chatbot.result.ChatbotConversationSummaryResult;
import es.upm.api.domain.model.chatbot.result.ChatbotHistoryMessageResult;
import es.upm.api.domain.model.pagination.PageResult;
import es.upm.api.domain.ports.out.ConversationGateway;
import es.upm.api.domain.ports.out.MessageGateway;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ChatbotHistoryService {

    private static final int DEFAULT_HISTORY_PAGE = 0;
    private static final int DEFAULT_HISTORY_SIZE = 20;
    private static final int MAX_HISTORY_SIZE = 100;

    private final ConversationGateway conversationGateway;
    private final MessageGateway messageGateway;
    private final ChatbotConversationService chatbotConversationService;
    private final ChatbotMessageService chatbotMessageService;

    public ChatbotHistoryService(
            ConversationGateway conversationGateway,
            MessageGateway messageGateway,
            ChatbotConversationService chatbotConversationService,
            ChatbotMessageService chatbotMessageService
    ) {
        this.conversationGateway = conversationGateway;
        this.messageGateway = messageGateway;
        this.chatbotConversationService = chatbotConversationService;
        this.chatbotMessageService = chatbotMessageService;
    }

    public List<ChatbotConversationSummaryResult> readConversationHistoryList(
            String userId,
            String type,
            String engagementLetterId
    ) {
        ConversationType normalizedType = this.normalizeConversationType(type);

        List<Conversation> conversations = ConversationType.CONTEXTUAL == normalizedType
                ? this.readContextualConversations(userId, engagementLetterId)
                : this.conversationGateway.findByUserIdAndTypeOrderByCreatedAtDesc(userId, normalizedType);

        return conversations.stream()
                .map(this::toConversationSummaryResult)
                .toList();
    }

    public ChatbotConversationHistoryResult readConversationHistory(
            String userId,
            String conversationId,
            Integer page,
            Integer size
    ) {
        Conversation conversation = this.chatbotConversationService.requireOwnedConversation(
                conversationId,
                userId
        );

        int normalizedPage = this.normalizeHistoryPage(page);
        int normalizedSize = this.normalizeHistorySize(size);

        PageResult<Message> pagedMessages = this.messageGateway.findByConversationIdOrderedDesc(
                conversationId,
                normalizedPage,
                normalizedSize
        );

        List<Message> messagesChunk = new ArrayList<>(pagedMessages.getContent());
        messagesChunk.sort((left, right) -> Integer.compare(left.getSequenceNumber(), right.getSequenceNumber()));

        List<ChatbotHistoryMessageResult> messages = messagesChunk
                .stream()
                .map(this.chatbotMessageService::toHistoryMessageResult)
                .toList();

        return ChatbotConversationHistoryResult.builder()
                .conversationId(conversation.getId())
                .engagementLetterId(conversation.getEngagementLetterId())
                .type(conversation.getType().name())
                .status(conversation.getStatus().name())
                .page(normalizedPage)
                .size(normalizedSize)
                .hasMore(pagedMessages.isHasNext())
                .totalMessages(pagedMessages.getTotalElements())
                .messages(messages)
                .build();
    }

    private List<Conversation> readContextualConversations(
            String userId,
            String engagementLetterId
    ) {
        if (engagementLetterId != null && !engagementLetterId.isBlank()) {
            return this.conversationGateway.findByUserIdAndEngagementLetterIdAndTypeOrderByCreatedAtDesc(
                    userId,
                    engagementLetterId,
                    ConversationType.CONTEXTUAL
            );
        }

        return this.conversationGateway.findByUserIdAndTypeOrderByCreatedAtDesc(
                userId,
                ConversationType.CONTEXTUAL
        );
    }

    private ChatbotConversationSummaryResult toConversationSummaryResult(Conversation conversation) {
        Optional<Message> latestMessage = this.messageGateway.findLatestByConversationId(conversation.getId());

        return ChatbotConversationSummaryResult.builder()
                .conversationId(conversation.getId())
                .type(conversation.getType().name())
                .status(this.statusName(conversation))
                .engagementLetterId(conversation.getEngagementLetterId())
                .createdAt(conversation.getCreatedAt().toString())
                .lastMessageAt(latestMessage.map(message -> message.getTimestamp().toString()).orElse(null))
                .preview(latestMessage.map(Message::getContent).orElse(null))
                .build();
    }

    private String statusName(Conversation conversation) {
        ConversationStatus status = conversation.getStatus();

        return status != null ? status.name() : null;
    }

    private ConversationType normalizeConversationType(String type) {
        if (type == null || type.isBlank()) {
            return ConversationType.GENERAL;
        }

        String normalizedType = type.trim().toUpperCase(Locale.ROOT);

        try {
            return ConversationType.valueOf(normalizedType);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Tipo de conversacion no soportado: " + type);
        }
    }

    private int normalizeHistoryPage(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_HISTORY_PAGE;
        }

        return page;
    }

    private int normalizeHistorySize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_HISTORY_SIZE;
        }

        return Math.min(size, MAX_HISTORY_SIZE);
    }
}

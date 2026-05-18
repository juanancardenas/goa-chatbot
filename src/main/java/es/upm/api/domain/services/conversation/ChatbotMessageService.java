package es.upm.api.domain.services.conversation;

import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.domain.model.Message;
import es.upm.api.domain.model.chatbot.result.ChatbotHistoryMessageResult;
import es.upm.api.domain.ports.out.ConversationGateway;
import es.upm.api.domain.ports.out.MessageGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChatbotMessageService {

    private final MessageGateway messageGateway;
    private final ConversationGateway conversationGateway;
    private static final Logger log = LoggerFactory.getLogger(ChatbotMessageService.class);

    public ChatbotMessageService(
            MessageGateway messageGateway,
            ConversationGateway conversationGateway
    ) {
        this.messageGateway = messageGateway;
        this.conversationGateway = conversationGateway;
    }

    public String saveMessage(
            String conversationId,
            MessageSenderType senderType,
            MessageType messageType,
            String content,
            Integer sequenceNumber,
            String parentMessageId,
            LocalDateTime timestamp
    ) {
        return this.messageGateway.createAndReturnId(
                Message.builder()
                        .id(UUID.randomUUID().toString())
                        .conversationId(conversationId)
                        .senderType(senderType)
                        .messageType(messageType)
                        .content(content)
                        .timestamp(timestamp)
                        .sequenceNumber(sequenceNumber)
                        .parentMessageId(parentMessageId)
                        .build()
        );
    }

    public Integer reserveSequenceNumbers(String conversationId, int quantity) {
        return this.conversationGateway.reserveSequenceNumbers(conversationId, quantity);
    }

    public ChatbotHistoryMessageResult toHistoryMessageResult(Message message) {
        return ChatbotHistoryMessageResult.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderType(message.getSenderType().name())
                .messageType(message.getMessageType().name())
                .content(message.getContent())
                .timestamp(message.getTimestamp().toString())
                .sequenceNumber(message.getSequenceNumber())
                .parentMessageId(message.getParentMessageId())
                .build();
    }

    public List<String> readRecentMessagesForPrompt(String conversationId, int maxMessages) {
        try {
            List<Message> messages = this.messageGateway.findByConversationIdOrdered(conversationId);

            if (messages == null || messages.isEmpty()) {
                return List.of();
            }

            return messages.stream()
                    .skip(Math.max(0, messages.size() - maxMessages))
                    .map(this::toPromptHistoryLine)
                    .toList();

        } catch (RuntimeException exception) {
            log.warn(
                    "Could not read recent messages for AI prompt. conversationId={}, limit={}, reason={}",
                    conversationId,
                    maxMessages,
                    exception.getMessage()
            );

            return List.of();
        }
    }

    private String toPromptHistoryLine(Message message) {
        return "%s: %s".formatted(
                message.getSenderType().name(),
                this.safeText(message.getContent(), "")
        );
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }
}

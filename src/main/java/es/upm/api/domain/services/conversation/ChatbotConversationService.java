package es.upm.api.domain.services.conversation;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.exceptions.ConflictException;
import es.upm.api.domain.exceptions.ForbiddenException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.ports.out.ConversationGateway;
import es.upm.api.domain.ports.out.MessageGateway;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ChatbotConversationService {

    private static final String TYPE_CONTEXTUAL = "CONTEXTUAL";
    private static final String TYPE_GENERAL = "GENERAL";

    private final ConversationGateway conversationGateway;
    private final MessageGateway messageGateway;

    public ChatbotConversationService(
            ConversationGateway conversationGateway,
            MessageGateway messageGateway
    ) {
        this.conversationGateway = conversationGateway;
        this.messageGateway = messageGateway;
    }

    public Conversation createGeneralConversation(
            String userId,
            LocalDateTime createdAt
    ) {
        Conversation conversation = Conversation.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .status(ConversationStatus.ACTIVE)
                .type(TYPE_GENERAL)
                .createdAt(createdAt)
                .build();

        this.conversationGateway.create(conversation);

        return conversation;
    }

    public Conversation findOrCreateContextualConversation(
            String userId,
            String engagementLetterId
    ) {
        return this.conversationGateway
                .findActiveContextualConversation(userId, engagementLetterId, TYPE_CONTEXTUAL)
                .orElseGet(() -> {
                    Conversation conversation = Conversation.builder()
                            .id(UUID.randomUUID().toString())
                            .userId(userId)
                            .engagementLetterId(engagementLetterId)
                            .status(ConversationStatus.ACTIVE)
                            .type(TYPE_CONTEXTUAL)
                            .createdAt(LocalDateTime.now())
                            .build();

                    this.conversationGateway.create(conversation);

                    return conversation;
                });
    }

    public Conversation requireOwnedConversation(
            String conversationId,
            String userId
    ) {
        Conversation conversation = this.conversationGateway.readById(conversationId);

        if (!userId.equals(conversation.getUserId())) {
            throw new ForbiddenException("No tienes permisos sobre esta conversacion");
        }

        return conversation;
    }

    public Conversation requireActiveOwnedConversation(
            String conversationId,
            String userId
    ) {
        Conversation conversation = this.requireOwnedConversation(conversationId, userId);

        if (conversation.getStatus() != ConversationStatus.ACTIVE) {
            throw new ConflictException("La conversacion no esta activa");
        }

        return conversation;
    }

    public void closeConversation(
            String conversationId,
            String userId
    ) {
        Conversation conversation = this.requireOwnedConversation(conversationId, userId);

        if (conversation.getStatus() != ConversationStatus.ACTIVE) {
            return;
        }

        conversation.setStatus(ConversationStatus.CLOSED);
        this.conversationGateway.update(conversation);
    }

    public void reopenConversation(
            String conversationId,
            String userId
    ) {
        Conversation conversation = this.requireOwnedConversation(conversationId, userId);

        if (conversation.getStatus() == ConversationStatus.ACTIVE) {
            return;
        }

        if (conversation.getStatus() == ConversationStatus.ARCHIVED) {
            throw new ConflictException("La conversacion archivada no se puede reabrir");
        }

        conversation.setStatus(ConversationStatus.ACTIVE);
        this.conversationGateway.update(conversation);
    }

    public void deleteConversation(
            String conversationId,
            String userId
    ) {
        this.requireOwnedConversation(conversationId, userId);
        this.messageGateway.deleteByConversationId(conversationId);
        this.conversationGateway.delete(conversationId);
    }
}

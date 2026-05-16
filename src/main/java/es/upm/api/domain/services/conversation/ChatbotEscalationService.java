package es.upm.api.domain.services.conversation;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.Escalation;
import es.upm.api.domain.model.platform.UserSummary;
import es.upm.api.domain.ports.out.ConversationGateway;
import es.upm.api.domain.ports.out.EscalationGateway;
import es.upm.api.domain.ports.out.UserClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChatbotEscalationService {

    private final ChatbotConversationService chatbotConversationService;
    private final ConversationGateway conversationGateway;
    private final EscalationGateway escalationGateway;
    private final UserClient userClient;

    public ChatbotEscalationService(
            ChatbotConversationService chatbotConversationService,
            ConversationGateway conversationGateway,
            EscalationGateway escalationGateway,
            UserClient userClient
    ) {
        this.chatbotConversationService = chatbotConversationService;
        this.conversationGateway = conversationGateway;
        this.escalationGateway = escalationGateway;
        this.userClient = userClient;
    }

    public void escalateConversation(
            String conversationId,
            String userId
    ) {
        Conversation conversation = this.chatbotConversationService.requireActiveOwnedConversation(
                conversationId,
                userId
        );

        Optional<UserSummary> user = this.readUserSafely(conversation.getUserId());

        LocalDateTime now = LocalDateTime.now();

        conversation.setStatus(ConversationStatus.ARCHIVED);
        this.conversationGateway.update(conversation);

        this.escalationGateway.create(
                Escalation.builder()
                        .id(UUID.randomUUID())
                        .conversationId(conversation.getId())
                        .userId(conversation.getUserId())
                        .createdAt(now)
                        .phone(user.map(UserSummary::getMobile).orElse(null))
                        .email(user.map(UserSummary::getEmail).orElse(null))
                        .build()
        );
    }

    private Optional<UserSummary> readUserSafely(String userId) {
        try {
            return Optional.ofNullable(this.userClient.readById(userId));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }
}

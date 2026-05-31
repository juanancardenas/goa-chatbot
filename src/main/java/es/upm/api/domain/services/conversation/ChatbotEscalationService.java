package es.upm.api.domain.services.conversation;

import es.upm.api.domain.exceptions.ConflictException;
import es.upm.api.domain.exceptions.ForbiddenException;
import es.upm.api.domain.exceptions.NotFoundException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.Escalation;
import es.upm.api.domain.model.metrics.ChatbotEscalationMetric;
import es.upm.api.domain.model.platform.UserSummary;
import es.upm.api.domain.ports.out.ChatbotMetricsRecorder;
import es.upm.api.domain.ports.out.EscalationGateway;
import es.upm.api.domain.ports.out.UserClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ChatbotEscalationService {

    private final ChatbotConversationService chatbotConversationService;
    private final EscalationGateway escalationGateway;
    private final ChatbotMetricsRecorder chatbotMetricsRecorder;
    private final UserClient userClient;

    public ChatbotEscalationService(
            ChatbotConversationService chatbotConversationService,
            EscalationGateway escalationGateway,
            ChatbotMetricsRecorder chatbotMetricsRecorder,
            UserClient userClient
    ) {
        this.chatbotConversationService = chatbotConversationService;
        this.escalationGateway = escalationGateway;
        this.chatbotMetricsRecorder = chatbotMetricsRecorder;
        this.userClient = userClient;
    }

    public void escalateConversation(
            String conversationId,
            String userId
    ) {
        boolean success = false;
        String errorType = null;

        try {
            Conversation conversation = this.chatbotConversationService.requireActiveOwnedConversation(
                    conversationId,
                    userId
            );

            Optional<UserSummary> user = this.readUserSafely(conversation.getUserId());

            LocalDateTime now = LocalDateTime.now();

            Escalation escalation = Escalation.builder()
                    .id(UUID.randomUUID())
                    .conversationId(conversation.getId())
                    .userId(conversation.getUserId())
                    .createdAt(now)
                    .phone(user.map(UserSummary::getMobile).orElse(null))
                    .email(user.map(UserSummary::getEmail).orElse(null))
                    .build();

            this.escalationGateway.createAndArchiveConversation(conversation, escalation);

            success = true;

        } catch (NotFoundException exception) {
            errorType = "CONVERSATION_NOT_FOUND";
            throw exception;

        } catch (ForbiddenException exception) {
            errorType = "CONVERSATION_FORBIDDEN";
            throw exception;

        } catch (ConflictException exception) {
            errorType = "CONVERSATION_NOT_ACTIVE";
            throw exception;

        } catch (RuntimeException exception) {
            errorType = "ESCALATION_ERROR";
            throw exception;

        } finally {
            ChatbotEscalationMetric metric = ChatbotEscalationMetric.builder()
                    .conversationId(conversationId)
                    .userId(userId)
                    .success(success)
                    .errorType(errorType)
                    .createdAt(LocalDateTime.now())
                    .build();

            this.recordEscalationMetricSafely(metric);
        }
    }

    private void recordEscalationMetricSafely(ChatbotEscalationMetric metric) {
        try {
            this.chatbotMetricsRecorder.recordEscalation(metric);
        } catch (RuntimeException exception) {
            log.warn(
                    "Escalation metric recording failed. conversationId={}, reason={}",
                    metric.getConversationId(),
                    exception.getClass().getSimpleName()
            );
        }
    }

    private Optional<UserSummary> readUserSafely(String userId) {
        try {
            return Optional.ofNullable(this.userClient.readById(userId));
        } catch (RuntimeException ex) {
            log.warn("Could not load user contact data for escalation. userId={}, error={}: {}",
                    userId,
                    ex.getClass().getSimpleName(),
                    ex.getMessage()
            );
            return Optional.empty();
        }
    }
}

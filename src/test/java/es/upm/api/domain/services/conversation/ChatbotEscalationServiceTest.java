package es.upm.api.domain.services.conversation;

import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.enums.ConversationStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotEscalationServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T10:00:00Z");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 1, 10, 0);

    @Mock
    private ChatbotConversationService chatbotConversationService;

    @Mock
    private EscalationGateway escalationGateway;

    @Mock
    private ChatbotMetricsRecorder chatbotMetricsRecorder;

    @Mock
    private UserClient userClient;

    private ChatbotEscalationService chatbotEscalationService;

    @BeforeEach
    void setUp() {
        this.chatbotEscalationService = new ChatbotEscalationService(
                this.chatbotConversationService,
                this.escalationGateway,
                this.chatbotMetricsRecorder,
                this.userClient,
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)
        );
    }

    @Test
    void escalateConversationShouldArchiveOwnedActiveConversationAndCreateEscalation() {
        Conversation existingConversation = Conversation.builder()
                .id("conversation-escalate")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .build();
        when(this.chatbotConversationService.requireActiveOwnedConversation("conversation-escalate", "customer-1"))
                .thenReturn(existingConversation);
        when(this.userClient.readById("customer-1")).thenReturn(
                UserSummary.builder()
                        .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                        .mobile("+34600111222")
                        .email("customer1@example.com")
                        .firstName("Customer")
                        .familyName("One")
                        .build()
        );

        this.chatbotEscalationService.escalateConversation("conversation-escalate", "customer-1");

        ArgumentCaptor<Escalation> escalationCaptor = ArgumentCaptor.forClass(Escalation.class);
        verify(this.escalationGateway).createAndArchiveConversation(eq(existingConversation), escalationCaptor.capture());
        Escalation escalation = escalationCaptor.getValue();

        assertThat(escalation.getId()).isNotNull();
        assertThat(escalation.getConversationId()).isEqualTo("conversation-escalate");
        assertThat(escalation.getUserId()).isEqualTo("customer-1");
        assertThat(escalation.getCreatedAt()).isEqualTo(FIXED_NOW);
        assertThat(escalation.getPhone()).isEqualTo("+34600111222");
        assertThat(escalation.getEmail()).isEqualTo("customer1@example.com");

        ChatbotEscalationMetric metric = this.captureEscalationMetric();
        assertThat(metric.getConversationId()).isEqualTo("conversation-escalate");
        assertThat(metric.getUserId()).isEqualTo("customer-1");
        assertThat(metric.isSuccess()).isTrue();
        assertThat(metric.getErrorType()).isNull();
        assertThat(metric.getCreatedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void escalateConversationShouldCreateEscalationWithoutContactDataWhenUserLookupFails() {
        Conversation existingConversation = Conversation.builder()
                .id("conversation-escalate")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .build();
        when(this.chatbotConversationService.requireActiveOwnedConversation("conversation-escalate", "customer-1"))
                .thenReturn(existingConversation);
        when(this.userClient.readById("customer-1")).thenThrow(new RuntimeException("user service unavailable"));

        this.chatbotEscalationService.escalateConversation("conversation-escalate", "customer-1");

        ArgumentCaptor<Escalation> escalationCaptor = ArgumentCaptor.forClass(Escalation.class);
        verify(this.escalationGateway).createAndArchiveConversation(eq(existingConversation), escalationCaptor.capture());
        Escalation escalation = escalationCaptor.getValue();

        assertThat(existingConversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(escalation.getPhone()).isNull();
        assertThat(escalation.getEmail()).isNull();

        ChatbotEscalationMetric metric = this.captureEscalationMetric();
        assertThat(metric.isSuccess()).isTrue();
        assertThat(metric.getErrorType()).isNull();
    }

    @Test
    void escalateConversationShouldKeepDomainConversationActiveWhenConsistentPersistenceFails() {
        Conversation existingConversation = Conversation.builder()
                .id("conversation-escalate")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .build();
        when(this.chatbotConversationService.requireActiveOwnedConversation("conversation-escalate", "customer-1"))
                .thenReturn(existingConversation);
        org.mockito.Mockito.doThrow(new RuntimeException("mongo unavailable"))
                .when(this.escalationGateway)
                .createAndArchiveConversation(eq(existingConversation), any(Escalation.class));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> this.chatbotEscalationService.escalateConversation("conversation-escalate", "customer-1")
        );

        assertThat(exception).hasMessageContaining("mongo unavailable");
        assertThat(existingConversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);

        ChatbotEscalationMetric metric = this.captureEscalationMetric();
        assertThat(metric.isSuccess()).isFalse();
        assertThat(metric.getErrorType()).isEqualTo("ESCALATION_ERROR");
    }

    @Test
    void escalateConversationShouldRejectOtherUsersConversation() {
        when(this.chatbotConversationService.requireActiveOwnedConversation("conversation-escalate", "customer-1"))
                .thenThrow(new ForbiddenException("No tienes permisos sobre esta conversacion"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> this.chatbotEscalationService.escalateConversation("conversation-escalate", "customer-1")
        );

        assertThat(exception).hasMessageContaining("No tienes permisos sobre esta conversacion");
        verify(this.escalationGateway, never()).create(any(Escalation.class));
        verify(this.escalationGateway, never()).createAndArchiveConversation(any(Conversation.class), any(Escalation.class));

        ChatbotEscalationMetric metric = this.captureEscalationMetric();
        assertThat(metric.isSuccess()).isFalse();
        assertThat(metric.getErrorType()).isEqualTo("CONVERSATION_FORBIDDEN");
    }

    @Test
    void escalateConversationShouldRejectNonActiveConversation() {
        when(this.chatbotConversationService.requireActiveOwnedConversation("conversation-escalate", "customer-1"))
                .thenThrow(new ConflictException("La conversacion no esta activa"));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> this.chatbotEscalationService.escalateConversation("conversation-escalate", "customer-1")
        );

        assertThat(exception).hasMessageContaining("La conversacion no esta activa");
        verify(this.escalationGateway, never()).create(any(Escalation.class));
        verify(this.escalationGateway, never()).createAndArchiveConversation(any(Conversation.class), any(Escalation.class));

        ChatbotEscalationMetric metric = this.captureEscalationMetric();
        assertThat(metric.isSuccess()).isFalse();
        assertThat(metric.getErrorType()).isEqualTo("CONVERSATION_NOT_ACTIVE");
    }

    @Test
    void escalateConversationShouldRecordNotFoundMetricWhenConversationDoesNotExist() {
        when(this.chatbotConversationService.requireActiveOwnedConversation("conversation-missing", "customer-1"))
                .thenThrow(new NotFoundException("conversationId no corresponde a una conversacion existente"));

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> this.chatbotEscalationService.escalateConversation("conversation-missing", "customer-1")
        );

        assertThat(exception).hasMessageContaining("conversationId no corresponde a una conversacion existente");
        verify(this.escalationGateway, never()).createAndArchiveConversation(any(Conversation.class), any(Escalation.class));

        ChatbotEscalationMetric metric = this.captureEscalationMetric();
        assertThat(metric.getConversationId()).isEqualTo("conversation-missing");
        assertThat(metric.getUserId()).isEqualTo("customer-1");
        assertThat(metric.isSuccess()).isFalse();
        assertThat(metric.getErrorType()).isEqualTo("CONVERSATION_NOT_FOUND");
    }

    @Test
    void escalateConversationShouldKeepOriginalExceptionWhenMetricsRecorderFails() {
        when(this.chatbotConversationService.requireActiveOwnedConversation("conversation-escalate", "customer-1"))
                .thenThrow(new ForbiddenException("No tienes permisos sobre esta conversacion"));
        doThrow(new RuntimeException("metrics unavailable"))
                .when(this.chatbotMetricsRecorder)
                .recordEscalation(any(ChatbotEscalationMetric.class));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> this.chatbotEscalationService.escalateConversation("conversation-escalate", "customer-1")
        );

        assertThat(exception).hasMessageContaining("No tienes permisos sobre esta conversacion");
        verify(this.chatbotMetricsRecorder).recordEscalation(any(ChatbotEscalationMetric.class));
    }

    private ChatbotEscalationMetric captureEscalationMetric() {
        ArgumentCaptor<ChatbotEscalationMetric> metricCaptor = ArgumentCaptor.forClass(ChatbotEscalationMetric.class);
        verify(this.chatbotMetricsRecorder).recordEscalation(metricCaptor.capture());
        return metricCaptor.getValue();
    }
}

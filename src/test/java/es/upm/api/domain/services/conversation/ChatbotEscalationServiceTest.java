package es.upm.api.domain.services.conversation;

import es.upm.api.domain.enums.ConversationType;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.exceptions.ConflictException;
import es.upm.api.domain.exceptions.ForbiddenException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.Escalation;
import es.upm.api.domain.model.platform.UserSummary;
import es.upm.api.domain.ports.out.EscalationGateway;
import es.upm.api.domain.ports.out.UserClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotEscalationServiceTest {

    @Mock
    private ChatbotConversationService chatbotConversationService;

    @Mock
    private EscalationGateway escalationGateway;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private ChatbotEscalationService chatbotEscalationService;

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
        assertThat(escalation.getCreatedAt()).isNotNull();
        assertThat(escalation.getPhone()).isEqualTo("+34600111222");
        assertThat(escalation.getEmail()).isEqualTo("customer1@example.com");
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
    }
}

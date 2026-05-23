package es.upm.api.adapter.out.mongodb.adapter;

import es.upm.api.domain.enums.ConversationType;

import com.mongodb.client.result.UpdateResult;
import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.exceptions.ConflictException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.Escalation;
import es.upm.api.adapter.out.mongodb.repository.EscalationRepository;
import es.upm.api.adapter.out.mongodb.entity.ConversationEntity;
import es.upm.api.adapter.out.mongodb.entity.EscalationEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscalationAdapterTest {

    @Mock
    private EscalationRepository escalationRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private EscalationAdapter escalationAdapter;

    @Test
    void createShouldSaveMappedEntity() {
        Escalation escalation = Escalation.builder()
                .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .conversationId("conversation-3")
                .userId("user-3")
                .createdAt(LocalDateTime.of(2026, 5, 2, 12, 15))
                .phone("+34600999888")
                .email("user3@example.com")
                .build();

        escalationAdapter.create(escalation);

        ArgumentCaptor<EscalationEntity> captor = ArgumentCaptor.forClass(EscalationEntity.class);
        verify(escalationRepository).save(captor.capture());
        EscalationEntity saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        assertThat(saved.getConversationId()).isEqualTo("conversation-3");
        assertThat(saved.getUserId()).isEqualTo("user-3");
        assertThat(saved.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 2, 12, 15));
        assertThat(saved.getPhone()).isEqualTo("+34600999888");
        assertThat(saved.getEmail()).isEqualTo("user3@example.com");
    }

    @Test
    void createAndArchiveConversationShouldUpsertEscalationBeforeArchivingConversation() {
        Conversation conversation = conversation();
        Escalation escalation = escalation();
        when(this.mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(EscalationEntity.class)
        )).thenReturn(EscalationEntity.fromEscalation(escalation));
        when(this.mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationEntity.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));

        this.escalationAdapter.createAndArchiveConversation(conversation, escalation);

        verify(this.mongoTemplate).findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(EscalationEntity.class)
        );
        verify(this.mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(ConversationEntity.class));
    }

    @Test
    void createAndArchiveConversationShouldNotArchiveWhenEscalationTraceCannotBeCreated() {
        Conversation conversation = conversation();
        Escalation escalation = escalation();
        when(this.mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(EscalationEntity.class)
        )).thenThrow(new RuntimeException("duplicate key"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> this.escalationAdapter.createAndArchiveConversation(conversation, escalation)
        );

        assertThat(exception).hasMessageContaining("duplicate key");
        verify(this.mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), eq(ConversationEntity.class));
    }

    @Test
    void createAndArchiveConversationShouldLeaveEscalationTraceWhenArchiveCannotBeApplied() {
        Conversation conversation = conversation();
        Escalation escalation = escalation();
        when(this.mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(EscalationEntity.class)
        )).thenReturn(EscalationEntity.fromEscalation(escalation));
        when(this.mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationEntity.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, null));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> this.escalationAdapter.createAndArchiveConversation(conversation, escalation)
        );

        assertThat(exception).hasMessageContaining("La conversacion no esta activa");
        verify(this.mongoTemplate).findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(EscalationEntity.class)
        );
    }

    private Conversation conversation() {
        return Conversation.builder()
                .id("conversation-3")
                .userId("user-3")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 5, 2, 12, 0))
                .build();
    }

    private Escalation escalation() {
        return Escalation.builder()
                .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .conversationId("conversation-3")
                .userId("user-3")
                .createdAt(LocalDateTime.of(2026, 5, 2, 12, 15))
                .phone("+34600999888")
                .email("user3@example.com")
                .build();
    }
}

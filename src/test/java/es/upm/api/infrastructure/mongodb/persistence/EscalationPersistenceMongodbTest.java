package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.model.Escalation;
import es.upm.api.infrastructure.mongodb.daos.EscalationRepository;
import es.upm.api.infrastructure.mongodb.entities.EscalationEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EscalationPersistenceMongodbTest {

    @Mock
    private EscalationRepository escalationRepository;

    @InjectMocks
    private EscalationPersistenceMongodb escalationPersistenceMongodb;

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

        escalationPersistenceMongodb.create(escalation);

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
}

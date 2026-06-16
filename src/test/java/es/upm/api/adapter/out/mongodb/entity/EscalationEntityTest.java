package es.upm.api.adapter.out.mongodb.entity;

import es.upm.api.domain.model.Escalation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EscalationEntityTest {

    @Test
    void constructorShouldCopyEscalationProperties() {
        Escalation escalation = this.escalation();

        EscalationEntity entity = new EscalationEntity(escalation);

        assertThat(entity.getId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(entity.getConversationId()).isEqualTo("conversation-1");
        assertThat(entity.getUserId()).isEqualTo("user-1");
        assertThat(entity.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, Month.MAY, 1, 11, 0));
        assertThat(entity.getPhone()).isEqualTo("+34600111222");
        assertThat(entity.getEmail()).isEqualTo("user1@example.com");
    }

    @Test
    void fromEscalationShouldMapDomainToEntity() {
        Escalation escalation = this.escalation();

        EscalationEntity entity = EscalationEntity.fromEscalation(escalation);

        assertThat(entity.getId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(entity.getConversationId()).isEqualTo("conversation-1");
        assertThat(entity.getUserId()).isEqualTo("user-1");
        assertThat(entity.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, Month.MAY, 1, 11, 0));
        assertThat(entity.getPhone()).isEqualTo("+34600111222");
        assertThat(entity.getEmail()).isEqualTo("user1@example.com");
    }

    @Test
    void toEscalationShouldMapEntityToDomain() {
        EscalationEntity entity = new EscalationEntity(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "conversation-2",
                "user-2",
                LocalDateTime.of(2026, Month.MAY, 2, 9, 30),
                null,
                "user2@example.com"
        );

        Escalation escalation = entity.toEscalation();

        assertThat(escalation.getId()).isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        assertThat(escalation.getConversationId()).isEqualTo("conversation-2");
        assertThat(escalation.getUserId()).isEqualTo("user-2");
        assertThat(escalation.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, Month.MAY, 2, 9, 30));
        assertThat(escalation.getPhone()).isNull();
        assertThat(escalation.getEmail()).isEqualTo("user2@example.com");
    }

    private Escalation escalation() {
        return Escalation.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .conversationId("conversation-1")
                .userId("user-1")
                .createdAt(LocalDateTime.of(2026, Month.MAY, 1, 11, 0))
                .phone("+34600111222")
                .email("user1@example.com")
                .build();
    }
}

package es.upm.api.adapter.out.mongodb.entity;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.model.Conversation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversationEntityTest {

    @Test
    void builderShouldUseActiveStatusByDefault() {
        ConversationEntity entity = ConversationEntity.builder()
                .id("conversation-1")
                .userId("user-1")
                .engagementLetterId("engagement-1")
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, Month.MAY, 3, 10, 0))
                .build();

        assertThat(entity.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
    }

    @Test
    void builderShouldUseZeroLastSequenceNumberByDefault() {
        ConversationEntity entity = ConversationEntity.builder()
                .id("conversation-1")
                .userId("user-1")
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, Month.MAY, 3, 10, 0))
                .build();

        assertThat(entity.getLastSequenceNumber()).isZero();
    }

    @Test
    void constructorShouldCopyConversationProperties() {
        Conversation conversation = this.conversation();

        ConversationEntity entity = new ConversationEntity(conversation);

        assertThat(entity.getId()).isEqualTo("conversation-1");
        assertThat(entity.getUserId()).isEqualTo("user-1");
        assertThat(entity.getEngagementLetterId()).isEqualTo("engagement-1");
        assertThat(entity.getStatus()).isEqualTo(ConversationStatus.CLOSED);
        assertThat(entity.getType()).isEqualTo("GENERAL");
        assertThat(entity.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, Month.MAY, 3, 10, 0));
        assertThat(entity.getLastSequenceNumber()).isEqualTo(4);
    }

    @Test
    void constructorShouldRejectNullConversationType() {
        Conversation conversation = Conversation.builder()
                .id("conversation-null-type")
                .userId("user-1")
                .status(ConversationStatus.ACTIVE)
                .type(null)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 3, 10, 0))
                .lastSequenceNumber(null)
                .build();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ConversationEntity(conversation)
        );

        assertThat(exception.getMessage()).isEqualTo("conversation.type must not be null");
    }

    @Test
    void constructorShouldRejectNullConversation() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ConversationEntity((Conversation) null)
        );

        assertThat(exception.getMessage()).isEqualTo("conversation must not be null");
    }

    @Test
    void constructorShouldRejectNullUserIdFromConversation() {
        Conversation conversation = Conversation.builder()
                .id("conversation-null-user")
                .userId(null)
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 3, 10, 0))
                .lastSequenceNumber(1)
                .build();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ConversationEntity(conversation)
        );

        assertThat(exception.getMessage()).isEqualTo("conversation.userId must not be null");
    }

    @Test
    void constructorShouldRejectNullCreatedAtFromConversation() {
        Conversation conversation = Conversation.builder()
                .id("conversation-null-created-at")
                .userId("user-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(null)
                .lastSequenceNumber(1)
                .build();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ConversationEntity(conversation)
        );

        assertThat(exception.getMessage()).isEqualTo("conversation.createdAt must not be null");
    }

    @Test
    void constructorShouldDefaultNullLastSequenceNumberFromConversationToZero() {
        Conversation conversation = Conversation.builder()
                .id("conversation-null-sequence")
                .userId("user-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 3, 10, 0))
                .lastSequenceNumber(null)
                .build();

        ConversationEntity entity = new ConversationEntity(conversation);

        assertThat(entity.getLastSequenceNumber()).isZero();
    }

    @Test
    void allArgsConstructorShouldDefaultStatusToActiveWhenNull() {
        ConversationEntity entity = new ConversationEntity(
                "conversation-2",
                "user-2",
                "engagement-2",
                null,
                "GENERAL",
                LocalDateTime.of(2026, Month.MAY, 4, 9, 30)
        );

        assertThat(entity.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(entity.getLastSequenceNumber()).isZero();
    }

    @Test
    void allArgsConstructorShouldRejectNullType() {
        LocalDateTime createdAt = LocalDateTime.of(2026, Month.MAY, 4, 9, 30);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ConversationEntity(
                        "conversation-null-type",
                        "user-1",
                        null,
                        ConversationStatus.ACTIVE,
                        null,
                        createdAt
                )
        );

        assertThat(exception.getMessage()).isEqualTo("type must not be null");
    }

    @Test
    void allArgsConstructorShouldRejectNullCreatedAt() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ConversationEntity(
                        "conversation-null-created-at",
                        "user-1",
                        null,
                        ConversationStatus.ACTIVE,
                        "GENERAL",
                        null
                )
        );

        assertThat(exception.getMessage()).isEqualTo("createdAt must not be null");
    }

    @Test
    void fromConversationShouldMapDomainToEntity() {
        Conversation conversation = this.conversation();

        ConversationEntity entity = ConversationEntity.fromConversation(conversation);

        assertThat(entity.getId()).isEqualTo("conversation-1");
        assertThat(entity.getUserId()).isEqualTo("user-1");
        assertThat(entity.getEngagementLetterId()).isEqualTo("engagement-1");
        assertThat(entity.getStatus()).isEqualTo(ConversationStatus.CLOSED);
        assertThat(entity.getType()).isEqualTo("GENERAL");
        assertThat(entity.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, Month.MAY, 3, 10, 0));
        assertThat(entity.getLastSequenceNumber()).isEqualTo(4);
    }

    @Test
    void toConversationShouldMapEntityToDomain() {
        ConversationEntity entity = ConversationEntity.builder()
                .id("conversation-3")
                .userId("user-3")
                .engagementLetterId("engagement-3")
                .status(ConversationStatus.ACTIVE)
                .type("CONTEXTUAL")
                .createdAt(LocalDateTime.of(2026, Month.MAY, 5, 8, 15))
                .lastSequenceNumber(9)
                .build();

        Conversation conversation = entity.toConversation();

        assertThat(conversation.getId()).isEqualTo("conversation-3");
        assertThat(conversation.getUserId()).isEqualTo("user-3");
        assertThat(conversation.getEngagementLetterId()).isEqualTo("engagement-3");
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(conversation.getType()).isEqualTo(ConversationType.CONTEXTUAL);
        assertThat(conversation.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, Month.MAY, 5, 8, 15));
        assertThat(conversation.getLastSequenceNumber()).isEqualTo(9);
    }

    @Test
    void toConversationShouldDefaultNullLastSequenceNumberToZero() {
        ConversationEntity entity = new ConversationEntity();
        entity.setId("conversation-null-sequence");
        entity.setUserId("user-3");
        entity.setStatus(ConversationStatus.ACTIVE);
        entity.setType("GENERAL");
        entity.setCreatedAt(LocalDateTime.of(2026, Month.MAY, 5, 8, 15));
        entity.setLastSequenceNumber(null);

        Conversation conversation = entity.toConversation();

        assertThat(conversation.getLastSequenceNumber()).isZero();
    }

    @Test
    void toConversationShouldRejectNullStatus() {
        ConversationEntity entity = new ConversationEntity();
        entity.setId("conversation-null-status");
        entity.setUserId("user-3");
        entity.setStatus(null);
        entity.setType("GENERAL");
        entity.setCreatedAt(LocalDateTime.of(2026, Month.MAY, 5, 8, 15));

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                entity::toConversation
        );

        assertThat(exception.getMessage()).isEqualTo("status must not be null");
    }

    @Test
    void toConversationShouldRejectNullType() {
        ConversationEntity entity = new ConversationEntity();
        entity.setId("conversation-null-type");
        entity.setUserId("user-3");
        entity.setStatus(ConversationStatus.ACTIVE);
        entity.setType(null);
        entity.setCreatedAt(LocalDateTime.of(2026, Month.MAY, 5, 8, 15));

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                entity::toConversation
        );

        assertThat(exception.getMessage()).isEqualTo("type must not be null");
    }

    @Test
    void toConversationShouldRejectNullCreatedAt() {
        ConversationEntity entity = new ConversationEntity();
        entity.setId("conversation-null-created-at");
        entity.setUserId("user-3");
        entity.setStatus(ConversationStatus.ACTIVE);
        entity.setType("GENERAL");
        entity.setCreatedAt(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                entity::toConversation
        );

        assertThat(exception.getMessage()).isEqualTo("createdAt must not be null");
    }

    @Test
    void toConversationShouldRejectUnknownConversationType() {
        ConversationEntity entity = ConversationEntity.builder()
                .id("conversation-unknown-type")
                .userId("user-3")
                .status(ConversationStatus.ACTIVE)
                .type("UNKNOWN")
                .createdAt(LocalDateTime.of(2026, Month.MAY, 5, 8, 15))
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                entity::toConversation
        );

        assertThat(exception.getMessage()).contains("UNKNOWN");
    }

    @Test
    void allArgsConstructorShouldDefaultLastSequenceNumberToZeroWhenNull() {
        ConversationEntity entity = new ConversationEntity(
                "conversation-4",
                "user-4",
                null,
                ConversationStatus.ACTIVE,
                "GENERAL",
                LocalDateTime.of(2026, Month.MAY, 6, 11, 45),
                null
        );

        assertThat(entity.getLastSequenceNumber()).isZero();
    }

    @Test
    void allArgsConstructorShouldPreserveExplicitLastSequenceNumber() {
        ConversationEntity entity = new ConversationEntity(
                "conversation-sequence",
                "user-sequence",
                "engagement-sequence",
                ConversationStatus.ARCHIVED,
                "CONTEXTUAL",
                LocalDateTime.of(2026, Month.MAY, 6, 12, 0),
                12
        );

        assertThat(entity.getStatus()).isEqualTo(ConversationStatus.ARCHIVED);
        assertThat(entity.getLastSequenceNumber()).isEqualTo(12);
    }

    @Test
    void constructorShouldRejectNullStatusFromConversation() {
        Conversation conversation = Conversation.builder()
                .id("conversation-null-status")
                .userId("user-1")
                .status(null)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 3, 10, 0))
                .lastSequenceNumber(1)
                .build();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ConversationEntity(conversation)
        );

        assertThat(exception.getMessage()).isEqualTo("conversation.status must not be null");
    }

    @Test
    void builderShouldRejectNullType() {
        ConversationEntity.ConversationEntityBuilder entityBuilder = ConversationEntity.builder()
                .id("conversation-5")
                .userId("user-5")
                .status(ConversationStatus.ACTIVE)
                .type(null)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 6, 11, 45))
                .lastSequenceNumber(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                entityBuilder::build
        );

        assertThat(exception.getMessage()).isEqualTo("type must not be null");
    }

    @Test
    void noArgsConstructorAndSettersShouldAllowMappingToDomain() {
        ConversationEntity entity = new ConversationEntity();
        entity.setId("conversation-setters");
        entity.setUserId("user-setters");
        entity.setEngagementLetterId("engagement-setters");
        entity.setStatus(ConversationStatus.CLOSED);
        entity.setType("CONTEXTUAL");
        entity.setCreatedAt(LocalDateTime.of(2026, Month.MAY, 7, 13, 15));
        entity.setLastSequenceNumber(17);

        Conversation conversation = entity.toConversation();

        assertThat(conversation.getId()).isEqualTo("conversation-setters");
        assertThat(conversation.getUserId()).isEqualTo("user-setters");
        assertThat(conversation.getEngagementLetterId()).isEqualTo("engagement-setters");
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.CLOSED);
        assertThat(conversation.getType()).isEqualTo(ConversationType.CONTEXTUAL);
        assertThat(conversation.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, Month.MAY, 7, 13, 15));
        assertThat(conversation.getLastSequenceNumber()).isEqualTo(17);
    }

    private Conversation conversation() {
        return Conversation.builder()
                .id("conversation-1")
                .userId("user-1")
                .engagementLetterId("engagement-1")
                .status(ConversationStatus.CLOSED)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 3, 10, 0))
                .lastSequenceNumber(4)
                .build();
    }
}

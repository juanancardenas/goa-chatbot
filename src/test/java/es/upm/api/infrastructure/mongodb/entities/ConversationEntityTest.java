package es.upm.api.infrastructure.mongodb.entities;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.model.Conversation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationEntityTest {

    @Test
    void builderShouldUseActiveStatusByDefault() {
        ConversationEntity entity = ConversationEntity.builder()
                .id("conversation-1")
                .userId("user-1")
                .engagementLetterId("engagement-1")
                .type("CHATBOT")
                .createdAt(LocalDateTime.of(2026, 5, 3, 10, 0))
                .build();

        assertThat(entity.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
    }

    @Test
    void builderShouldUseZeroLastSequenceNumberByDefault() {
        ConversationEntity entity = ConversationEntity.builder()
                .id("conversation-1")
                .userId("user-1")
                .type("GENERAL")
                .createdAt(LocalDateTime.of(2026, 5, 3, 10, 0))
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
        assertThat(entity.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 3, 10, 0));
        assertThat(entity.getLastSequenceNumber()).isEqualTo(4);
    }

    @Test
    void constructorShouldAllowNullConversationTypeAndDefaultNullSequenceToZero() {
        Conversation conversation = Conversation.builder()
                .id("conversation-null-type")
                .userId("user-1")
                .status(ConversationStatus.ACTIVE)
                .type(null)
                .createdAt(LocalDateTime.of(2026, 5, 3, 10, 0))
                .lastSequenceNumber(null)
                .build();

        ConversationEntity entity = new ConversationEntity(conversation);

        assertThat(entity.getType()).isNull();
        assertThat(entity.getLastSequenceNumber()).isZero();
    }

    @Test
    void allArgsConstructorShouldDefaultStatusToActiveWhenNull() {
        ConversationEntity entity = new ConversationEntity(
                "conversation-2",
                "user-2",
                "engagement-2",
                null,
                "ASSISTED",
                LocalDateTime.of(2026, 5, 4, 9, 30)
        );

        assertThat(entity.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(entity.getLastSequenceNumber()).isZero();
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
        assertThat(entity.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 3, 10, 0));
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
                .createdAt(LocalDateTime.of(2026, 5, 5, 8, 15))
                .lastSequenceNumber(9)
                .build();

        Conversation conversation = entity.toConversation();

        assertThat(conversation.getId()).isEqualTo("conversation-3");
        assertThat(conversation.getUserId()).isEqualTo("user-3");
        assertThat(conversation.getEngagementLetterId()).isEqualTo("engagement-3");
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(conversation.getType()).isEqualTo(ConversationType.CONTEXTUAL);
        assertThat(conversation.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 5, 8, 15));
        assertThat(conversation.getLastSequenceNumber()).isEqualTo(9);
    }

    @Test
    void allArgsConstructorShouldDefaultLastSequenceNumberToZeroWhenNull() {
        ConversationEntity entity = new ConversationEntity(
                "conversation-4",
                "user-4",
                null,
                ConversationStatus.ACTIVE,
                "GENERAL",
                LocalDateTime.of(2026, 5, 6, 11, 45),
                null
        );

        assertThat(entity.getLastSequenceNumber()).isZero();
    }

    @Test
    void toConversationShouldAllowNullTypeAndDefaultNullSequenceToZero() {
        ConversationEntity entity = ConversationEntity.builder()
                .id("conversation-5")
                .userId("user-5")
                .status(ConversationStatus.ACTIVE)
                .type(null)
                .createdAt(LocalDateTime.of(2026, 5, 6, 11, 45))
                .lastSequenceNumber(null)
                .build();

        Conversation conversation = entity.toConversation();

        assertThat(conversation.getType()).isNull();
        assertThat(conversation.getLastSequenceNumber()).isZero();
    }

    private Conversation conversation() {
        return Conversation.builder()
                .id("conversation-1")
                .userId("user-1")
                .engagementLetterId("engagement-1")
                .status(ConversationStatus.CLOSED)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 5, 3, 10, 0))
                .lastSequenceNumber(4)
                .build();
    }
}

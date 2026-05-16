package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.exceptions.NotFoundException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.infrastructure.mongodb.daos.ConversationRepository;
import es.upm.api.infrastructure.mongodb.daos.MessageRepository;
import es.upm.api.infrastructure.mongodb.entities.ConversationEntity;
import es.upm.api.infrastructure.mongodb.entities.MessageEntity;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationPersistenceMongodbTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ConversationPersistenceMongodb conversationPersistenceMongodb;

    @Test
    void readByIdShouldReturnConversation() {
        ConversationEntity entity = conversationEntity("conversation-1", "user-1", "EL-1", "CONTEXTUAL");
        when(conversationRepository.findById("conversation-1")).thenReturn(Optional.of(entity));

        Conversation result = conversationPersistenceMongodb.readById("conversation-1");

        assertThat(result.getId()).isEqualTo("conversation-1");
        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getEngagementLetterId()).isEqualTo("EL-1");
        assertThat(result.getType()).isEqualTo("CONTEXTUAL");
    }

    @Test
    void readByIdShouldThrowWhenConversationDoesNotExist() {
        when(conversationRepository.findById("missing-id")).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> conversationPersistenceMongodb.readById("missing-id")
        );

        assertThat(exception.getMessage()).contains("conversacion existente");
    }

    @Test
    void findMethodsShouldMapEntitiesToDomain() {
        ConversationEntity older = conversationEntity("conversation-1", "user-1", null, "GENERAL");
        ConversationEntity latest = conversationEntity("conversation-2", "user-1", "EL-2", "CONTEXTUAL");

        when(conversationRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(latest, older));
        when(conversationRepository.findByUserIdAndEngagementLetterIdAndType("user-1", "EL-2", "CONTEXTUAL"))
                .thenReturn(Optional.of(latest));
        when(conversationRepository.findByUserIdAndEngagementLetterIdAndTypeAndStatus(
                "user-1", "EL-2", "CONTEXTUAL", ConversationStatus.ACTIVE))
                .thenReturn(Optional.of(latest));
        when(conversationRepository.findByUserIdAndTypeOrderByCreatedAtDesc("user-1", "GENERAL"))
                .thenReturn(List.of(older));
        when(conversationRepository.findByUserIdAndEngagementLetterIdAndTypeOrderByCreatedAtDesc(
                "user-1", "EL-2", "CONTEXTUAL"))
                .thenReturn(List.of(latest));

        List<Conversation> byUser = conversationPersistenceMongodb.findByUserId("user-1");
        Optional<Conversation> contextual = conversationPersistenceMongodb
                .findContextualConversation("user-1", "EL-2", "CONTEXTUAL");
        Optional<Conversation> activeContextual = conversationPersistenceMongodb
                .findActiveContextualConversation("user-1", "EL-2", "CONTEXTUAL");
        List<Conversation> byType = conversationPersistenceMongodb
                .findByUserIdAndTypeOrderByCreatedAtDesc("user-1", "GENERAL");
        List<Conversation> byEngagement = conversationPersistenceMongodb
                .findByUserIdAndEngagementLetterIdAndTypeOrderByCreatedAtDesc("user-1", "EL-2", "CONTEXTUAL");

        assertThat(byUser).hasSize(2);
        assertThat(contextual).isPresent();
        assertThat(activeContextual).isPresent();
        assertThat(byType).hasSize(1);
        assertThat(byEngagement).hasSize(1);
        assertThat(byUser.get(0).getId()).isEqualTo("conversation-2");
    }

    @Test
    void createAndUpdateShouldSaveMappedEntity() {
        Conversation conversation = Conversation.builder()
                .id("conversation-10")
                .userId("user-10")
                .engagementLetterId("EL-10")
                .status(ConversationStatus.ACTIVE)
                .type("CONTEXTUAL")
                .createdAt(LocalDateTime.of(2026, 4, 30, 10, 0))
                .build();

        conversationPersistenceMongodb.create(conversation);
        conversationPersistenceMongodb.update(conversation);

        ArgumentCaptor<ConversationEntity> captor = ArgumentCaptor.forClass(ConversationEntity.class);
        verify(conversationRepository, times(2)).save(captor.capture());
        List<ConversationEntity> saved = captor.getAllValues();

        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getId()).isEqualTo("conversation-10");
        assertThat(saved.get(0).getUserId()).isEqualTo("user-10");
        assertThat(saved.get(0).getType()).isEqualTo("CONTEXTUAL");
    }

    @Test
    void deleteShouldDelegateToRepository() {
        conversationPersistenceMongodb.delete("conversation-11");

        verify(conversationRepository).deleteById("conversation-11");
    }

    @Test
    void reserveSequenceNumbersShouldIncrementConversationCounterAtomically() {
        ConversationEntity updated = conversationEntity("conversation-12", "user-12", null, "GENERAL");
        updated.setLastSequenceNumber(12);
        when(this.messageRepository.findFirstByConversationIdOrderBySequenceNumberDesc("conversation-12"))
                .thenReturn(Optional.empty());
        when(this.mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(ConversationEntity.class)
        )).thenReturn(updated);

        Integer firstReservedSequence = this.conversationPersistenceMongodb.reserveSequenceNumbers("conversation-12", 2);

        assertThat(firstReservedSequence).isEqualTo(11);
        verify(this.mongoTemplate).findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(ConversationEntity.class)
        );
    }

    @Test
    void reserveSequenceNumbersShouldInitializeCounterFromExistingMessagesBeforeIncrement() {
        ConversationEntity updated = conversationEntity("conversation-13", "user-13", null, "GENERAL");
        updated.setLastSequenceNumber(7);
        when(this.messageRepository.findFirstByConversationIdOrderBySequenceNumberDesc("conversation-13"))
                .thenReturn(Optional.of(MessageEntity.builder().sequenceNumber(5).build()));
        when(this.mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(ConversationEntity.class)
        )).thenReturn(updated);

        Integer firstReservedSequence = this.conversationPersistenceMongodb.reserveSequenceNumbers("conversation-13", 2);

        assertThat(firstReservedSequence).isEqualTo(6);
        verify(this.mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(ConversationEntity.class));
    }

    @Test
    void reserveSequenceNumbersShouldThrowWhenConversationDoesNotExist() {
        when(this.messageRepository.findFirstByConversationIdOrderBySequenceNumberDesc("missing-conversation"))
                .thenReturn(Optional.empty());
        when(this.mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(ConversationEntity.class)
        )).thenReturn(null);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> this.conversationPersistenceMongodb.reserveSequenceNumbers("missing-conversation", 2)
        );

        assertThat(exception.getMessage()).contains("conversacion existente");
    }

    private ConversationEntity conversationEntity(String id, String userId, String engagementLetterId, String type) {
        return new ConversationEntity(
                id,
                userId,
                engagementLetterId,
                ConversationStatus.ACTIVE,
                type,
                LocalDateTime.of(2026, 4, 30, 10, 0),
                0
        );
    }
}

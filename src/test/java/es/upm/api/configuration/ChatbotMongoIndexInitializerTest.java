package es.upm.api.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotMongoIndexInitializerTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private IndexOperations messageIndexOperations;

    @Mock
    private IndexOperations escalationIndexOperations;

    private ChatbotMongoIndexInitializer initializer;

    @BeforeEach
    void setUp() {
        this.initializer = new ChatbotMongoIndexInitializer(this.mongoTemplate);
        when(this.mongoTemplate.indexOps("messages")).thenReturn(this.messageIndexOperations);
        when(this.mongoTemplate.indexOps("escalations")).thenReturn(this.escalationIndexOperations);
    }

    @Test
    void runShouldCreateMissingIndexes() {
        when(this.messageIndexOperations.getIndexInfo()).thenReturn(List.of());
        when(this.escalationIndexOperations.getIndexInfo()).thenReturn(List.of());

        this.initializer.run(null);

        verify(this.messageIndexOperations).createIndex(any(IndexDefinition.class));
        verify(this.escalationIndexOperations).createIndex(any(IndexDefinition.class));
    }

    @Test
    void runShouldReuseExistingUniqueIndexesEvenWhenNamesAreDifferent() {
        when(this.messageIndexOperations.getIndexInfo()).thenReturn(List.of(
                indexInfo("legacy_message_index", true, "conversationId", "sequenceNumber")
        ));
        when(this.escalationIndexOperations.getIndexInfo()).thenReturn(List.of(
                indexInfo("conversationId", true, "conversationId")
        ));

        this.initializer.run(null);

        verify(this.messageIndexOperations, never()).dropIndex(any(String.class));
        verify(this.messageIndexOperations, never()).createIndex(any(IndexDefinition.class));
        verify(this.escalationIndexOperations, never()).dropIndex(any(String.class));
        verify(this.escalationIndexOperations, never()).createIndex(any(IndexDefinition.class));
    }

    @Test
    void runShouldDropExistingNonUniqueIndexesForSameFieldsBeforeCreatingUniqueIndexes() {
        when(this.messageIndexOperations.getIndexInfo()).thenReturn(List.of(
                indexInfo("conversation_sequence_idx", false, "conversationId", "sequenceNumber")
        ));
        when(this.escalationIndexOperations.getIndexInfo()).thenReturn(List.of(
                indexInfo("conversationId", false, "conversationId")
        ));

        this.initializer.run(null);

        verify(this.messageIndexOperations).dropIndex("conversation_sequence_idx");
        verify(this.messageIndexOperations).createIndex(any(IndexDefinition.class));
        verify(this.escalationIndexOperations).dropIndex("conversationId");
        verify(this.escalationIndexOperations).createIndex(any(IndexDefinition.class));
    }

    @Test
    void runShouldContinueWhenOneIndexMigrationFails() {
        when(this.messageIndexOperations.getIndexInfo()).thenReturn(List.of());
        when(this.escalationIndexOperations.getIndexInfo()).thenReturn(List.of());
        doThrow(new RuntimeException("duplicated data"))
                .when(this.messageIndexOperations)
                .createIndex(any(IndexDefinition.class));

        this.initializer.run(null);

        verify(this.messageIndexOperations).createIndex(any(IndexDefinition.class));
        verify(this.escalationIndexOperations).createIndex(any(IndexDefinition.class));
    }

    private static IndexInfo indexInfo(String name, boolean unique, String... fields) {
        List<IndexField> indexFields = List.of(fields).stream()
                .map(field -> IndexField.create(field, Sort.Direction.ASC))
                .toList();

        return new IndexInfo(indexFields, name, unique, false, "");
    }
}

package es.upm.api.configuration;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ChatbotMongoIndexInitializer implements ApplicationRunner {

    private static final String MESSAGES_COLLECTION = "messages";
    private static final String ESCALATIONS_COLLECTION = "escalations";
    private static final String CONVERSATION_SEQUENCE_INDEX = "conversation_sequence_idx";
    private static final String ESCALATION_CONVERSATION_INDEX = "escalation_conversation_idx";
    private static final String FIELD_CONVERSATION_ID = "conversationId";
    private static final String FIELD_SEQUENCE_NUMBER = "sequenceNumber";

    private final MongoTemplate mongoTemplate;

    public ChatbotMongoIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        this.runIndexMigration("messages conversation sequence", this::ensureConversationSequenceIndex);
        this.runIndexMigration("escalations conversation", this::ensureEscalationConversationIndex);
    }

    private void ensureConversationSequenceIndex() {
        IndexOperations indexOperations = this.mongoTemplate.indexOps(MESSAGES_COLLECTION);
        if (this.uniqueIndexAlreadyExists(indexOperations, List.of(FIELD_CONVERSATION_ID, FIELD_SEQUENCE_NUMBER))) {
            return;
        }
        this.dropNonUniqueIndexForFields(indexOperations, List.of(FIELD_CONVERSATION_ID, FIELD_SEQUENCE_NUMBER));

        indexOperations.createIndex(new CompoundIndexDefinition(
                new Document(FIELD_CONVERSATION_ID, 1).append(FIELD_SEQUENCE_NUMBER, 1)
        ).named(CONVERSATION_SEQUENCE_INDEX).unique());
    }

    private void ensureEscalationConversationIndex() {
        IndexOperations indexOperations = this.mongoTemplate.indexOps(ESCALATIONS_COLLECTION);
        if (this.uniqueIndexAlreadyExists(indexOperations, List.of(FIELD_CONVERSATION_ID))) {
            return;
        }
        this.dropNonUniqueIndexForFields(indexOperations, List.of(FIELD_CONVERSATION_ID));

        indexOperations.createIndex(new Index()
                .on(FIELD_CONVERSATION_ID, Sort.Direction.ASC)
                .named(ESCALATION_CONVERSATION_INDEX)
                .unique());
    }

    private boolean uniqueIndexAlreadyExists(IndexOperations indexOperations, List<String> fields) {
        return indexOperations.getIndexInfo().stream()
                .filter(indexInfo -> this.hasExactlyFields(indexInfo, fields))
                .anyMatch(indexInfo -> {
                    if (indexInfo.isUnique()) {
                        log.info("MongoDB unique index already exists. index={}, fields={}", indexInfo.getName(), fields);
                        return true;
                    }
                    return false;
                });
    }

    private void dropNonUniqueIndexForFields(IndexOperations indexOperations, List<String> fields) {
        indexOperations.getIndexInfo().stream()
                .filter(indexInfo -> this.hasExactlyFields(indexInfo, fields))
                .filter(indexInfo -> !indexInfo.isUnique())
                .findFirst()
                .ifPresent(indexInfo -> {
                    log.warn(
                            "Dropping non-unique MongoDB index before recreating it as unique. index={}, fields={}",
                            indexInfo.getName(),
                            fields
                    );
                    indexOperations.dropIndex(indexInfo.getName());
                });
    }

    private boolean hasExactlyFields(org.springframework.data.mongodb.core.index.IndexInfo indexInfo, List<String> fields) {
        List<String> indexFields = indexInfo.getIndexFields().stream()
                .map(org.springframework.data.mongodb.core.index.IndexField::getKey)
                .toList();
        return indexFields.equals(fields);
    }

    private void runIndexMigration(String indexDescription, Runnable migration) {
        try {
            migration.run();
        } catch (RuntimeException ex) {
            log.error(
                    "MongoDB index migration failed but application startup will continue. index={}, error={}: {}",
                    indexDescription,
                    ex.getClass().getSimpleName(),
                    ex.getMessage()
            );
        }
    }
}

package es.upm.api.infrastructure.mongodb.persistence;

import com.mongodb.client.result.UpdateResult;
import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.exceptions.ConflictException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.Escalation;
import es.upm.api.domain.ports.out.EscalationGateway;
import es.upm.api.infrastructure.mongodb.daos.EscalationRepository;
import es.upm.api.infrastructure.mongodb.entities.ConversationEntity;
import es.upm.api.infrastructure.mongodb.entities.EscalationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class EscalationPersistenceMongodb implements EscalationGateway {
    private final EscalationRepository escalationRepository;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public EscalationPersistenceMongodb(
            EscalationRepository escalationRepository,
            MongoTemplate mongoTemplate
    ) {
        this.escalationRepository = escalationRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void create(Escalation escalation) {
        EscalationEntity entity = EscalationEntity.fromEscalation(escalation);
        this.escalationRepository.save(entity);
    }

    @Override
    public void createAndArchiveConversation(Conversation conversation, Escalation escalation) {
        this.upsertEscalation(escalation);

        try {
            UpdateResult result = this.mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(conversation.getId())
                            .and("status").is(ConversationStatus.ACTIVE)),
                    new Update().set("status", ConversationStatus.ARCHIVED),
                    ConversationEntity.class
            );

            if (result.getMatchedCount() == 0) {
                log.error(
                        "Escalation trace exists but conversation could not be archived. conversationId={}, userId={}",
                        conversation.getId(),
                        conversation.getUserId()
                );
                throw new ConflictException("La conversacion no esta activa");
            }
        } catch (RuntimeException ex) {
            log.error(
                    "Escalation trace exists but conversation archive failed. conversationId={}, userId={}, error={}: {}",
                    conversation.getId(),
                    conversation.getUserId(),
                    ex.getClass().getSimpleName(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    private void upsertEscalation(Escalation escalation) {
        try {
            this.mongoTemplate.findAndModify(
                    Query.query(Criteria.where("conversationId").is(escalation.getConversationId())),
                    new Update()
                            .setOnInsert("_id", escalation.getId())
                            .setOnInsert("conversationId", escalation.getConversationId())
                            .setOnInsert("userId", escalation.getUserId())
                            .setOnInsert("createdAt", escalation.getCreatedAt())
                            .setOnInsert("phone", escalation.getPhone())
                            .setOnInsert("email", escalation.getEmail()),
                    FindAndModifyOptions.options().upsert(true).returnNew(true),
                    EscalationEntity.class
            );
        } catch (RuntimeException ex) {
            log.error(
                    "Conversation archive skipped because escalation trace could not be created. conversationId={}, userId={}, error={}: {}",
                    escalation.getConversationId(),
                    escalation.getUserId(),
                    ex.getClass().getSimpleName(),
                    ex.getMessage()
            );
            throw ex;
        }
    }
}

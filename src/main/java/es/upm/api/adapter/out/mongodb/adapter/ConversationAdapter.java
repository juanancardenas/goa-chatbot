package es.upm.api.adapter.out.mongodb.adapter;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.exceptions.NotFoundException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.ports.out.ConversationGateway;
import es.upm.api.adapter.out.mongodb.repository.ConversationRepository;
import es.upm.api.adapter.out.mongodb.repository.MessageRepository;
import es.upm.api.adapter.out.mongodb.entity.ConversationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ConversationAdapter implements ConversationGateway {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public ConversationAdapter(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            MongoTemplate mongoTemplate
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Conversation readById(String id) {
        return this.conversationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("conversationId no corresponde a una conversacion existente"))
                .toConversation();
    }

    @Override
    public List<Conversation> findByUserId(String userId) {
        return this.conversationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ConversationEntity::toConversation)
                .toList();
    }

    @Override
    public Optional<Conversation> findContextualConversation(
            String userId,
            String engagementLetterId,
            ConversationType type
    ) {
        return this.conversationRepository
                .findByUserIdAndEngagementLetterIdAndType(userId, engagementLetterId, type.name())
                .map(ConversationEntity::toConversation);
    }

    @Override
    public Optional<Conversation> findActiveContextualConversation(
            String userId,
            String engagementLetterId,
            ConversationType type
    ) {
        return this.conversationRepository
                .findByUserIdAndEngagementLetterIdAndTypeAndStatus(
                        userId,
                        engagementLetterId,
                        type.name(),
                        ConversationStatus.ACTIVE
                )
                .map(ConversationEntity::toConversation);
    }

    @Override
    public List<Conversation> findByUserIdAndTypeOrderByCreatedAtDesc(
            String userId,
            ConversationType type
    ) {
        return this.conversationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type.name())
                .stream()
                .map(ConversationEntity::toConversation)
                .toList();
    }

    @Override
    public List<Conversation> findByUserIdAndEngagementLetterIdAndTypeOrderByCreatedAtDesc(
            String userId,
            String engagementLetterId,
            ConversationType type
    ) {
        return this.conversationRepository
                .findByUserIdAndEngagementLetterIdAndTypeOrderByCreatedAtDesc(
                        userId,
                        engagementLetterId,
                        type.name()
                )
                .stream()
                .map(ConversationEntity::toConversation)
                .toList();
    }

    @Override
    public void create(Conversation conversation) {
        ConversationEntity entity = ConversationEntity.fromConversation(conversation);
        this.conversationRepository.save(entity);
    }

    @Override
    public void update(Conversation conversation) {
        ConversationEntity entity = ConversationEntity.fromConversation(conversation);
        this.conversationRepository.save(entity);
    }

    @Override
    public Integer reserveSequenceNumbers(String conversationId, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("quantity debe ser mayor que cero");
        }

        this.initializeSequenceCounterIfNeeded(conversationId);

        ConversationEntity updatedConversation = this.mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(conversationId)),
                new Update().inc("lastSequenceNumber", quantity),
                FindAndModifyOptions.options().returnNew(true),
                ConversationEntity.class
        );

        if (updatedConversation == null) {
            throw new NotFoundException("conversationId no corresponde a una conversacion existente");
        }

        return updatedConversation.getLastSequenceNumber() - quantity + 1;
    }

    private void initializeSequenceCounterIfNeeded(String conversationId) {
        this.messageRepository.findFirstByConversationIdOrderBySequenceNumberDesc(conversationId)
                .ifPresent(latestMessage -> this.mongoTemplate.updateFirst(
                        Query.query(new Criteria().andOperator(
                                Criteria.where("_id").is(conversationId),
                                new Criteria().orOperator(
                                        Criteria.where("lastSequenceNumber").exists(false),
                                        Criteria.where("lastSequenceNumber").lt(latestMessage.getSequenceNumber())
                                )
                        )),
                        new Update().set("lastSequenceNumber", latestMessage.getSequenceNumber()),
                        ConversationEntity.class
                ));
    }

    @Override
    public void delete(String conversationId) {
        this.conversationRepository.deleteById(conversationId);
    }
}

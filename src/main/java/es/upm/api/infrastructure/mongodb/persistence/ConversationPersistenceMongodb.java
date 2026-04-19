package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.exceptions.NotFoundException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.persistence.ConversationPersistence;
import es.upm.api.infrastructure.mongodb.daos.ConversationRepository;
import es.upm.api.infrastructure.mongodb.entities.ConversationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ConversationPersistenceMongodb implements ConversationPersistence {

    private final ConversationRepository conversationRepository;

    @Autowired
    public ConversationPersistenceMongodb(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @Override
    public Conversation readById(String id) {
        return this.conversationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("conversationId no corresponde a una conversacion existente"))
                .toConversation();
    }

    @Override
    public Optional<Conversation> findContextualConversation(
            String userId,
            String engagementLetterId,
            String type
    ) {
        return this.conversationRepository
                .findByUserIdAndEngagementLetterIdAndType(userId, engagementLetterId, type)
                .map(ConversationEntity::toConversation);
    }

    @Override
    public void create(Conversation conversation) {
        ConversationEntity entity = ConversationEntity.fromConversation(conversation);
        this.conversationRepository.save(entity);
    }
}

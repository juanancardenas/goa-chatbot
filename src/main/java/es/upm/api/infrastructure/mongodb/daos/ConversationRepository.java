package es.upm.api.infrastructure.mongodb.daos;

import es.upm.api.infrastructure.mongodb.entities.ConversationEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ConversationRepository extends MongoRepository<ConversationEntity, String> {

    Optional<ConversationEntity> findByUserIdAndEngagementLetterIdAndType(
            String userId,
            String engagementLetterId,
            String type
    );

}

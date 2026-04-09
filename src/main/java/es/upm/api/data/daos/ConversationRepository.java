package es.upm.api.data.daos;

import es.upm.api.data.entities.ConversationEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ConversationRepository extends MongoRepository<ConversationEntity, String> {

    Optional<ConversationEntity> findByUserIdAndEngagementLetterIdAndType(
            String userId,
            String engagementLetterId,
            String type
    );

}

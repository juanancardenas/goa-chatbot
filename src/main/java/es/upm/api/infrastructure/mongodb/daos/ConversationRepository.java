package es.upm.api.infrastructure.mongodb.daos;

import es.upm.api.infrastructure.mongodb.entities.ConversationEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<ConversationEntity, String> {

    List<ConversationEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<ConversationEntity> findByUserIdAndEngagementLetterIdAndType(
            String userId,
            String engagementLetterId,
            String type
    );

}

package es.upm.api.infrastructure.mongodb.daos;

import es.upm.api.infrastructure.mongodb.entities.MessageEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends MongoRepository<MessageEntity, String> {

    List<MessageEntity> findByConversationIdOrderBySequenceNumberAsc(String conversationId);

    Optional<MessageEntity> findFirstByConversationIdOrderBySequenceNumberDesc(String conversationId);

    List<MessageEntity> findByConversationId(String conversationId);

    List<MessageEntity> findByParentMessageId(String parentMessageId);
}

package es.upm.api.data.daos;

import es.upm.api.data.entities.MessageEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MessageRepository extends MongoRepository<MessageEntity, String> {

    List<MessageEntity> findByConversationIdOrderBySequenceNumberAsc(String conversationId);

    List<MessageEntity> findByConversationId(String conversationId);

    List<MessageEntity> findByParentMessageId(String parentMessageId);
}

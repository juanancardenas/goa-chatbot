package es.upm.api.adapter.out.mongodb.repository;

import es.upm.api.adapter.out.mongodb.entity.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends MongoRepository<MessageEntity, String> {
    List<MessageEntity> findByConversationIdOrderBySequenceNumberAsc(String conversationId);

    Page<MessageEntity> findByConversationIdOrderBySequenceNumberDesc(String conversationId, Pageable pageable);

    Optional<MessageEntity> findFirstByConversationIdOrderBySequenceNumberDesc(String conversationId);

    Optional<MessageEntity> findFirstByConversationIdOrderByTimestampDesc(String conversationId);

    List<MessageEntity> findByConversationId(String conversationId);

    List<MessageEntity> findByParentMessageId(String parentMessageId);

    void deleteByConversationId(String conversationId);
}

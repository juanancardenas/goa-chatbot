package es.upm.api.domain.persistence;

import es.upm.api.domain.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessagePersistence {

    void create(Message message);

    String createAndReturnId(Message message);

    List<Message> findByConversationId(String conversationId);

    Integer nextSequenceNumber(String conversationId);

    Optional<Message> findLatestByConversationId(String conversationId);

    List<Message> findByConversationIdOrdered(String conversationId);

    Page<Message> findByConversationIdOrderedDesc(String conversationId, int page, int size);
}
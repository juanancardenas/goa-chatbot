package es.upm.api.domain.persistence;

import es.upm.api.domain.model.Message;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessagePersistence {

    void create(Message message);

    String createAndReturnId(Message message);

    List<Message> findByConversationId(String conversationId);

    Integer nextSequenceNumber(String conversationId);
}

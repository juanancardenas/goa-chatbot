package es.upm.api.domain.persistence;

import es.upm.api.domain.model.Message;
import org.springframework.stereotype.Repository;

@Repository
public interface MessagePersistence {

    void create(Message message);

    String createAndReturnId(Message message);

    Integer nextSequenceNumber(String conversationId);
}

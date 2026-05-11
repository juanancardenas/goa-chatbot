package es.upm.api.domain.ports.out;

import es.upm.api.domain.model.Message;
import es.upm.api.domain.model.configuration.PageResult;

import java.util.List;
import java.util.Optional;

public interface MessageGateway {

    void create(Message message);

    String createAndReturnId(Message message);

    List<Message> findByConversationId(String conversationId);

    Integer nextSequenceNumber(String conversationId);

    Optional<Message> findLatestByConversationId(String conversationId);

    List<Message> findByConversationIdOrdered(String conversationId);

    PageResult<Message> findByConversationIdOrderedDesc(String conversationId, int page, int size);

    void deleteByConversationId(String conversationId);
}

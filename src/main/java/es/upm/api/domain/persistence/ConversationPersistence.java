package es.upm.api.domain.persistence;

import es.upm.api.domain.model.Conversation;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationPersistence {

    Conversation readById(String conversationId);

    Optional<Conversation> findContextualConversation(String userId, String engagementLetterId, String type);

    void create(Conversation conversation);

    void update(Conversation conversation);

}

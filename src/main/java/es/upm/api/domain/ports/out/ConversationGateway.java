package es.upm.api.domain.ports.out;

import es.upm.api.domain.model.Conversation;

import java.util.List;
import java.util.Optional;

public interface ConversationGateway {

    Conversation readById(String conversationId);

    List<Conversation> findByUserId(String userId);

    Optional<Conversation> findContextualConversation(String userId, String engagementLetterId, String type);

    Optional<Conversation> findActiveContextualConversation(
            String userId,
            String engagementLetterId,
            String type
    );

    List<Conversation> findByUserIdAndTypeOrderByCreatedAtDesc(
            String userId,
            String type
    );

    List<Conversation> findByUserIdAndEngagementLetterIdAndTypeOrderByCreatedAtDesc(
            String userId,
            String engagementLetterId,
            String type
    );

    void create(Conversation conversation);

    void update(Conversation conversation);

    Integer reserveSequenceNumbers(String conversationId, int quantity);

    void delete(String conversationId);

}

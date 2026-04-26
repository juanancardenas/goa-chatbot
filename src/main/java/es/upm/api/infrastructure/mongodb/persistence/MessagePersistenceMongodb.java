package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.model.Message;
import es.upm.api.domain.persistence.MessagePersistence;
import es.upm.api.infrastructure.mongodb.daos.MessageRepository;
import es.upm.api.infrastructure.mongodb.entities.MessageEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MessagePersistenceMongodb implements MessagePersistence {

    private final MessageRepository messageRepository;

    @Autowired
    public MessagePersistenceMongodb(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void create(Message message) {
        MessageEntity entity = MessageEntity.fromMessage(message);
        this.messageRepository.save(entity);
    }

    @Override
    public String createAndReturnId(Message message) {
        this.create(message);
        return message.getId();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return this.messageRepository.findByConversationIdOrderBySequenceNumberAsc(conversationId).stream()
                .map(MessageEntity::toMessage)
                .toList();
    }

    @Override
    public Integer nextSequenceNumber(String conversationId) {
        return this.messageRepository
                .findFirstByConversationIdOrderBySequenceNumberDesc(conversationId)
                .map(message -> message.getSequenceNumber() + 1)
                .orElse(1);
    }
}

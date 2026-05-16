package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.model.Message;
import es.upm.api.domain.model.pagination.PageResult;
import es.upm.api.domain.ports.out.MessageGateway;
import es.upm.api.infrastructure.mongodb.daos.MessageRepository;
import es.upm.api.infrastructure.mongodb.entities.MessageEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MessagePersistenceMongodb implements MessageGateway {
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
    public Optional<Message> findLatestByConversationId(String conversationId) {
        return this.messageRepository.findFirstByConversationIdOrderByTimestampDesc(conversationId)
                .map(MessageEntity::toMessage);
    }

    @Override
    public List<Message> findByConversationIdOrdered(String conversationId) {
        return this.messageRepository.findByConversationIdOrderBySequenceNumberAsc(conversationId)
                .stream()
                .map(MessageEntity::toMessage)
                .toList();
    }

    @Override
    public PageResult<Message> findByConversationIdOrderedDesc(String conversationId, int page, int size) {
        Page<Message> pagedMessages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberDesc(conversationId, PageRequest.of(page, size))
                .map(MessageEntity::toMessage);

        return PageResult.<Message>builder()
                .content(pagedMessages.getContent())
                .page(page)
                .size(size)
                .hasNext(pagedMessages.hasNext())
                .totalElements(pagedMessages.getTotalElements())
                .build();
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        this.messageRepository.deleteByConversationId(conversationId);
    }
}

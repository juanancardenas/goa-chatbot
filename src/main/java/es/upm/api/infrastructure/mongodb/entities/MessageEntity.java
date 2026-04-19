package es.upm.api.infrastructure.mongodb.entities;

import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.Message;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "messages")
public class MessageEntity {
    @Id
    private String id;
    @Indexed
    private String conversationId;
    @NotNull
    private MessageSenderType senderType;
    @NotNull
    private MessageType messageType;
    @NotNull
    private String content;
    @NotNull
    private LocalDateTime timestamp;
    @NotNull
    private Integer sequenceNumber;
    private String parentMessageId;

    public MessageEntity(Message message) {
        BeanUtils.copyProperties(message, this);
    }

    public static MessageEntity fromMessage(Message message) {
        MessageEntity entity = new MessageEntity();
        BeanUtils.copyProperties(message, entity);

        return entity;
    }

    public Message toMessage() {
        Message message = new Message();
        BeanUtils.copyProperties(this, message);

        return message;
    }
}

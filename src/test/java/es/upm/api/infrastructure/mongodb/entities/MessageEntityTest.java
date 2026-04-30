package es.upm.api.infrastructure.mongodb.entities;

import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.domain.model.Message;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MessageEntityTest {

    @Test
    void constructorShouldCopyMessageProperties() {
        Message message = this.message();

        MessageEntity entity = new MessageEntity(message);

        assertThat(entity.getId()).isEqualTo("message-1");
        assertThat(entity.getConversationId()).isEqualTo("conversation-1");
        assertThat(entity.getSenderType()).isEqualTo(MessageSenderType.USER);
        assertThat(entity.getMessageType()).isEqualTo(MessageType.REQUEST);
        assertThat(entity.getContent()).isEqualTo("content");
        assertThat(entity.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 4, 30, 10, 0));
        assertThat(entity.getSequenceNumber()).isEqualTo(7);
        assertThat(entity.getParentMessageId()).isEqualTo("parent-1");
    }

    @Test
    void fromMessageShouldMapMessageToEntity() {
        Message message = this.message();

        MessageEntity entity = MessageEntity.fromMessage(message);

        assertThat(entity.getId()).isEqualTo("message-1");
        assertThat(entity.getConversationId()).isEqualTo("conversation-1");
        assertThat(entity.getSenderType()).isEqualTo(MessageSenderType.USER);
        assertThat(entity.getMessageType()).isEqualTo(MessageType.REQUEST);
        assertThat(entity.getContent()).isEqualTo("content");
        assertThat(entity.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 4, 30, 10, 0));
        assertThat(entity.getSequenceNumber()).isEqualTo(7);
        assertThat(entity.getParentMessageId()).isEqualTo("parent-1");
    }

    @Test
    void toMessageShouldMapEntityToDomain() {
        MessageEntity entity = MessageEntity.builder()
                .id("message-1")
                .conversationId("conversation-1")
                .senderType(MessageSenderType.ASSISTANT)
                .messageType(MessageType.RESPONSE)
                .content("reply")
                .timestamp(LocalDateTime.of(2026, 5, 1, 9, 0))
                .sequenceNumber(8)
                .parentMessageId("message-0")
                .build();

        Message message = entity.toMessage();

        assertThat(message.getId()).isEqualTo("message-1");
        assertThat(message.getConversationId()).isEqualTo("conversation-1");
        assertThat(message.getSenderType()).isEqualTo(MessageSenderType.ASSISTANT);
        assertThat(message.getMessageType()).isEqualTo(MessageType.RESPONSE);
        assertThat(message.getContent()).isEqualTo("reply");
        assertThat(message.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 5, 1, 9, 0));
        assertThat(message.getSequenceNumber()).isEqualTo(8);
        assertThat(message.getParentMessageId()).isEqualTo("message-0");
    }

    private Message message() {
        return Message.builder()
                .id("message-1")
                .conversationId("conversation-1")
                .senderType(MessageSenderType.USER)
                .messageType(MessageType.REQUEST)
                .content("content")
                .timestamp(LocalDateTime.of(2026, 4, 30, 10, 0))
                .sequenceNumber(7)
                .parentMessageId("parent-1")
                .build();
    }
}

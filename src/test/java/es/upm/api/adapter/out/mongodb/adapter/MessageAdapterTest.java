package es.upm.api.adapter.out.mongodb.adapter;

import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.domain.model.Message;
import es.upm.api.adapter.out.mongodb.repository.MessageRepository;
import es.upm.api.adapter.out.mongodb.entity.MessageEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageAdapterTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MessageAdapter messageAdapter;

    @Test
    void createAndCreateAndReturnIdShouldSaveMessage() {
        Message message = message("message-1", "conversation-1", 1);

        messageAdapter.create(message);
        String id = messageAdapter.createAndReturnId(message);

        ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(messageRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues().get(0).getConversationId()).isEqualTo("conversation-1");
        assertThat(id).isEqualTo("message-1");
    }

    @Test
    void findByConversationMethodsShouldReturnMappedMessages() {
        MessageEntity first = messageEntity("message-1", "conversation-1", 1);
        MessageEntity second = messageEntity("message-2", "conversation-1", 2);

        when(messageRepository.findByConversationIdOrderBySequenceNumberAsc("conversation-1"))
                .thenReturn(List.of(first, second));

        List<Message> orderedAsc = messageAdapter.findByConversationId("conversation-1");
        List<Message> orderedAscAlias = messageAdapter.findByConversationIdOrdered("conversation-1");

        assertThat(orderedAsc).hasSize(2);
        assertThat(orderedAscAlias).hasSize(2);
        assertThat(orderedAsc.get(0).getSequenceNumber()).isEqualTo(1);
        assertThat(orderedAscAlias.get(1).getSequenceNumber()).isEqualTo(2);
    }

    @Test
    void findLatestAndFindOrderedDescShouldMapRepositoryResults() {
        MessageEntity latest = messageEntity("message-10", "conversation-1", 10);
        when(messageRepository.findFirstByConversationIdOrderByTimestampDesc("conversation-1"))
                .thenReturn(Optional.of(latest));
        when(messageRepository.findByConversationIdOrderBySequenceNumberDesc(
                eq("conversation-1"),
                any(PageRequest.class)
        )).thenReturn(new PageImpl<>(List.of(latest)));

        Optional<Message> latestMessage = messageAdapter.findLatestByConversationId("conversation-1");
        var page = messageAdapter.findByConversationIdOrderedDesc("conversation-1", 0, 5);

        assertThat(latestMessage).isPresent();
        assertThat(latestMessage.orElseThrow().getId()).isEqualTo("message-10");
        assertThat(page.getPage()).isEqualTo(0);
        assertThat(page.getSize()).isEqualTo(5);
        assertThat(page.isHasNext()).isFalse();
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getSequenceNumber()).isEqualTo(10);
    }

    @Test
    void deleteByConversationIdShouldDelegateToRepository() {
        messageAdapter.deleteByConversationId("conversation-1");

        verify(messageRepository).deleteByConversationId("conversation-1");
    }

    private Message message(String id, String conversationId, int sequence) {
        return Message.builder()
                .id(id)
                .conversationId(conversationId)
                .senderType(MessageSenderType.USER)
                .messageType(MessageType.REQUEST)
                .content("content")
                .timestamp(LocalDateTime.of(2026, Month.APRIL, 30, 10, 0))
                .sequenceNumber(sequence)
                .build();
    }

    private MessageEntity messageEntity(String id, String conversationId, int sequence) {
        return MessageEntity.builder()
                .id(id)
                .conversationId(conversationId)
                .senderType(MessageSenderType.USER)
                .messageType(MessageType.REQUEST)
                .content("content")
                .timestamp(LocalDateTime.of(2026, Month.APRIL, 30, 10, 0))
                .sequenceNumber(sequence)
                .build();
    }
}

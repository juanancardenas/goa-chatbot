package es.upm.api.integrationtests;

import es.upm.api.data.daos.ConversationRepository;
import es.upm.api.data.daos.MessageRepository;
import es.upm.api.data.entities.ConversationEntity;
import es.upm.api.data.entities.ConversationStatus;
import es.upm.api.data.entities.MessageEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class DatabaseSeederDevIT {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    void shouldSeedConversationsAndMessagesOnStartup() {
        List<ConversationEntity> conversations = this.conversationRepository.findAll();
        List<MessageEntity> messages = this.messageRepository.findAll();

        assertThat(conversations).hasSize(3);
        assertThat(messages).hasSize(9);

        assertThat(this.conversationRepository.findById("conversation-dev-001"))
                .isPresent()
                .get()
                .extracting(ConversationEntity::getStatus)
                .isEqualTo(ConversationStatus.ACTIVE);

        assertThat(this.conversationRepository.findById("conversation-dev-002"))
                .isPresent()
                .get()
                .extracting(ConversationEntity::getStatus)
                .isEqualTo(ConversationStatus.CLOSED);

        assertThat(this.conversationRepository.findById("conversation-dev-003"))
                .isPresent()
                .get()
                .extracting(ConversationEntity::getStatus)
                .isEqualTo(ConversationStatus.ARCHIVED);

        assertThat(this.messageRepository.findByConversationIdOrderBySequenceNumberAsc("conversation-dev-001"))
                .hasSize(3)
                .extracting(MessageEntity::getSequenceNumber)
                .containsExactly(1, 2, 3);

        assertThat(this.messageRepository.findByConversationIdOrderBySequenceNumberAsc("conversation-dev-002"))
                .hasSize(3)
                .extracting(MessageEntity::getSequenceNumber)
                .containsExactly(1, 2, 3);

        assertThat(this.messageRepository.findByConversationIdOrderBySequenceNumberAsc("conversation-dev-003"))
                .hasSize(3)
                .extracting(MessageEntity::getSequenceNumber)
                .containsExactly(1, 2, 3);

        assertThat(messages)
                .extracting(MessageEntity::getConversationId)
                .containsOnly("conversation-dev-001", "conversation-dev-002", "conversation-dev-003");
    }
}

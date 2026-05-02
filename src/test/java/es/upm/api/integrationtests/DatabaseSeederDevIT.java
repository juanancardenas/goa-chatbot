package es.upm.api.integrationtests;

import es.upm.api.infrastructure.mongodb.daos.ConversationRepository;
import es.upm.api.infrastructure.mongodb.daos.EscalationRepository;
import es.upm.api.infrastructure.mongodb.daos.MessageRepository;
import es.upm.api.infrastructure.mongodb.entities.ConversationEntity;
import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.infrastructure.mongodb.entities.EscalationEntity;
import es.upm.api.infrastructure.mongodb.entities.MessageEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class DatabaseSeederDevIT {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EscalationRepository escalationRepository;

    @Test
    void shouldSeedConversationsEscalationsAndMessagesOnStartup() {
        List<ConversationEntity> conversations = this.conversationRepository.findAll();
        List<EscalationEntity> escalations = this.escalationRepository.findAll();
        List<MessageEntity> messages = this.messageRepository.findAll();

        assertThat(conversations).hasSize(3);
        assertThat(escalations).hasSize(2);
        assertThat(messages).hasSize(9);

        assertThat(this.conversationRepository.findById("conversation-dev-001"))
                .isPresent()
                .get()
                .extracting(ConversationEntity::getStatus)
                .isEqualTo(ConversationStatus.ACTIVE);

        assertThat(this.conversationRepository.findById("conversation-dev-002"))
                .isPresent()
                .get()
                .satisfies(conversation -> {
                    assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.CLOSED);
                    assertThat(conversation.getType()).isEqualTo("GENERAL");
                    assertThat(conversation.getEngagementLetterId()).isNull();
                });

        assertThat(this.conversationRepository.findById("conversation-dev-003"))
                .isPresent()
                .get()
                .satisfies(conversation -> {
                    assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.ARCHIVED);
                    assertThat(conversation.getType()).isEqualTo("CONTEXTUAL");
                    assertThat(conversation.getEngagementLetterId()).isEqualTo("engagement-dev-003");
                });

        assertThat(this.messageRepository.findByConversationIdOrderBySequenceNumberAsc("conversation-dev-001"))
                .hasSize(3)
                .extracting(MessageEntity::getSequenceNumber)
                .containsExactly(1, 2, 3);

        assertThat(this.messageRepository.findByConversationIdOrderBySequenceNumberAsc("conversation-dev-002"))
                .hasSize(3)
                .extracting(MessageEntity::getSequenceNumber)
                .containsExactly(1, 2, 3);
        assertThat(this.messageRepository.findByConversationIdOrderBySequenceNumberAsc("conversation-dev-002"))
                .extracting(MessageEntity::getContent)
                .contains(
                        "Quiero iniciar una conversacion general para resolver una duda.",
                        "Perfecto, esta conversacion general queda disponible para tus consultas."
                );

        assertThat(this.messageRepository.findByConversationIdOrderBySequenceNumberAsc("conversation-dev-003"))
                .hasSize(3)
                .extracting(MessageEntity::getSequenceNumber)
                .containsExactly(1, 2, 3);
        assertThat(this.messageRepository.findByConversationIdOrderBySequenceNumberAsc("conversation-dev-003"))
                .extracting(MessageEntity::getContent)
                .contains(
                        "Quiero consultar el contexto asociado a mi engagement letter.",
                        "He recuperado la conversacion asociada a tu engagement letter."
                );

        assertThat(this.escalationRepository.findById(UUID.fromString("11111111-1111-1111-1111-111111111111")))
                .isPresent()
                .get()
                .satisfies(escalation -> {
                    assertThat(escalation.getConversationId()).isEqualTo("conversation-dev-001");
                    assertThat(escalation.getUserId()).isEqualTo("customer-dev-001");
                    assertThat(escalation.getPhone()).isEqualTo("+34600111222");
                    assertThat(escalation.getEmail()).isEqualTo("customer1@example.com");
                });

        assertThat(this.escalationRepository.findById(UUID.fromString("22222222-2222-2222-2222-222222222222")))
                .isPresent()
                .get()
                .satisfies(escalation -> {
                    assertThat(escalation.getConversationId()).isEqualTo("conversation-dev-002");
                    assertThat(escalation.getUserId()).isEqualTo("customer-dev-002");
                    assertThat(escalation.getPhone()).isNull();
                    assertThat(escalation.getEmail()).isEqualTo("customer2@example.com");
                });

        assertThat(messages)
                .extracting(MessageEntity::getConversationId)
                .containsOnly("conversation-dev-001", "conversation-dev-002", "conversation-dev-003");
    }
}

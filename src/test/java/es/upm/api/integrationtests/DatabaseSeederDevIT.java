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

        assertThat(conversations).hasSize(6);
        assertThat(escalations).hasSize(3);
        assertThat(messages).hasSize(21);

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

        assertThat(this.conversationRepository.findById("conversation-dev-004"))
                .isPresent()
                .get()
                .satisfies(conversation -> {
                    assertThat(conversation.getUserId()).isEqualTo("6");
                    assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
                    assertThat(conversation.getType()).isEqualTo("CONTEXTUAL");
                    assertThat(conversation.getEngagementLetterId()).isEqualTo("engagement-dev-006-a");
                });

        assertThat(this.conversationRepository.findById("conversation-dev-005"))
                .isPresent()
                .get()
                .satisfies(conversation -> {
                    assertThat(conversation.getUserId()).isEqualTo("6");
                    assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.CLOSED);
                    assertThat(conversation.getType()).isEqualTo("GENERAL");
                    assertThat(conversation.getEngagementLetterId()).isNull();
                });

        assertThat(this.conversationRepository.findById("conversation-dev-006"))
                .isPresent()
                .get()
                .satisfies(conversation -> {
                    assertThat(conversation.getUserId()).isEqualTo("6");
                    assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.ARCHIVED);
                    assertThat(conversation.getType()).isEqualTo("CONTEXTUAL");
                    assertThat(conversation.getEngagementLetterId()).isEqualTo("engagement-dev-006-b");
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

        assertThat(this.messageRepository.findByConversationIdOrderBySequenceNumberAsc("conversation-dev-004"))
                .hasSize(4)
                .extracting(MessageEntity::getSequenceNumber)
                .containsExactly(1, 2, 3, 4);
        assertThat(this.messageRepository.findByConversationIdOrderBySequenceNumberAsc("conversation-dev-004"))
                .extracting(MessageEntity::getContent)
                .contains(
                        "Necesito contexto actualizado de mi encargo activo.",
                        "Resumen preparado con los ultimos hitos asociados al encargo."
                );

        assertThat(this.messageRepository.findByConversationIdOrderBySequenceNumberAsc("conversation-dev-005"))
                .hasSize(4)
                .extracting(MessageEntity::getSequenceNumber)
                .containsExactly(1, 2, 3, 4);
        assertThat(this.messageRepository.findByConversationIdOrderBySequenceNumberAsc("conversation-dev-005"))
                .extracting(MessageEntity::getContent)
                .contains(
                        "Quiero abrir una conversacion general sobre facturacion.",
                        "La conversacion queda cerrada con la ultima informacion disponible."
                );

        assertThat(this.messageRepository.findByConversationIdOrderBySequenceNumberAsc("conversation-dev-006"))
                .hasSize(4)
                .extracting(MessageEntity::getSequenceNumber)
                .containsExactly(1, 2, 3, 4);
        assertThat(this.messageRepository.findByConversationIdOrderBySequenceNumberAsc("conversation-dev-006"))
                .extracting(MessageEntity::getContent)
                .contains(
                        "Quiero escalar mi caso a una persona del equipo.",
                        "Escalacion registrada y conversacion archivada para seguimiento."
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

        assertThat(this.escalationRepository.findById(UUID.fromString("66666666-6666-6666-6666-666666666666")))
                .isPresent()
                .get()
                .satisfies(escalation -> {
                    assertThat(escalation.getConversationId()).isEqualTo("conversation-dev-006");
                    assertThat(escalation.getUserId()).isEqualTo("6");
                    assertThat(escalation.getPhone()).isEqualTo("+34600600606");
                    assertThat(escalation.getEmail()).isEqualTo("user6@example.com");
                });

        assertThat(conversations)
                .filteredOn(conversation -> "6".equals(conversation.getUserId()))
                .hasSize(3);

        assertThat(messages)
                .extracting(MessageEntity::getConversationId)
                .containsOnly(
                        "conversation-dev-001",
                        "conversation-dev-002",
                        "conversation-dev-003",
                        "conversation-dev-004",
                        "conversation-dev-005",
                        "conversation-dev-006"
                );
    }
}

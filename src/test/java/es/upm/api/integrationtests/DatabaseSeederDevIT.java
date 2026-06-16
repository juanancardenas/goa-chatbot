package es.upm.api.integrationtests;

import es.upm.api.adapter.out.mongodb.repository.ConversationRepository;
import es.upm.api.adapter.out.mongodb.repository.EscalationRepository;
import es.upm.api.adapter.out.mongodb.repository.MessageRepository;
import es.upm.api.adapter.out.mongodb.entity.ConversationEntity;
import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.adapter.out.mongodb.entity.EscalationEntity;
import es.upm.api.adapter.out.mongodb.entity.MessageEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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

        assertThat(conversations).hasSize(9);
        assertThat(escalations).hasSize(3);
        assertThat(messages).hasSize(27);

        assertThat(conversations)
                .extracting(
                        ConversationEntity::getId,
                        ConversationEntity::getUserId,
                        ConversationEntity::getStatus,
                        ConversationEntity::getType,
                        ConversationEntity::getEngagementLetterId
                )
                .contains(
                        tuple("conversation-dev-001", "customer-dev-001", ConversationStatus.CLOSED, "GENERAL", null),
                        tuple("conversation-dev-002", "customer-dev-002", ConversationStatus.CLOSED, "GENERAL", null),
                        tuple("conversation-dev-003", "customer-dev-003", ConversationStatus.ARCHIVED, "GENERAL", null),
                        tuple("conversation-dev-004", "6", ConversationStatus.CLOSED, "GENERAL", null),
                        tuple("conversation-dev-005", "6", ConversationStatus.CLOSED, "GENERAL", null),
                        tuple("conversation-dev-006", "6", ConversationStatus.ARCHIVED, "GENERAL", null),
                        tuple("conversation-dev-007", "6", ConversationStatus.CLOSED, "GENERAL", null),
                        tuple("conversation-dev-008", "6", ConversationStatus.CLOSED, "GENERAL", null),
                        tuple(
                                "conversation-dev-009",
                                "6",
                                ConversationStatus.ACTIVE,
                                "CONTEXTUAL",
                                "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001"
                        )
                );

        assertThat(messages)
                .extracting(MessageEntity::getConversationId, MessageEntity::getSequenceNumber)
                .contains(
                        tuple("conversation-dev-001", 1),
                        tuple("conversation-dev-001", 2),
                        tuple("conversation-dev-001", 3),
                        tuple("conversation-dev-002", 1),
                        tuple("conversation-dev-002", 2),
                        tuple("conversation-dev-002", 3),
                        tuple("conversation-dev-003", 1),
                        tuple("conversation-dev-003", 2),
                        tuple("conversation-dev-003", 3),
                        tuple("conversation-dev-004", 1),
                        tuple("conversation-dev-004", 2),
                        tuple("conversation-dev-004", 3),
                        tuple("conversation-dev-004", 4),
                        tuple("conversation-dev-005", 1),
                        tuple("conversation-dev-005", 2),
                        tuple("conversation-dev-005", 3),
                        tuple("conversation-dev-005", 4),
                        tuple("conversation-dev-006", 1),
                        tuple("conversation-dev-006", 2),
                        tuple("conversation-dev-006", 3),
                        tuple("conversation-dev-006", 4),
                        tuple("conversation-dev-007", 1),
                        tuple("conversation-dev-007", 2),
                        tuple("conversation-dev-008", 1),
                        tuple("conversation-dev-008", 2),
                        tuple("conversation-dev-009", 1),
                        tuple("conversation-dev-009", 2)
                );

        assertThat(messages)
                .extracting(MessageEntity::getContent)
                .contains(
                        "Quiero iniciar una conversacion general para resolver una duda.",
                        "Perfecto, esta conversacion general queda disponible para tus consultas.",
                        "Quiero consultar el contexto asociado a mi engagement letter.",
                        "He recuperado la conversacion asociada a tu engagement letter.",
                        "Necesito contexto actualizado de mi encargo activo.",
                        "Resumen preparado con los ultimos hitos asociados al encargo.",
                        "Quiero abrir una conversacion general sobre facturacion.",
                        "La conversacion queda cerrada con la ultima informacion disponible.",
                        "Quiero escalar mi caso a una persona del equipo.",
                        "Escalacion registrada y conversacion archivada para seguimiento.",
                        "Necesito dejar cerrada una consulta general sobre plazos.",
                        "La consulta general sobre plazos queda registrada como cerrada.",
                        "Quiero cerrar una duda general sobre documentacion pendiente.",
                        "La duda general sobre documentacion queda cerrada en el historial.",
                        "Necesito revisar el estado del engagement aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001.",
                        "Conversacion contextual preparada para el engagement aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001."
                );

        assertThat(escalations)
                .extracting(
                        escalation -> escalation.getId().toString(),
                        EscalationEntity::getConversationId,
                        EscalationEntity::getUserId,
                        EscalationEntity::getPhone,
                        EscalationEntity::getEmail
                )
                .contains(
                        tuple(
                                "11111111-1111-1111-1111-111111111111",
                                "conversation-dev-001",
                                "customer-dev-001",
                                "+34600111222",
                                "customer1@example.com"
                        ),
                        tuple(
                                "22222222-2222-2222-2222-222222222222",
                                "conversation-dev-002",
                                "customer-dev-002",
                                null,
                                "customer2@example.com"
                        ),
                        tuple(
                                "66666666-6666-6666-6666-666666666666",
                                "conversation-dev-006",
                                "6",
                                "+34600600606",
                                "user6@example.com"
                        )
                );

        assertThat(conversations)
                .filteredOn(conversation -> "6".equals(conversation.getUserId()))
                .hasSize(6);

        assertThat(messages)
                .extracting(MessageEntity::getConversationId)
                .containsOnly(
                        "conversation-dev-001",
                        "conversation-dev-002",
                        "conversation-dev-003",
                        "conversation-dev-004",
                        "conversation-dev-005",
                        "conversation-dev-006",
                        "conversation-dev-007",
                        "conversation-dev-008",
                        "conversation-dev-009"
                );
    }
}

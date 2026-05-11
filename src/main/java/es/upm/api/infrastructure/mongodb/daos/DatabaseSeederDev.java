package es.upm.api.infrastructure.mongodb.daos;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.infrastructure.mongodb.entities.ConversationEntity;
import es.upm.api.infrastructure.mongodb.entities.EscalationEntity;
import es.upm.api.infrastructure.mongodb.entities.MessageEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Profile({"dev", "test"})
public class DatabaseSeederDev {
    private static final String TYPE_CONTEXTUAL = "CONTEXTUAL";
    private static final String TYPE_GENERAL = "GENERAL";

    private static final Logger log = LogManager.getLogger(DatabaseSeederDev.class);

    private final ConversationRepository conversationRepository;
    private final EscalationRepository escalationRepository;
    private final MessageRepository messageRepository;

    public DatabaseSeederDev(
            ConversationRepository conversationRepository,
            EscalationRepository escalationRepository,
            MessageRepository messageRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.escalationRepository = escalationRepository;
        this.messageRepository = messageRepository;
        this.deleteAllAndInitializeAndSeedDataBase();
    }

    public void deleteAllAndInitializeAndSeedDataBase() {
        this.deleteAllAndInitialize();
        this.seedDataBaseJava();
    }

    private void deleteAllAndInitialize() {
        log.warn("------- Delete All -----------");
        this.messageRepository.deleteAll();
        this.escalationRepository.deleteAll();
        this.conversationRepository.deleteAll();
    }

    private void seedDataBaseJava() {
        log.warn("------- Initial Load from JAVA ---------------------------------------------------------------");

        LocalDateTime baseTime = LocalDateTime.now().minusDays(1);

        ConversationEntity conversation1 = new ConversationEntity(
                "conversation-dev-001",
                "customer-dev-001",
                "engagement-dev-001",
                ConversationStatus.ACTIVE,
                TYPE_CONTEXTUAL,
                baseTime
        );
        ConversationEntity conversation2 = new ConversationEntity(
                "conversation-dev-002",
                "customer-dev-002",
                null,
                ConversationStatus.CLOSED,
                TYPE_GENERAL,
                baseTime.plusMinutes(10)
        );

        ConversationEntity conversation3 = new ConversationEntity(
                "conversation-dev-003",
                "customer-dev-003",
                "engagement-dev-003",
                ConversationStatus.ARCHIVED,
                TYPE_CONTEXTUAL,
                baseTime.plusMinutes(20)
        );

        ConversationEntity conversation4 = new ConversationEntity(
                "conversation-dev-004",
                "6",
                "engagement-dev-006-a",
                ConversationStatus.ACTIVE,
                TYPE_CONTEXTUAL,
                baseTime.plusMinutes(30)
        );
        ConversationEntity conversation5 = new ConversationEntity(
                "conversation-dev-005",
                "6",
                null,
                ConversationStatus.CLOSED,
                TYPE_GENERAL,
                baseTime.plusMinutes(40)
        );
        ConversationEntity conversation6 = new ConversationEntity(
                "conversation-dev-006",
                "6",
                "engagement-dev-006-b",
                ConversationStatus.ARCHIVED,
                TYPE_CONTEXTUAL,
                baseTime.plusMinutes(50)
        );

        this.conversationRepository.saveAll(List.of(
                conversation1,
                conversation2,
                conversation3,
                conversation4,
                conversation5,
                conversation6
        ));

        EscalationEntity escalation1 = new EscalationEntity(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                conversation1.getId(),
                conversation1.getUserId(),
                baseTime.plusMinutes(5),
                "+34600111222",
                "customer1@example.com"
        );
        EscalationEntity escalation2 = new EscalationEntity(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                conversation2.getId(),
                conversation2.getUserId(),
                baseTime.plusMinutes(15),
                null,
                "customer2@example.com"
        );
        EscalationEntity escalation3 = new EscalationEntity(
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                conversation6.getId(),
                conversation6.getUserId(),
                baseTime.plusMinutes(55),
                "+34600600606",
                "user6@example.com"
        );

        this.escalationRepository.saveAll(List.of(escalation1, escalation2, escalation3));

        MessageEntity message1 = MessageEntity.builder()
                .id("message-dev-001")
                .conversationId(conversation1.getId())
                .senderType(MessageSenderType.USER)
                .messageType(MessageType.REQUEST)
                .content("Quiero iniciar una conversacion contextual.")
                .timestamp(baseTime.plusSeconds(10))
                .sequenceNumber(1)
                .build();
        MessageEntity message2 = MessageEntity.builder()
                .id("message-dev-002")
                .conversationId(conversation1.getId())
                .senderType(MessageSenderType.ASSISTANT)
                .messageType(MessageType.RESPONSE)
                .content("Conversacion contextual iniciada.")
                .timestamp(baseTime.plusSeconds(20))
                .sequenceNumber(2)
                .parentMessageId(message1.getId())
                .build();
        MessageEntity message3 = MessageEntity.builder()
                .id("message-dev-003")
                .conversationId(conversation1.getId())
                .senderType(MessageSenderType.SYSTEM)
                .messageType(MessageType.INSTRUCTION)
                .content("Mantener el contexto asociado a la conversacion.")
                .timestamp(baseTime.plusSeconds(30))
                .sequenceNumber(3)
                .parentMessageId(message2.getId())
                .build();

        MessageEntity message4 = MessageEntity.builder()
                .id("message-dev-004")
                .conversationId(conversation2.getId())
                .senderType(MessageSenderType.USER)
                .messageType(MessageType.REQUEST)
                .content("Quiero iniciar una conversacion general para resolver una duda.")
                .timestamp(baseTime.plusMinutes(10).plusSeconds(10))
                .sequenceNumber(1)
                .build();
        MessageEntity message5 = MessageEntity.builder()
                .id("message-dev-005")
                .conversationId(conversation2.getId())
                .senderType(MessageSenderType.ASSISTANT)
                .messageType(MessageType.RESPONSE)
                .content("Perfecto, esta conversacion general queda disponible para tus consultas.")
                .timestamp(baseTime.plusMinutes(10).plusSeconds(20))
                .sequenceNumber(2)
                .parentMessageId(message4.getId())
                .build();
        MessageEntity message6 = MessageEntity.builder()
                .id("message-dev-006")
                .conversationId(conversation2.getId())
                .senderType(MessageSenderType.SYSTEM)
                .messageType(MessageType.INSTRUCTION)
                .content("Conversacion marcada para seguimiento posterior.")
                .timestamp(baseTime.plusMinutes(10).plusSeconds(30))
                .sequenceNumber(3)
                .parentMessageId(message5.getId())
                .build();

        MessageEntity message7 = MessageEntity.builder()
                .id("message-dev-007")
                .conversationId(conversation3.getId())
                .senderType(MessageSenderType.USER)
                .messageType(MessageType.REQUEST)
                .content("Quiero consultar el contexto asociado a mi engagement letter.")
                .timestamp(baseTime.plusMinutes(20).plusSeconds(10))
                .sequenceNumber(1)
                .build();
        MessageEntity message8 = MessageEntity.builder()
                .id("message-dev-008")
                .conversationId(conversation3.getId())
                .senderType(MessageSenderType.ASSISTANT)
                .messageType(MessageType.RESPONSE)
                .content("He recuperado la conversacion asociada a tu engagement letter.")
                .timestamp(baseTime.plusMinutes(20).plusSeconds(20))
                .sequenceNumber(2)
                .parentMessageId(message7.getId())
                .build();
        MessageEntity message9 = MessageEntity.builder()
                .id("message-dev-009")
                .conversationId(conversation3.getId())
                .senderType(MessageSenderType.SYSTEM)
                .messageType(MessageType.INSTRUCTION)
                .content("Conversacion archivada para consulta historica.")
                .timestamp(baseTime.plusMinutes(20).plusSeconds(30))
                .sequenceNumber(3)
                .parentMessageId(message8.getId())
                .build();

        MessageEntity message10 = MessageEntity.builder()
                .id("message-dev-010")
                .conversationId(conversation4.getId())
                .senderType(MessageSenderType.USER)
                .messageType(MessageType.REQUEST)
                .content("Necesito contexto actualizado de mi encargo activo.")
                .timestamp(baseTime.plusMinutes(30).plusSeconds(10))
                .sequenceNumber(1)
                .build();
        MessageEntity message11 = MessageEntity.builder()
                .id("message-dev-011")
                .conversationId(conversation4.getId())
                .senderType(MessageSenderType.ASSISTANT)
                .messageType(MessageType.RESPONSE)
                .content("Puedo ayudarte con el contexto del encargo engagement-dev-006-a.")
                .timestamp(baseTime.plusMinutes(30).plusSeconds(20))
                .sequenceNumber(2)
                .parentMessageId(message10.getId())
                .build();
        MessageEntity message12 = MessageEntity.builder()
                .id("message-dev-012")
                .conversationId(conversation4.getId())
                .senderType(MessageSenderType.USER)
                .messageType(MessageType.REQUEST)
                .content("Dame un resumen de los ultimos hitos disponibles.")
                .timestamp(baseTime.plusMinutes(30).plusSeconds(30))
                .sequenceNumber(3)
                .build();
        MessageEntity message13 = MessageEntity.builder()
                .id("message-dev-013")
                .conversationId(conversation4.getId())
                .senderType(MessageSenderType.ASSISTANT)
                .messageType(MessageType.RESPONSE)
                .content("Resumen preparado con los ultimos hitos asociados al encargo.")
                .timestamp(baseTime.plusMinutes(30).plusSeconds(40))
                .sequenceNumber(4)
                .parentMessageId(message12.getId())
                .build();

        MessageEntity message14 = MessageEntity.builder()
                .id("message-dev-014")
                .conversationId(conversation5.getId())
                .senderType(MessageSenderType.USER)
                .messageType(MessageType.REQUEST)
                .content("Quiero abrir una conversacion general sobre facturacion.")
                .timestamp(baseTime.plusMinutes(40).plusSeconds(10))
                .sequenceNumber(1)
                .build();
        MessageEntity message15 = MessageEntity.builder()
                .id("message-dev-015")
                .conversationId(conversation5.getId())
                .senderType(MessageSenderType.ASSISTANT)
                .messageType(MessageType.RESPONSE)
                .content("Puedo ayudarte con dudas generales de facturacion.")
                .timestamp(baseTime.plusMinutes(40).plusSeconds(20))
                .sequenceNumber(2)
                .parentMessageId(message14.getId())
                .build();
        MessageEntity message16 = MessageEntity.builder()
                .id("message-dev-016")
                .conversationId(conversation5.getId())
                .senderType(MessageSenderType.USER)
                .messageType(MessageType.REQUEST)
                .content("Necesito saber si hay importes pendientes.")
                .timestamp(baseTime.plusMinutes(40).plusSeconds(30))
                .sequenceNumber(3)
                .build();
        MessageEntity message17 = MessageEntity.builder()
                .id("message-dev-017")
                .conversationId(conversation5.getId())
                .senderType(MessageSenderType.ASSISTANT)
                .messageType(MessageType.RESPONSE)
                .content("La conversacion queda cerrada con la ultima informacion disponible.")
                .timestamp(baseTime.plusMinutes(40).plusSeconds(40))
                .sequenceNumber(4)
                .parentMessageId(message16.getId())
                .build();

        MessageEntity message18 = MessageEntity.builder()
                .id("message-dev-018")
                .conversationId(conversation6.getId())
                .senderType(MessageSenderType.USER)
                .messageType(MessageType.REQUEST)
                .content("Quiero escalar mi caso a una persona del equipo.")
                .timestamp(baseTime.plusMinutes(50).plusSeconds(10))
                .sequenceNumber(1)
                .build();
        MessageEntity message19 = MessageEntity.builder()
                .id("message-dev-019")
                .conversationId(conversation6.getId())
                .senderType(MessageSenderType.ASSISTANT)
                .messageType(MessageType.RESPONSE)
                .content("He preparado la escalacion sobre el encargo engagement-dev-006-b.")
                .timestamp(baseTime.plusMinutes(50).plusSeconds(20))
                .sequenceNumber(2)
                .parentMessageId(message18.getId())
                .build();
        MessageEntity message20 = MessageEntity.builder()
                .id("message-dev-020")
                .conversationId(conversation6.getId())
                .senderType(MessageSenderType.USER)
                .messageType(MessageType.REQUEST)
                .content("Confirmo mis datos de contacto para la llamada.")
                .timestamp(baseTime.plusMinutes(50).plusSeconds(30))
                .sequenceNumber(3)
                .build();
        MessageEntity message21 = MessageEntity.builder()
                .id("message-dev-021")
                .conversationId(conversation6.getId())
                .senderType(MessageSenderType.ASSISTANT)
                .messageType(MessageType.RESPONSE)
                .content("Escalacion registrada y conversacion archivada para seguimiento.")
                .timestamp(baseTime.plusMinutes(50).plusSeconds(40))
                .sequenceNumber(4)
                .parentMessageId(message20.getId())
                .build();

        this.messageRepository.saveAll(List.of(
                message1, message2, message3,
                message4, message5, message6,
                message7, message8, message9,
                message10, message11, message12, message13,
                message14, message15, message16, message17,
                message18, message19, message20, message21
        ));

        log.warn("------- Seeded {} conversations, {} escalations and {} messages -----------", 6, 3, 21);
    }

}

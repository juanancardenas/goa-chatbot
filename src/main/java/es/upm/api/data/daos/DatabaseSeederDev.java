package es.upm.api.data.daos;

import es.upm.api.data.entities.ConversationEntity;
import es.upm.api.data.entities.ConversationStatus;
import es.upm.api.data.entities.MessageEntity;
import es.upm.api.data.entities.MessageSenderType;
import es.upm.api.data.entities.MessageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Profile({"dev", "test"})
public class DatabaseSeederDev {
    private static final Logger log = LogManager.getLogger(DatabaseSeederDev.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public DatabaseSeederDev(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository
    ) {
        this.conversationRepository = conversationRepository;
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
                "CONTEXTUAL",
                baseTime
        );
        ConversationEntity conversation2 = new ConversationEntity(
                "conversation-dev-002",
                "customer-dev-002",
                "engagement-dev-002",
                ConversationStatus.CLOSED,
                "CONTEXTUAL",
                baseTime.plusMinutes(10)
        );
        ConversationEntity conversation3 = new ConversationEntity(
                "conversation-dev-003",
                "customer-dev-003",
                null,
                ConversationStatus.ARCHIVED,
                "CONTEXTUAL",
                baseTime.plusMinutes(20)
        );

        this.conversationRepository.saveAll(List.of(conversation1, conversation2, conversation3));

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
                .content("Necesito revisar el estado de mi carta de encargo.")
                .timestamp(baseTime.plusMinutes(10).plusSeconds(10))
                .sequenceNumber(1)
                .build();
        MessageEntity message5 = MessageEntity.builder()
                .id("message-dev-005")
                .conversationId(conversation2.getId())
                .senderType(MessageSenderType.ASSISTANT)
                .messageType(MessageType.RESPONSE)
                .content("La carta de encargo fue revisada correctamente.")
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
                .content("Quiero consultar una conversacion sin engagement letter.")
                .timestamp(baseTime.plusMinutes(20).plusSeconds(10))
                .sequenceNumber(1)
                .build();
        MessageEntity message8 = MessageEntity.builder()
                .id("message-dev-008")
                .conversationId(conversation3.getId())
                .senderType(MessageSenderType.ASSISTANT)
                .messageType(MessageType.RESPONSE)
                .content("He recuperado la conversacion asociada al usuario.")
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

        this.messageRepository.saveAll(List.of(
                message1, message2, message3,
                message4, message5, message6,
                message7, message8, message9
        ));

        log.warn("------- Seeded {} conversations and {} messages -----------", 3, 9);
    }

}

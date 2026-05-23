package es.upm.api.integrationtests;

import es.upm.api.domain.enums.ConversationType;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.domain.ports.out.ConversationGateway;
import es.upm.api.adapter.out.mongodb.repository.ConversationRepository;
import es.upm.api.adapter.out.mongodb.repository.MessageRepository;
import es.upm.api.adapter.out.mongodb.entity.ConversationEntity;
import es.upm.api.adapter.out.mongodb.entity.MessageEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ConversationSequenceConcurrencyIT {

    private static final String CONCURRENCY_PREFIX = "conversation-concurrency-";
    private static final String UNIQUE_SEQUENCE_PREFIX = "conversation-unique-sequence-";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ConversationGateway conversationGateway;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @AfterEach
    void cleanGeneratedData() {
        this.messageRepository.findAll().stream()
                .filter(message -> message.getConversationId() != null)
                .filter(message -> message.getConversationId().startsWith(CONCURRENCY_PREFIX)
                        || message.getConversationId().startsWith(UNIQUE_SEQUENCE_PREFIX))
                .forEach(message -> this.messageRepository.deleteById(message.getId()));
        this.conversationRepository.findAll().stream()
                .filter(conversation -> conversation.getId() != null)
                .filter(conversation -> conversation.getId().startsWith(CONCURRENCY_PREFIX)
                        || conversation.getId().startsWith(UNIQUE_SEQUENCE_PREFIX))
                .forEach(conversation -> this.conversationRepository.deleteById(conversation.getId()));
    }

    @Test
    void reserveSequenceNumbersShouldReturnUniqueValuesUnderConcurrency() throws Exception {
        String conversationId = CONCURRENCY_PREFIX + UUID.randomUUID();
        this.conversationRepository.save(ConversationEntity.builder()
                .id(conversationId)
                .userId("user-concurrency")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL.name())
                .createdAt(LocalDateTime.of(2026, 5, 16, 12, 0))
                .lastSequenceNumber(0)
                .build());
        int concurrentReservations = 20;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(8);

        List<Callable<Integer>> tasks = IntStream.range(0, concurrentReservations)
                .mapToObj(ignored -> (Callable<Integer>) () -> {
                    start.await(5, TimeUnit.SECONDS);
                    return this.conversationGateway.reserveSequenceNumbers(conversationId, 1);
                })
                .toList();
        List<Future<Integer>> futures = tasks.stream()
                .map(executorService::submit)
                .toList();

        start.countDown();
        List<Integer> reservedSequences = futures.stream()
                .map(future -> {
                    try {
                        return future.get(5, TimeUnit.SECONDS);
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                })
                .toList();
        executorService.shutdownNow();

        assertThat(new HashSet<>(reservedSequences)).hasSize(concurrentReservations);
        assertThat(reservedSequences).containsExactlyInAnyOrderElementsOf(
                IntStream.rangeClosed(1, concurrentReservations).boxed().toList()
        );
        assertThat(this.conversationRepository.findById(conversationId))
                .isPresent()
                .get()
                .extracting(ConversationEntity::getLastSequenceNumber)
                .isEqualTo(concurrentReservations);
    }

    @Test
    void messageUniqueIndexShouldRejectDuplicatedConversationSequence() {
        String conversationId = UNIQUE_SEQUENCE_PREFIX + UUID.randomUUID();
        this.messageRepository.save(this.message("message-a-" + UUID.randomUUID(), conversationId, 1));

        assertThrows(
                DuplicateKeyException.class,
                () -> this.messageRepository.save(this.message("message-b-" + UUID.randomUUID(), conversationId, 1))
        );
    }

    private MessageEntity message(String id, String conversationId, int sequenceNumber) {
        return MessageEntity.builder()
                .id(id)
                .conversationId(conversationId)
                .senderType(MessageSenderType.USER)
                .messageType(MessageType.REQUEST)
                .content("content")
                .timestamp(LocalDateTime.of(2026, 5, 16, 12, 30))
                .sequenceNumber(sequenceNumber)
                .build();
    }
}

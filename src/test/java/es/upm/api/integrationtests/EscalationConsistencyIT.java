package es.upm.api.integrationtests;

import es.upm.api.domain.enums.ConversationType;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.exceptions.ConflictException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.Escalation;
import es.upm.api.domain.ports.out.EscalationGateway;
import es.upm.api.adapter.out.mongodb.repository.ConversationRepository;
import es.upm.api.adapter.out.mongodb.repository.EscalationRepository;
import es.upm.api.adapter.out.mongodb.entity.ConversationEntity;
import es.upm.api.adapter.out.mongodb.entity.EscalationEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
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
class EscalationConsistencyIT {

    private static final String ESCALATION_PREFIX = "conversation-escalation-consistency-";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private EscalationGateway escalationGateway;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private EscalationRepository escalationRepository;

    @AfterEach
    void cleanGeneratedData() {
        this.escalationRepository.findAll().stream()
                .filter(escalation -> escalation.getConversationId() != null)
                .filter(escalation -> escalation.getConversationId().startsWith(ESCALATION_PREFIX))
                .forEach(escalation -> this.escalationRepository.deleteById(escalation.getId()));
        this.conversationRepository.findAll().stream()
                .filter(conversation -> conversation.getId() != null)
                .filter(conversation -> conversation.getId().startsWith(ESCALATION_PREFIX))
                .forEach(conversation -> this.conversationRepository.deleteById(conversation.getId()));
    }

    @Test
    void retryOverAlreadyEscalatedConversationShouldKeepSingleTraceAndArchivedConversation() {
        String conversationId = ESCALATION_PREFIX + UUID.randomUUID();
        this.saveConversation(conversationId, ConversationStatus.ARCHIVED);
        this.escalationRepository.save(this.escalationEntity(conversationId));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> this.escalationGateway.createAndArchiveConversation(
                        this.conversation(conversationId),
                        this.escalation(conversationId)
                )
        );

        assertThat(exception).hasMessageContaining("La conversacion no esta activa");
        assertThat(this.escalationsFor(conversationId)).hasSize(1);
        assertThat(this.conversationRepository.findById(conversationId))
                .isPresent()
                .get()
                .extracting(ConversationEntity::getStatus)
                .isEqualTo(ConversationStatus.ARCHIVED);
    }

    @Test
    void simultaneousEscalationRequestsShouldCreateOneTraceAndArchiveConversationOnce() throws Exception {
        String conversationId = ESCALATION_PREFIX + UUID.randomUUID();
        this.saveConversation(conversationId, ConversationStatus.ACTIVE);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        List<Callable<String>> tasks = IntStream.range(0, 2)
                .mapToObj(ignored -> (Callable<String>) () -> {
                    start.await(5, TimeUnit.SECONDS);
                    try {
                        this.escalationGateway.createAndArchiveConversation(
                                this.conversation(conversationId),
                                this.escalation(conversationId)
                        );
                        return "success";
                    } catch (ConflictException ex) {
                        return "conflict";
                    }
                })
                .toList();
        List<Future<String>> futures = tasks.stream()
                .map(executorService::submit)
                .toList();

        start.countDown();
        List<String> results = futures.stream()
                .map(future -> {
                    try {
                        return future.get(5, TimeUnit.SECONDS);
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                })
                .toList();
        executorService.shutdownNow();

        assertThat(results).containsExactlyInAnyOrder("success", "conflict");
        assertThat(this.escalationsFor(conversationId)).hasSize(1);
        assertThat(this.conversationRepository.findById(conversationId))
                .isPresent()
                .get()
                .extracting(ConversationEntity::getStatus)
                .isEqualTo(ConversationStatus.ARCHIVED);
    }

    private void saveConversation(String conversationId, ConversationStatus status) {
        this.conversationRepository.save(ConversationEntity.builder()
                .id(conversationId)
                .userId("user-escalation")
                .status(status)
                .type(ConversationType.GENERAL.name())
                .createdAt(LocalDateTime.of(2026, 5, 16, 17, 0))
                .build());
    }

    private List<EscalationEntity> escalationsFor(String conversationId) {
        return this.escalationRepository.findAll().stream()
                .filter(escalation -> conversationId.equals(escalation.getConversationId()))
                .toList();
    }

    private Conversation conversation(String conversationId) {
        return Conversation.builder()
                .id(conversationId)
                .userId("user-escalation")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 5, 16, 17, 0))
                .build();
    }

    private Escalation escalation(String conversationId) {
        return Escalation.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .userId("user-escalation")
                .createdAt(LocalDateTime.of(2026, 5, 16, 17, 5))
                .phone("+34600111222")
                .email("user@example.com")
                .build();
    }

    private EscalationEntity escalationEntity(String conversationId) {
        return EscalationEntity.fromEscalation(this.escalation(conversationId));
    }
}

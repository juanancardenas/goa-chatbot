package es.upm.api.functionaltests;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.domain.model.platform.EngagementEventPage;
import es.upm.api.domain.model.platform.EngagementEventSummary;
import es.upm.api.domain.model.platform.EngagementLetterSummary;
import es.upm.api.domain.model.platform.LegalProcedureSummary;
import es.upm.api.domain.model.platform.UserSummary;
import es.upm.api.domain.ports.out.ChatbotAiClient;
import es.upm.api.domain.ports.out.EngagementClient;
import es.upm.api.domain.ports.out.UserClient;
import es.upm.api.functionaltests.support.ChatbotTestMessages;
import es.upm.api.adapter.in.rest.dto.ChatbotConfigurationStatusDto;
import es.upm.api.adapter.in.rest.dto.ChatbotContextualConversationRequestDto;
import es.upm.api.adapter.in.rest.dto.ChatbotContextualConversationResponseDto;
import es.upm.api.adapter.in.rest.dto.ChatbotConversationHistoryResponseDto;
import es.upm.api.adapter.in.rest.dto.ChatbotConversationSummaryDto;
import es.upm.api.adapter.in.rest.dto.ChatbotHistoryMessageDto;
import es.upm.api.adapter.in.rest.dto.ChatbotMessageRequestDto;
import es.upm.api.adapter.in.rest.dto.ChatbotMessageResponseDto;
import es.upm.api.adapter.out.mongodb.repository.ConversationRepository;
import es.upm.api.adapter.out.mongodb.repository.EscalationRepository;
import es.upm.api.adapter.out.mongodb.repository.MessageRepository;
import es.upm.api.adapter.out.mongodb.entity.ConversationEntity;
import es.upm.api.adapter.out.mongodb.entity.EscalationEntity;
import es.upm.api.adapter.out.mongodb.entity.MessageEntity;
import es.upm.api.adapter.in.rest.ChatbotResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChatbotResourceFT {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T10:00:00Z");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, Month.JANUARY, 1, 10, 0);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EscalationRepository escalationRepository;

    @LocalServerPort
    private int port;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private EngagementClient engagementClient;

    @MockitoBean
    private ChatbotAiClient chatbotAiClient;

    @MockitoBean
    private UserClient userClient;

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        }
    }

    @BeforeEach
    void setUp() {
        this.escalationRepository.deleteAll();
        this.messageRepository.deleteAll();
        this.conversationRepository.deleteAll();
        when(this.userClient.readById(anyString())).thenAnswer(invocation -> {
            String userId = invocation.getArgument(0, String.class);
            return UserSummary.builder()
                    .id(UUID.nameUUIDFromBytes(userId.getBytes()))
                    .mobile("+34600111222")
                    .email(userId + "@example.com")
                    .firstName("User")
                    .familyName(userId)
                    .build();
        });
    }

    @Test
    void testStartContextualConversationAuthenticated() {
        HttpHeaders headers = this.authHeaders("fake-token-contextual", "customer-1", List.of("customer"));

        ChatbotContextualConversationRequestDto request = new ChatbotContextualConversationRequestDto();
        request.setEngagementLetterId("aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000");

        HttpEntity<ChatbotContextualConversationRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatbotContextualConversationResponseDto> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
                POST,
                entity,
                ChatbotContextualConversationResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .isNotNull()
                .returns("aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000", ChatbotContextualConversationResponseDto::getEngagementLetterId)
                .returns(null, ChatbotContextualConversationResponseDto::getError)
                .satisfies(body -> {
                    assertThat(body.getConversationId()).isNotBlank();
                    assertThat(body.getCreatedAt()).isNotBlank();
                });

        List<ConversationEntity> conversations = this.conversationRepository.findAll();
        assertThat(conversations)
                .singleElement()
                .returns("customer-1", ConversationEntity::getUserId)
                .returns("aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000", ConversationEntity::getEngagementLetterId)
                .returns(ConversationStatus.ACTIVE, ConversationEntity::getStatus)
                .returns(ConversationType.CONTEXTUAL.name(), ConversationEntity::getType);
    }

    @Test
    void testStartContextualConversationUnauthorizedWithoutToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ChatbotContextualConversationRequestDto request = new ChatbotContextualConversationRequestDto();
        request.setEngagementLetterId("aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000");

        HttpEntity<ChatbotContextualConversationRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
                POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testStartContextualConversationAuthenticatedWhenEngagementLetterIdIsBlank() {
        HttpHeaders headers = this.authHeaders("fake-token-blank", "customer-1", List.of("customer"));

        ChatbotContextualConversationRequestDto request = new ChatbotContextualConversationRequestDto();
        request.setEngagementLetterId("");

        HttpEntity<ChatbotContextualConversationRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
                POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isNotNull()
                .contains("\"code\":400")
                .contains("engagementLetterId es obligatorio");
        assertThat(this.conversationRepository.findAll()).isEmpty();
    }

    @Test
    void testStartContextualConversationAuthenticatedWithoutEngagementLetterId() {
        HttpHeaders headers = this.authHeaders("fake-token-null", "customer-1", List.of("customer"));

        ChatbotContextualConversationRequestDto request = new ChatbotContextualConversationRequestDto();

        HttpEntity<ChatbotContextualConversationRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
                POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isNotNull()
                .contains("\"code\":400")
                .contains("engagementLetterId es obligatorio");
        assertThat(this.conversationRepository.findAll()).isEmpty();
    }

    @Test
    void testStartContextualConversationReusesSameConversationForSameUserAndEngagementLetter() {
        HttpHeaders headers = this.authHeaders("fake-token-reuse", "customer-1", List.of("customer"));

        ChatbotContextualConversationRequestDto request = new ChatbotContextualConversationRequestDto();
        request.setEngagementLetterId("aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000");

        HttpEntity<ChatbotContextualConversationRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatbotContextualConversationResponseDto> firstResponse = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
                POST,
                entity,
                ChatbotContextualConversationResponseDto.class
        );

        ResponseEntity<ChatbotContextualConversationResponseDto> secondResponse = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
                POST,
                entity,
                ChatbotContextualConversationResponseDto.class
        );

        assertThat(firstResponse.getStatusCode()).isEqualTo(OK);
        assertThat(secondResponse.getStatusCode()).isEqualTo(OK);
        assertThat(firstResponse.getBody()).isNotNull();
        assertThat(secondResponse.getBody())
                .isNotNull()
                .returns(firstResponse.getBody().getConversationId(), ChatbotContextualConversationResponseDto::getConversationId);

        List<ConversationEntity> conversations = this.conversationRepository.findAll();
        assertThat(conversations).hasSize(1);
    }

    @Test
    void testStartContextualConversationAfterCloseCreatesNewActiveConversation() {
        HttpHeaders headers = this.authHeaders("fake-token-reopen", "customer-1", List.of("customer"));

        ChatbotContextualConversationRequestDto startRequest = new ChatbotContextualConversationRequestDto();
        startRequest.setEngagementLetterId("aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000");
        HttpEntity<ChatbotContextualConversationRequestDto> startEntity = new HttpEntity<>(startRequest, headers);

        ResponseEntity<ChatbotContextualConversationResponseDto> firstStartResponse = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
                POST,
                startEntity,
                ChatbotContextualConversationResponseDto.class
        );

        assertThat(firstStartResponse.getStatusCode()).isEqualTo(OK);
        assertThat(firstStartResponse.getBody()).isNotNull();

        ResponseEntity<Void> closeResponse = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CLOSE_CONVERSATION
                        .replace("{conversationId}", firstStartResponse.getBody().getConversationId()),
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                Void.class
        );

        assertThat(closeResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<ChatbotContextualConversationResponseDto> secondStartResponse = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
                POST,
                startEntity,
                ChatbotContextualConversationResponseDto.class
        );

        assertThat(secondStartResponse.getStatusCode()).isEqualTo(OK);
        assertThat(secondStartResponse.getBody()).isNotNull();
        assertThat(secondStartResponse.getBody().getConversationId())
                .isNotEqualTo(firstStartResponse.getBody().getConversationId());

        ChatbotMessageRequestDto messageRequest = new ChatbotMessageRequestDto(
                secondStartResponse.getBody().getConversationId(),
                "Dame contexto del caso"
        );
        HttpEntity<ChatbotMessageRequestDto> messageEntity = new HttpEntity<>(messageRequest, headers);

        ResponseEntity<ChatbotMessageResponseDto> messageResponse = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.MESSAGES,
                POST,
                messageEntity,
                ChatbotMessageResponseDto.class
        );

        assertThat(messageResponse.getStatusCode()).isEqualTo(OK);
        assertThat(messageResponse.getBody()).isNotNull();
        assertThat(messageResponse.getBody().getConversationId())
                .isEqualTo(secondStartResponse.getBody().getConversationId());

        List<ConversationEntity> conversations = this.conversationRepository.findAll();
        assertThat(conversations)
                .hasSize(2)
                .anySatisfy(conversation -> assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.CLOSED))
                .anySatisfy(conversation -> assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE));
    }

    @Test
    void testReadGeneralConversationHistoryListAuthenticatedReturnsOnlyGeneralConversationsOfUser() {
        String userId = "customer-1";

        ConversationEntity generalClosed = new ConversationEntity(
                "general-closed-001",
                userId,
                null,
                ConversationStatus.CLOSED,
                ConversationType.GENERAL.name(),
                FIXED_NOW.minusHours(5)
        );
        ConversationEntity generalActive = new ConversationEntity(
                "general-active-001",
                userId,
                null,
                ConversationStatus.ACTIVE,
                ConversationType.GENERAL.name(),
                FIXED_NOW.minusHours(1)
        );
        ConversationEntity contextual = new ConversationEntity(
                "contextual-001",
                userId,
                "eng-001",
                ConversationStatus.ACTIVE,
                ConversationType.CONTEXTUAL.name(),
                FIXED_NOW.minusMinutes(30)
        );
        ConversationEntity anotherUserGeneral = new ConversationEntity(
                "general-other-user-001",
                "customer-2",
                null,
                ConversationStatus.ACTIVE,
                ConversationType.GENERAL.name(),
                FIXED_NOW.minusMinutes(10)
        );

        this.conversationRepository.saveAll(List.of(generalClosed, generalActive, contextual, anotherUserGeneral));

        this.messageRepository.saveAll(List.of(
                MessageEntity.builder()
                        .id("msg-general-closed")
                        .conversationId(generalClosed.getId())
                        .senderType(MessageSenderType.USER)
                        .messageType(MessageType.REQUEST)
                        .content("Consulta antigua general")
                        .timestamp(FIXED_NOW.minusHours(4))
                        .sequenceNumber(1)
                        .build(),
                MessageEntity.builder()
                        .id("msg-general-active")
                        .conversationId(generalActive.getId())
                        .senderType(MessageSenderType.USER)
                        .messageType(MessageType.REQUEST)
                        .content("Consulta reciente general")
                        .timestamp(FIXED_NOW.minusMinutes(50))
                        .sequenceNumber(1)
                        .build(),
                MessageEntity.builder()
                        .id("msg-contextual")
                        .conversationId(contextual.getId())
                        .senderType(MessageSenderType.USER)
                        .messageType(MessageType.REQUEST)
                        .content("Consulta contextual")
                        .timestamp(FIXED_NOW.minusMinutes(20))
                        .sequenceNumber(1)
                        .build()
        ));

        HttpHeaders headers = this.authHeaders("fake-token-general-list", userId, List.of("customer"));

        ResponseEntity<ChatbotConversationSummaryDto[]> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONVERSATIONS + "?type=GENERAL",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ChatbotConversationSummaryDto[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .isNotNull()
                .hasSize(2)
                .extracting(
                        ChatbotConversationSummaryDto::getConversationId,
                        ChatbotConversationSummaryDto::getPreview,
                        ChatbotConversationSummaryDto::getType
                )
                .containsExactly(
                        tuple("general-active-001", "Consulta reciente general", ConversationType.GENERAL.name()),
                        tuple("general-closed-001", "Consulta antigua general", ConversationType.GENERAL.name())
                );
    }

    @Test
    void testReadConfigurationStatusAuthenticated() {
        HttpHeaders headers = this.authHeaders("fake-token-config", "customer-1", List.of("customer"));

        ResponseEntity<ChatbotConfigurationStatusDto> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONFIGURATION_STATUS,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ChatbotConfigurationStatusDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .returns(true, ChatbotConfigurationStatusDto::isEnabled)
                .returns("ollama", ChatbotConfigurationStatusDto::getProvider)
                .returns(false, ChatbotConfigurationStatusDto::isDocumentsAvailable)
                .satisfies(body -> {
                    assertThat(body.getModel()).isNotBlank();
                    assertThat(body.getMaxInputCharacters()).isGreaterThan(0);
                    assertThat(body.getMaxOutputTokens()).isGreaterThan(0);
                    assertThat(body.getMaxContextMessages()).isGreaterThanOrEqualTo(0);
                });
    }

    @Test
    void testReadConfigurationStatusDoesNotExposeSensitiveFields() {
        HttpHeaders headers = this.authHeaders("fake-token-config-safe", "customer-1", List.of("customer"));

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONFIGURATION_STATUS,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .contains("provider")
                .contains("model")
                .doesNotContain("apiKey")
                .doesNotContain("basePrompt")
                .doesNotContain("prompt")
                .doesNotContain("CHATBOT_OPENAI_API_KEY")
                .doesNotContain("secret")
                .doesNotContain("stackTrace");
    }

    @Test
    void testReadContextualConversationHistoryListAuthenticatedReturnsOnlyContextualConversationsOfEngagement() {
        String userId = "customer-1";

        ConversationEntity contextualA = new ConversationEntity(
                "contextual-a-001",
                userId,
                "eng-001",
                ConversationStatus.ACTIVE,
                ConversationType.CONTEXTUAL.name(),
                FIXED_NOW.minusHours(3)
        );
        ConversationEntity contextualB = new ConversationEntity(
                "contextual-b-001",
                userId,
                "eng-001",
                ConversationStatus.CLOSED,
                ConversationType.CONTEXTUAL.name(),
                FIXED_NOW.minusHours(1)
        );
        ConversationEntity contextualOtherEngagement = new ConversationEntity(
                "contextual-other-engagement-001",
                userId,
                "eng-002",
                ConversationStatus.ACTIVE,
                ConversationType.CONTEXTUAL.name(),
                FIXED_NOW.minusMinutes(20)
        );
        ConversationEntity general = new ConversationEntity(
                "general-001",
                userId,
                null,
                ConversationStatus.ACTIVE,
                ConversationType.GENERAL.name(),
                FIXED_NOW.minusMinutes(10)
        );

        this.conversationRepository.saveAll(List.of(contextualA, contextualB, contextualOtherEngagement, general));

        this.messageRepository.saveAll(List.of(
                MessageEntity.builder()
                        .id("msg-contextual-a")
                        .conversationId(contextualA.getId())
                        .senderType(MessageSenderType.USER)
                        .messageType(MessageType.REQUEST)
                        .content("Contextual antigua")
                        .timestamp(FIXED_NOW.minusHours(2))
                        .sequenceNumber(1)
                        .build(),
                MessageEntity.builder()
                        .id("msg-contextual-b")
                        .conversationId(contextualB.getId())
                        .senderType(MessageSenderType.USER)
                        .messageType(MessageType.REQUEST)
                        .content("Contextual más reciente")
                        .timestamp(FIXED_NOW.minusMinutes(40))
                        .sequenceNumber(1)
                        .build()
        ));

        HttpHeaders headers = this.authHeaders("fake-token-contextual-list", userId, List.of("customer"));

        ResponseEntity<ChatbotConversationSummaryDto[]> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONVERSATIONS
                        + "?type=CONTEXTUAL&engagementLetterId=eng-001",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ChatbotConversationSummaryDto[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .isNotNull()
                .hasSize(2)
                .extracting(
                        ChatbotConversationSummaryDto::getConversationId,
                        ChatbotConversationSummaryDto::getEngagementLetterId,
                        ChatbotConversationSummaryDto::getType
                )
                .containsExactly(
                        tuple("contextual-b-001", "eng-001", ConversationType.CONTEXTUAL.name()),
                        tuple("contextual-a-001", "eng-001", ConversationType.CONTEXTUAL.name())
                );
    }

    @Test
    void testReadConversationHistoryAuthenticatedReturnsOrderedMessages() {
        String conversationId = "conversation-history-001";
        String userId = "customer-1";

        this.conversationRepository.save(new ConversationEntity(
                conversationId,
                userId,
                "eng-001",
                ConversationStatus.CLOSED,
                ConversationType.CONTEXTUAL.name(),
                FIXED_NOW.minusHours(1)
        ));

        this.messageRepository.saveAll(List.of(
                MessageEntity.builder()
                        .id("history-msg-2")
                        .conversationId(conversationId)
                        .senderType(MessageSenderType.ASSISTANT)
                        .messageType(MessageType.RESPONSE)
                        .content("Respuesta del asistente")
                        .timestamp(FIXED_NOW.minusMinutes(9))
                        .sequenceNumber(2)
                        .parentMessageId("history-msg-1")
                        .build(),
                MessageEntity.builder()
                        .id("history-msg-1")
                        .conversationId(conversationId)
                        .senderType(MessageSenderType.USER)
                        .messageType(MessageType.REQUEST)
                        .content("Consulta inicial")
                        .timestamp(FIXED_NOW.minusMinutes(10))
                        .sequenceNumber(1)
                        .build()
        ));

        HttpHeaders headers = this.authHeaders("fake-token-history", userId, List.of("customer"));

        ResponseEntity<ChatbotConversationHistoryResponseDto> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT
                        + ChatbotResource.CONVERSATION_MESSAGES.replace("{conversationId}", conversationId),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ChatbotConversationHistoryResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .isNotNull()
                .returns(conversationId, ChatbotConversationHistoryResponseDto::getConversationId)
                .returns(ConversationType.CONTEXTUAL.name(), ChatbotConversationHistoryResponseDto::getType)
                .returns(ConversationStatus.CLOSED.name(), ChatbotConversationHistoryResponseDto::getStatus)
                .returns(0, ChatbotConversationHistoryResponseDto::getPage)
                .returns(20, ChatbotConversationHistoryResponseDto::getSize)
                .returns(false, ChatbotConversationHistoryResponseDto::getHasMore)
                .returns(2L, ChatbotConversationHistoryResponseDto::getTotalMessages)
                .satisfies(body -> assertThat(body.getMessages())
                        .hasSize(2)
                        .extracting(
                                ChatbotHistoryMessageDto::getSequenceNumber,
                                ChatbotHistoryMessageDto::getContent
                        )
                        .containsExactly(
                                tuple(1, "Consulta inicial"),
                                tuple(2, "Respuesta del asistente")
                        ));
    }

    @Test
    void testReadContextualConversationHistoryListWithoutEngagementLetterReturnsAllContextualConversations() {
        HttpHeaders headers = this.authHeaders("fake-token-contextual-list-bad-request", "customer-1", List.of("customer"));

        ResponseEntity<ChatbotConversationSummaryDto[]> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONVERSATIONS + "?type=CONTEXTUAL",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ChatbotConversationSummaryDto[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .isNotNull()
                .isEmpty();
    }

    @Test
    void testReadConversationHistoryListUnauthorizedWithoutToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONVERSATIONS + "?type=GENERAL",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testReopenConversationAuthenticatedAsOwnerReturnsNoContent() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-reopen-001",
                "customer-1",
                null,
                ConversationStatus.CLOSED,
                ConversationType.GENERAL.name(),
                FIXED_NOW
        )).getId();

        HttpHeaders headers = this.authHeaders("fake-token-reopen-owner", "customer-1", List.of("customer"));

        ResponseEntity<Void> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT
                        + ChatbotResource.REOPEN_CONVERSATION.replace("{conversationId}", conversationId),
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ConversationEntity updatedConversation = this.conversationRepository.findById(conversationId).orElseThrow();
        assertThat(updatedConversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
    }

    @Test
    void testReopenConversationOfAnotherUserReturnsForbidden() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-reopen-foreign-001",
                "customer-2",
                null,
                ConversationStatus.CLOSED,
                ConversationType.GENERAL.name(),
                FIXED_NOW
        )).getId();

        HttpHeaders headers = this.authHeaders("fake-token-reopen-forbidden", "customer-1", List.of("customer"));

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT
                        + ChatbotResource.REOPEN_CONVERSATION.replace("{conversationId}", conversationId),
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void testReopenConversationUnauthorizedWithoutToken() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-reopen-unauthorized-001",
                "customer-1",
                null,
                ConversationStatus.CLOSED,
                ConversationType.GENERAL.name(),
                FIXED_NOW
        )).getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT
                        + ChatbotResource.REOPEN_CONVERSATION.replace("{conversationId}", conversationId),
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testReopenConversationArchivedReturnsConflict() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-reopen-archived-001",
                "customer-1",
                null,
                ConversationStatus.ARCHIVED,
                ConversationType.GENERAL.name(),
                FIXED_NOW
        )).getId();

        HttpHeaders headers = this.authHeaders("fake-token-reopen-archived", "customer-1", List.of("customer"));

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT
                        + ChatbotResource.REOPEN_CONVERSATION.replace("{conversationId}", conversationId),
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("La conversacion archivada no se puede reabrir");

        ConversationEntity updatedConversation = this.conversationRepository.findById(conversationId).orElseThrow();
        assertThat(updatedConversation.getStatus()).isEqualTo(ConversationStatus.ARCHIVED);
    }

    @Test
    void testStartGeneralConversationAuthenticatedAsCustomer() {
        HttpHeaders headers = this.authHeaders("fake-token-general-customer", "customer-1", List.of("customer"));

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(null, "Hola chatbot");
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatbotMessageResponseDto> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.GENERAL_CONVERSATIONS,
                POST,
                entity,
                ChatbotMessageResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .isNotNull()
                .returns(ChatbotTestMessages.CLIENT_GENERAL_START_REPLY, ChatbotMessageResponseDto::getMessage)
                .returns(null, ChatbotMessageResponseDto::getError)
                .satisfies(body -> {
                    assertThat(body.getConversationId()).isNotBlank();
                    assertThat(body.getCreatedAt()).isNotBlank();
                });

        List<ConversationEntity> conversations = this.conversationRepository.findAll();
        assertThat(conversations)
                .singleElement()
                .returns("customer-1", ConversationEntity::getUserId)
                .returns(null, ConversationEntity::getEngagementLetterId)
                .returns(ConversationStatus.ACTIVE, ConversationEntity::getStatus)
                .returns(ConversationType.GENERAL.name(), ConversationEntity::getType);

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(conversations.getFirst().getId());
        assertThat(messages).hasSize(2);

        MessageEntity firstMessage = messages.get(0);
        assertThat(firstMessage)
                .returns(MessageSenderType.USER, MessageEntity::getSenderType)
                .returns(MessageType.REQUEST, MessageEntity::getMessageType)
                .returns("Hola chatbot", MessageEntity::getContent)
                .returns(1, MessageEntity::getSequenceNumber)
                .returns(null, MessageEntity::getParentMessageId)
                .satisfies(message -> assertThat(message.getTimestamp()).isNotNull());

        MessageEntity secondMessage = messages.get(1);
        assertThat(secondMessage)
                .returns(MessageSenderType.ASSISTANT, MessageEntity::getSenderType)
                .returns(MessageType.RESPONSE, MessageEntity::getMessageType)
                .returns(ChatbotTestMessages.CLIENT_GENERAL_START_REPLY, MessageEntity::getContent)
                .returns(2, MessageEntity::getSequenceNumber)
                .returns(firstMessage.getId(), MessageEntity::getParentMessageId)
                .satisfies(message -> assertThat(message.getTimestamp()).isNotNull());
    }

    @Test
    void testStartGeneralConversationAuthenticatedAsProfessional() {
        HttpHeaders headers = this.authHeaders("fake-token-general-professional", "admin-1", List.of("admin"));

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(null, "Necesito soporte operativo");
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatbotMessageResponseDto> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.GENERAL_CONVERSATIONS,
                POST,
                entity,
                ChatbotMessageResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .isNotNull()
                .returns(ChatbotTestMessages.PROFESSIONAL_GENERAL_START_REPLY, ChatbotMessageResponseDto::getMessage)
                .returns(null, ChatbotMessageResponseDto::getError)
                .satisfies(body -> {
                    assertThat(body.getConversationId()).isNotBlank();
                    assertThat(body.getCreatedAt()).isNotBlank();
                });

        List<ConversationEntity> conversations = this.conversationRepository.findAll();
        assertThat(conversations)
                .singleElement()
                .returns("admin-1", ConversationEntity::getUserId)
                .returns(null, ConversationEntity::getEngagementLetterId)
                .returns(ConversationStatus.ACTIVE, ConversationEntity::getStatus)
                .returns(ConversationType.GENERAL.name(), ConversationEntity::getType);

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(conversations.getFirst().getId());
        assertThat(messages)
                .hasSize(2)
                .extracting(MessageEntity::getContent)
                .containsExactly(
                        "Necesito soporte operativo",
                        ChatbotTestMessages.PROFESSIONAL_GENERAL_START_REPLY
                );
    }

    @Test
    void testStartGeneralConversationUnauthorizedWithoutToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(null, "Hola chatbot");
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.GENERAL_CONVERSATIONS,
                POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testReadUserConversationsUnauthorizedWithoutToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONVERSATIONS,
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testSendMessageAuthenticatedAsCustomer() {
        HttpHeaders headers = this.authHeaders("fake-token-message-customer", "customer-1", List.of("customer"));

        ChatbotMessageRequestDto startRequest = new ChatbotMessageRequestDto(null, "Hola chatbot");
        HttpEntity<ChatbotMessageRequestDto> startEntity = new HttpEntity<>(startRequest, headers);

        ResponseEntity<ChatbotMessageResponseDto> startResponse = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.GENERAL_CONVERSATIONS,
                POST,
                startEntity,
                ChatbotMessageResponseDto.class
        );

        assertThat(startResponse.getStatusCode()).isEqualTo(OK);
        assertThat(startResponse.getBody()).isNotNull();

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(
                startResponse.getBody().getConversationId(),
                "¿Cómo puedo consultar el estado de un encargo?"
        );
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatbotMessageResponseDto> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.MESSAGES,
                POST,
                entity,
                ChatbotMessageResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .isNotNull()
                .returns(startResponse.getBody().getConversationId(), ChatbotMessageResponseDto::getConversationId)
                .returns(ChatbotTestMessages.CLIENT_GENERAL_STATUS_REPLY, ChatbotMessageResponseDto::getMessage)
                .returns(null, ChatbotMessageResponseDto::getError)
                .satisfies(body -> assertThat(body.getCreatedAt()).isNotBlank());

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(startResponse.getBody().getConversationId());
        assertThat(messages)
                .hasSize(4)
                .extracting(
                        MessageEntity::getContent,
                        MessageEntity::getSequenceNumber,
                        MessageEntity::getSenderType
                )
                .containsExactly(
                        tuple(startRequest.getMessage(), 1, MessageSenderType.USER),
                        tuple(ChatbotTestMessages.CLIENT_GENERAL_START_REPLY, 2, MessageSenderType.ASSISTANT),
                        tuple(request.getMessage(), 3, MessageSenderType.USER),
                        tuple(ChatbotTestMessages.CLIENT_GENERAL_STATUS_REPLY, 4, MessageSenderType.ASSISTANT)
                );
        assertThat(messages.get(2).getParentMessageId()).isNull();
        assertThat(messages.get(3).getParentMessageId()).isEqualTo(messages.get(2).getId());
    }

    @Test
    void testSendMessageAuthenticatedAsProfessional() {
        HttpHeaders headers = this.authHeaders("fake-token-message-professional", "manager-1", List.of("manager"));

        ChatbotMessageRequestDto startRequest = new ChatbotMessageRequestDto(null, "Necesito revisar el flujo");
        HttpEntity<ChatbotMessageRequestDto> startEntity = new HttpEntity<>(startRequest, headers);

        ResponseEntity<ChatbotMessageResponseDto> startResponse = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.GENERAL_CONVERSATIONS,
                POST,
                startEntity,
                ChatbotMessageResponseDto.class
        );

        assertThat(startResponse.getStatusCode()).isEqualTo(OK);
        assertThat(startResponse.getBody()).isNotNull();

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(
                startResponse.getBody().getConversationId(),
                "Necesito saber cómo revisar el estado de un encargo"
        );
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatbotMessageResponseDto> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.MESSAGES,
                POST,
                entity,
                ChatbotMessageResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .isNotNull()
                .returns(startResponse.getBody().getConversationId(), ChatbotMessageResponseDto::getConversationId)
                .returns(ChatbotTestMessages.PROFESSIONAL_GENERAL_STATUS_REPLY, ChatbotMessageResponseDto::getMessage)
                .returns(null, ChatbotMessageResponseDto::getError)
                .satisfies(body -> assertThat(body.getCreatedAt()).isNotBlank());

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(startResponse.getBody().getConversationId());
        assertThat(messages)
                .hasSize(4)
                .extracting(
                        MessageEntity::getContent,
                        MessageEntity::getSequenceNumber,
                        MessageEntity::getSenderType
                )
                .containsExactly(
                        tuple(startRequest.getMessage(), 1, MessageSenderType.USER),
                        tuple(ChatbotTestMessages.PROFESSIONAL_GENERAL_START_REPLY, 2, MessageSenderType.ASSISTANT),
                        tuple(request.getMessage(), 3, MessageSenderType.USER),
                        tuple(ChatbotTestMessages.PROFESSIONAL_GENERAL_STATUS_REPLY, 4, MessageSenderType.ASSISTANT)
                );
        assertThat(messages.get(2).getParentMessageId()).isNull();
        assertThat(messages.get(3).getParentMessageId()).isEqualTo(messages.get(2).getId());
    }

    @Test
    void testSendMessageAuthenticatedWithoutConversationIdReturnsBadRequest() {
        HttpHeaders headers = this.authHeaders(
                "fake-token-message-without-conversation",
                "customer-1",
                List.of("customer")
        );

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(null, "Mensaje sin conversacion");
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.MESSAGES,
                POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("conversationId es obligatorio para enviar mensajes");
    }

    @Test
    void testSendMessageAuthenticatedToAnotherUsersConversationReturnsForbidden() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-owned-by-other-user",
                "customer-2",
                null,
                ConversationStatus.ACTIVE,
                ConversationType.GENERAL.name(),
                FIXED_NOW
        )).getId();

        HttpHeaders headers = this.authHeaders("fake-token-forbidden", "customer-1", List.of("customer"));
        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(conversationId, "Mensaje ajeno");
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.MESSAGES,
                POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("No tienes permisos sobre esta conversacion");
    }

    @Test
    void testSendMessageAuthenticatedToClosedConversationReturnsConflict() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-closed",
                "customer-1",
                null,
                ConversationStatus.CLOSED,
                ConversationType.GENERAL.name(),
                FIXED_NOW
        )).getId();

        HttpHeaders headers = this.authHeaders("fake-token-conflict", "customer-1", List.of("customer"));
        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(conversationId, "Mensaje en cerrada");
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.MESSAGES,
                POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("La conversacion no esta activa");
    }

    @Test
    void testCloseConversationAuthenticatedAsOwnerReturnsNoContent() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-to-close",
                "customer-1",
                null,
                ConversationStatus.ACTIVE,
                ConversationType.GENERAL.name(),
                FIXED_NOW
        )).getId();

        HttpHeaders headers = this.authHeaders("fake-token-close", "customer-1", List.of("customer"));
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<Void> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CLOSE_CONVERSATION
                        .replace("{conversationId}", conversationId),
                HttpMethod.PATCH,
                entity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ConversationEntity conversation = this.conversationRepository.findById(conversationId).orElseThrow();
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.CLOSED);
    }

    @Test
    void testCloseConversationAuthenticatedAsOwnerAndAlreadyClosedReturnsNoContent() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-already-closed",
                "customer-1",
                null,
                ConversationStatus.CLOSED,
                ConversationType.GENERAL.name(),
                FIXED_NOW
        )).getId();

        HttpHeaders headers = this.authHeaders("fake-token-close-closed", "customer-1", List.of("customer"));
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<Void> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CLOSE_CONVERSATION
                        .replace("{conversationId}", conversationId),
                HttpMethod.PATCH,
                entity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ConversationEntity conversation = this.conversationRepository.findById(conversationId).orElseThrow();
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.CLOSED);
    }

    @Test
    void testEscalateConversationAuthenticatedAsOwnerReturnsNoContentAndCreatesEscalation() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-to-escalate",
                "customer-1",
                null,
                ConversationStatus.ACTIVE,
                ConversationType.GENERAL.name(),
                FIXED_NOW
        )).getId();

        HttpHeaders headers = this.authHeaders("fake-token-escalate", "customer-1", List.of("customer"));
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<Void> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.ESCALATE_CONVERSATION
                        .replace("{conversationId}", conversationId),
                HttpMethod.PATCH,
                entity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ConversationEntity conversation = this.conversationRepository.findById(conversationId).orElseThrow();
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.ARCHIVED);

        List<EscalationEntity> escalations = this.escalationRepository.findAll();
        assertThat(escalations)
                .singleElement()
                .returns(conversationId, EscalationEntity::getConversationId)
                .returns("customer-1", EscalationEntity::getUserId)
                .returns("+34600111222", EscalationEntity::getPhone)
                .returns("customer-1@example.com", EscalationEntity::getEmail);
    }

    @Test
    void testDeleteConversationAuthenticatedAsOwnerReturnsNoContentAndRemovesMessages() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-to-delete",
                "customer-1",
                null,
                ConversationStatus.CLOSED,
                ConversationType.GENERAL.name(),
                FIXED_NOW
        )).getId();
        this.messageRepository.saveAll(List.of(
                MessageEntity.builder()
                        .id("message-delete-1")
                        .conversationId(conversationId)
                        .senderType(MessageSenderType.USER)
                        .messageType(MessageType.REQUEST)
                        .content("Hola")
                        .timestamp(FIXED_NOW.minusMinutes(2))
                        .sequenceNumber(1)
                        .build(),
                MessageEntity.builder()
                        .id("message-delete-2")
                        .conversationId(conversationId)
                        .senderType(MessageSenderType.ASSISTANT)
                        .messageType(MessageType.RESPONSE)
                        .content("Respuesta")
                        .timestamp(FIXED_NOW.minusMinutes(1))
                        .sequenceNumber(2)
                        .build()
        ));

        HttpHeaders headers = this.authHeaders("fake-token-delete", "customer-1", List.of("customer"));
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<Void> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.DELETE_CONVERSATION
                        .replace("{conversationId}", conversationId),
                HttpMethod.DELETE,
                entity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(this.conversationRepository.findById(conversationId)).isEmpty();
        assertThat(this.messageRepository.findByConversationId(conversationId)).isEmpty();
    }

    @Test
    void testDeleteConversationOfAnotherUserReturnsForbidden() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-owned-by-other-user-to-delete",
                "customer-2",
                null,
                ConversationStatus.ACTIVE,
                ConversationType.GENERAL.name(),
                FIXED_NOW
        )).getId();

        HttpHeaders headers = this.authHeaders("fake-token-delete-forbidden", "customer-1", List.of("customer"));
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.DELETE_CONVERSATION
                        .replace("{conversationId}", conversationId),
                HttpMethod.DELETE,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("No tienes permisos sobre esta conversacion");
        assertThat(this.conversationRepository.findById(conversationId)).isPresent();
    }

    @Test
    void testDeleteConversationUnauthorizedWithoutToken() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-unauthorized-delete",
                "customer-1",
                null,
                ConversationStatus.ACTIVE,
                ConversationType.GENERAL.name(),
                FIXED_NOW
        )).getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.DELETE_CONVERSATION
                        .replace("{conversationId}", conversationId),
                HttpMethod.DELETE,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(this.conversationRepository.findById(conversationId)).isPresent();
    }

    @Test
    void testCloseConversationOfAnotherUserReturnsForbidden() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-owned-by-other-user-to-close",
                "customer-2",
                null,
                ConversationStatus.ACTIVE,
                ConversationType.GENERAL.name(),
                FIXED_NOW
        )).getId();

        HttpHeaders headers = this.authHeaders("fake-token-close-forbidden", "customer-1", List.of("customer"));
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CLOSE_CONVERSATION
                        .replace("{conversationId}", conversationId),
                HttpMethod.PATCH,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("No tienes permisos sobre esta conversacion");
    }

    @Test
    void testEscalateConversationOfAnotherUserReturnsForbidden() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-owned-by-other-user-to-escalate",
                "customer-2",
                null,
                ConversationStatus.ACTIVE,
                ConversationType.GENERAL.name(),
                FIXED_NOW
        )).getId();

        HttpHeaders headers = this.authHeaders("fake-token-escalate-forbidden", "customer-1", List.of("customer"));
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.ESCALATE_CONVERSATION
                        .replace("{conversationId}", conversationId),
                HttpMethod.PATCH,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("No tienes permisos sobre esta conversacion");
        assertThat(this.escalationRepository.findAll()).isEmpty();
    }

    @Test
    void testCloseConversationUnauthorizedWithoutToken() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-unauthorized-close",
                "customer-1",
                null,
                ConversationStatus.ACTIVE,
                ConversationType.GENERAL.name(),
                FIXED_NOW
        )).getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CLOSE_CONVERSATION
                        .replace("{conversationId}", conversationId),
                HttpMethod.PATCH,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testEscalateConversationUnauthorizedWithoutToken() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-unauthorized-escalate",
                "customer-1",
                null,
                ConversationStatus.ACTIVE,
                ConversationType.GENERAL.name(),
                FIXED_NOW
        )).getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.ESCALATE_CONVERSATION
                        .replace("{conversationId}", conversationId),
                HttpMethod.PATCH,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(this.escalationRepository.findAll()).isEmpty();
    }

    @Test
    void testSendMessageInContextualConversationAboutOtherCaseReturnsOutOfScopeReply() {
        HttpHeaders headers = this.authHeaders("fake-token-scope-contextual", "customer-1", List.of("customer"));

        ChatbotContextualConversationRequestDto startRequest = new ChatbotContextualConversationRequestDto();
        startRequest.setEngagementLetterId("aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000");

        HttpEntity<ChatbotContextualConversationRequestDto> startEntity = new HttpEntity<>(startRequest, headers);

        ResponseEntity<ChatbotContextualConversationResponseDto> startResponse = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
                POST,
                startEntity,
                ChatbotContextualConversationResponseDto.class
        );

        assertThat(startResponse.getStatusCode()).isEqualTo(OK);
        assertThat(startResponse.getBody()).isNotNull();

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(
                startResponse.getBody().getConversationId(),
                "¿Qué pasará con mi otro caso?"
        );
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatbotMessageResponseDto> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.MESSAGES,
                POST,
                entity,
                ChatbotMessageResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .isNotNull()
                .returns(startResponse.getBody().getConversationId(), ChatbotMessageResponseDto::getConversationId)
                .returns(ChatbotTestMessages.OUT_OF_CASE_SCOPE_REPLY, ChatbotMessageResponseDto::getMessage)
                .returns(null, ChatbotMessageResponseDto::getError);

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(startResponse.getBody().getConversationId());

        assertThat(messages)
                .hasSize(2)
                .extracting(MessageEntity::getContent, MessageEntity::getSequenceNumber)
                .containsExactly(
                        tuple(request.getMessage(), 1),
                        tuple(ChatbotTestMessages.OUT_OF_CASE_SCOPE_REPLY, 2)
                );
    }

    @Test
    void testSendMessageInContextualConversationUsesPlatformDataWhenContextIsAvailable() {
        String engagementLetterId = "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000";
        when(this.engagementClient.readById(engagementLetterId))
                .thenReturn(new EngagementLetterSummary(
                        UUID.fromString(engagementLetterId),
                        LocalDate.of(2026, Month.APRIL, 1),
                        null,
                        new UserSummary(
                                UUID.fromString("bbbbbbb0-bbbb-cccc-dddd-eeeeffff0000"),
                                "Ana",
                                "Ocaña",
                                "ana@example.com",
                                "600000000"
                        ),
                        List.of(new LegalProcedureSummary(
                                "Reclamación civil",
                                LocalDate.of(2026, Month.APRIL, 2),
                                null,
                                List.of("Revisión documental")
                        ))
                ));
        when(this.engagementClient.readEventsByEngagementLetterId(engagementLetterId, 0, 5))
                .thenReturn(new EngagementEventPage(List.of(
                        new EngagementEventSummary(
                                "MILESTONE",
                                "OPEN",
                                "Se registró escrito",
                                "Escrito de demanda",
                                LocalDate.of(2026, Month.APRIL, 10)
                        ),
                        new EngagementEventSummary(
                                "EVENT",
                                "SCHEDULED",
                                "Vista programada",
                                "Vista inicial",
                                LocalDate.of(2026, Month.APRIL, 15)
                        )
                )));
        HttpHeaders headers = this.authHeaders("fake-token-context-success", "customer-1", List.of("customer"));

        ChatbotContextualConversationRequestDto startRequest = new ChatbotContextualConversationRequestDto();
        startRequest.setEngagementLetterId(engagementLetterId);

        HttpEntity<ChatbotContextualConversationRequestDto> startEntity = new HttpEntity<>(startRequest, headers);

        ResponseEntity<ChatbotContextualConversationResponseDto> startResponse = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
                POST,
                startEntity,
                ChatbotContextualConversationResponseDto.class
        );

        assertThat(startResponse.getStatusCode()).isEqualTo(OK);
        assertThat(startResponse.getBody()).isNotNull();

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(
                startResponse.getBody().getConversationId(),
                "Que hitos recientes tiene el caso"
        );
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatbotMessageResponseDto> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.MESSAGES,
                POST,
                entity,
                ChatbotMessageResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .isNotNull()
                .returns(startResponse.getBody().getConversationId(), ChatbotMessageResponseDto::getConversationId)
                .returns("CONTEXTUAL_PLATFORM_DATA", ChatbotMessageResponseDto::getResponseMode)
                .returns(true, ChatbotMessageResponseDto::getUsedPlatformData)
                .returns(null, ChatbotMessageResponseDto::getError)
                .satisfies(body -> {
                    assertThat(body.getSourcesSummary())
                            .anySatisfy(source -> assertThat(source).startsWith("Hoja de encargo"))
                            .contains(
                                    "Procedimiento: Reclamación civil",
                                    "Hito/evento: Se registró escrito [MILESTONE] - OPEN"
                            );
                    assertThat(body.getMessage())
                            .contains("Se registró escrito")
                            .contains("Vista programada");
                });

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(startResponse.getBody().getConversationId());

        assertThat(messages)
                .hasSize(2)
                .extracting(MessageEntity::getContent, MessageEntity::getSequenceNumber)
                .containsExactly(
                        tuple("Que hitos recientes tiene el caso", 1),
                        tuple(response.getBody().getMessage(), 2)
                );
    }

    @Test
    void testSendMessageInContextualConversationWhenPlatformContextIsUnavailableReturnsRestrictedReply() {
        when(this.engagementClient.readById(anyString()))
                .thenThrow(new IllegalStateException("platform unavailable"));
        HttpHeaders headers = this.authHeaders("fake-token-context-unavailable", "customer-1", List.of("customer"));

        ChatbotContextualConversationRequestDto startRequest = new ChatbotContextualConversationRequestDto();
        startRequest.setEngagementLetterId("engagement-letter-without-platform-context");

        HttpEntity<ChatbotContextualConversationRequestDto> startEntity = new HttpEntity<>(startRequest, headers);

        ResponseEntity<ChatbotContextualConversationResponseDto> startResponse = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
                POST,
                startEntity,
                ChatbotContextualConversationResponseDto.class
        );

        assertThat(startResponse.getStatusCode()).isEqualTo(OK);
        assertThat(startResponse.getBody()).isNotNull();

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(
                startResponse.getBody().getConversationId(),
                "Dame contexto del caso"
        );
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatbotMessageResponseDto> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.MESSAGES,
                POST,
                entity,
                ChatbotMessageResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .isNotNull()
                .returns(startResponse.getBody().getConversationId(), ChatbotMessageResponseDto::getConversationId)
                .returns("CONTEXTUAL_RESTRICTED", ChatbotMessageResponseDto::getResponseMode)
                .returns(false, ChatbotMessageResponseDto::getUsedPlatformData)
                .returns(null, ChatbotMessageResponseDto::getError)
                .satisfies(body -> {
                    assertThat(body.getSourcesSummary()).isEmpty();
                    assertThat(body.getMessage()).contains("no he podido recuperar");
                });

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(startResponse.getBody().getConversationId());

        assertThat(messages)
                .hasSize(2)
                .extracting(MessageEntity::getContent)
                .containsExactly(
                        "Dame contexto del caso",
                        response.getBody().getMessage()
                );
    }

    @Test
    void testSendMessageInGeneralConversationAboutCaseReturnsMissingContextReply() {
        HttpHeaders headers = this.authHeaders("fake-token-scope-general", "customer-1", List.of("customer"));

        ChatbotMessageRequestDto startRequest = new ChatbotMessageRequestDto(null, "Hola chatbot");
        HttpEntity<ChatbotMessageRequestDto> startEntity = new HttpEntity<>(startRequest, headers);

        ResponseEntity<ChatbotMessageResponseDto> startResponse = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.GENERAL_CONVERSATIONS,
                POST,
                startEntity,
                ChatbotMessageResponseDto.class
        );

        assertThat(startResponse.getStatusCode()).isEqualTo(OK);
        assertThat(startResponse.getBody()).isNotNull();

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(
                startResponse.getBody().getConversationId(),
                "¿Cuál es el estado de mi encargo?"
        );
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatbotMessageResponseDto> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.MESSAGES,
                POST,
                entity,
                ChatbotMessageResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .isNotNull()
                .returns(startResponse.getBody().getConversationId(), ChatbotMessageResponseDto::getConversationId)
                .returns(ChatbotTestMessages.MISSING_CASE_CONTEXT_REPLY, ChatbotMessageResponseDto::getMessage)
                .returns(null, ChatbotMessageResponseDto::getError);

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(startResponse.getBody().getConversationId());

        assertThat(messages)
                .hasSize(4)
                .extracting(MessageEntity::getContent)
                .containsExactly(
                        "Hola chatbot",
                        ChatbotTestMessages.CLIENT_GENERAL_START_REPLY,
                        "¿Cuál es el estado de mi encargo?",
                        ChatbotTestMessages.MISSING_CASE_CONTEXT_REPLY
                );
    }

    @Test
    void testSendMessageRequestingBindingLegalAdviceReturnsSafeReply() {
        HttpHeaders headers = this.authHeaders("fake-token-scope-legal", "manager-1", List.of("manager"));

        ChatbotMessageRequestDto startRequest = new ChatbotMessageRequestDto(null, "Necesito soporte");
        HttpEntity<ChatbotMessageRequestDto> startEntity = new HttpEntity<>(startRequest, headers);

        ResponseEntity<ChatbotMessageResponseDto> startResponse = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.GENERAL_CONVERSATIONS,
                POST,
                startEntity,
                ChatbotMessageResponseDto.class
        );

        assertThat(startResponse.getStatusCode()).isEqualTo(OK);
        assertThat(startResponse.getBody()).isNotNull();

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(
                startResponse.getBody().getConversationId(),
                "Dime exactamente qué debo alegar jurídicamente"
        );
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatbotMessageResponseDto> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.MESSAGES,
                POST,
                entity,
                ChatbotMessageResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .isNotNull()
                .returns(startResponse.getBody().getConversationId(), ChatbotMessageResponseDto::getConversationId)
                .returns(ChatbotTestMessages.LEGAL_BINDING_ADVICE_REPLY, ChatbotMessageResponseDto::getMessage)
                .returns(null, ChatbotMessageResponseDto::getError);

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(startResponse.getBody().getConversationId());

        assertThat(messages)
                .hasSize(4)
                .extracting(MessageEntity::getContent)
                .containsExactly(
                        "Necesito soporte",
                        ChatbotTestMessages.PROFESSIONAL_GENERAL_START_REPLY,
                        "Dime exactamente qué debo alegar jurídicamente",
                        ChatbotTestMessages.LEGAL_BINDING_ADVICE_REPLY
                );
    }

    @Test
    void testSendMessageUnauthorizedWithoutToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(null, "Hola chatbot");
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.MESSAGES,
                POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testPreflightOptionsIsAllowed() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ORIGIN, "http://localhost:4200");
        headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
        headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.MESSAGES,
                HttpMethod.OPTIONS,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:4200");
    }

    @Test
    void testPreflightOptionsIsAllowedForCloseConversationPatch() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ORIGIN, "http://localhost:4200");
        headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH");
        headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,authorization");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT
                        + ChatbotResource.CLOSE_CONVERSATION.replace("{conversationId}", "conversation-id"),
                HttpMethod.OPTIONS,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        HttpHeaders responseHeaders = response.getHeaders();
        assertThat(responseHeaders.getAccessControlAllowOrigin()).isEqualTo("http://localhost:4200");
        assertThat(responseHeaders.getAccessControlAllowMethods()).contains(HttpMethod.PATCH);
    }

    private HttpHeaders authHeaders(String token, String subject, List<String> roles) {
        Jwt jwt = new Jwt(
                token,
                FIXED_INSTANT,
                FIXED_INSTANT.plusSeconds(300),
                Map.of("alg", "none"),
                Map.of(
                        "sub", subject,
                        "roles", roles
                )
        );

        when(this.jwtDecoder.decode(token)).thenReturn(jwt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}


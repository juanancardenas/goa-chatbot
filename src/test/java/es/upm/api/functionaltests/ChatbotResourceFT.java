package es.upm.api.functionaltests;

import es.upm.api.infrastructure.dtos.*;
import es.upm.api.infrastructure.mongodb.daos.ConversationRepository;
import es.upm.api.infrastructure.mongodb.daos.MessageRepository;
import es.upm.api.infrastructure.mongodb.entities.ConversationEntity;
import es.upm.api.infrastructure.mongodb.entities.MessageEntity;
import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.domain.model.platform.EngagementEventPage;
import es.upm.api.domain.model.platform.EngagementEventSummary;
import es.upm.api.domain.model.platform.EngagementLetterSummary;
import es.upm.api.domain.model.platform.LegalProcedureSummary;
import es.upm.api.domain.model.platform.UserSummary;
import es.upm.api.domain.services.ai.ChatbotAiClient;
import es.upm.api.domain.webclients.EngagementWebClient;
import es.upm.api.functionaltests.support.ChatbotTestMessages;
import es.upm.api.infrastructure.resources.ChatbotResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChatbotResourceFT {
    private static final String TYPE_GENERAL = "GENERAL";
    private static final String TYPE_CONTEXTUAL = "CONTEXTUAL";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @LocalServerPort
    private int port;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private EngagementWebClient engagementWebClient;

    @MockitoBean
    private ChatbotAiClient chatbotAiClient;

    @BeforeEach
    void setUp() {
        this.messageRepository.deleteAll();
        this.conversationRepository.deleteAll();
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConversationId()).isNotBlank();
        assertThat(response.getBody().getEngagementLetterId()).isEqualTo("aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000");
        assertThat(response.getBody().getCreatedAt()).isNotBlank();
        assertThat(response.getBody().getError()).isNull();

        List<ConversationEntity> conversations = this.conversationRepository.findAll();
        assertThat(conversations).hasSize(1);
        assertThat(conversations.getFirst().getUserId()).isEqualTo("customer-1");
        assertThat(conversations.getFirst().getEngagementLetterId()).isEqualTo("aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000");
        assertThat(conversations.getFirst().getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(conversations.getFirst().getType()).isEqualTo(TYPE_CONTEXTUAL);
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"code\":400");
        assertThat(response.getBody()).contains("engagementLetterId es obligatorio");
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"code\":400");
        assertThat(response.getBody()).contains("engagementLetterId es obligatorio");
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
        assertThat(secondResponse.getBody()).isNotNull();
        assertThat(secondResponse.getBody().getConversationId()).isEqualTo(firstResponse.getBody().getConversationId());

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
        assertThat(conversations).hasSize(2);
        assertThat(conversations)
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
                TYPE_GENERAL,
                LocalDateTime.now().minusHours(5)
        );
        ConversationEntity generalActive = new ConversationEntity(
                "general-active-001",
                userId,
                null,
                ConversationStatus.ACTIVE,
                TYPE_GENERAL,
                LocalDateTime.now().minusHours(1)
        );
        ConversationEntity contextual = new ConversationEntity(
                "contextual-001",
                userId,
                "eng-001",
                ConversationStatus.ACTIVE,
                TYPE_CONTEXTUAL,
                LocalDateTime.now().minusMinutes(30)
        );
        ConversationEntity anotherUserGeneral = new ConversationEntity(
                "general-other-user-001",
                "customer-2",
                null,
                ConversationStatus.ACTIVE,
                TYPE_GENERAL,
                LocalDateTime.now().minusMinutes(10)
        );

        this.conversationRepository.saveAll(List.of(generalClosed, generalActive, contextual, anotherUserGeneral));

        this.messageRepository.saveAll(List.of(
                MessageEntity.builder()
                        .id("msg-general-closed")
                        .conversationId(generalClosed.getId())
                        .senderType(MessageSenderType.USER)
                        .messageType(MessageType.REQUEST)
                        .content("Consulta antigua general")
                        .timestamp(LocalDateTime.now().minusHours(4))
                        .sequenceNumber(1)
                        .build(),
                MessageEntity.builder()
                        .id("msg-general-active")
                        .conversationId(generalActive.getId())
                        .senderType(MessageSenderType.USER)
                        .messageType(MessageType.REQUEST)
                        .content("Consulta reciente general")
                        .timestamp(LocalDateTime.now().minusMinutes(50))
                        .sequenceNumber(1)
                        .build(),
                MessageEntity.builder()
                        .id("msg-contextual")
                        .conversationId(contextual.getId())
                        .senderType(MessageSenderType.USER)
                        .messageType(MessageType.REQUEST)
                        .content("Consulta contextual")
                        .timestamp(LocalDateTime.now().minusMinutes(20))
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);

        assertThat(response.getBody())
                .extracting(ChatbotConversationSummaryDto::getConversationId)
                .containsExactly("general-active-001", "general-closed-001");

        assertThat(response.getBody())
                .extracting(ChatbotConversationSummaryDto::getPreview)
                .containsExactly("Consulta reciente general", "Consulta antigua general");

        assertThat(response.getBody())
                .extracting(ChatbotConversationSummaryDto::getType)
                .containsOnly(TYPE_GENERAL);
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isEnabled()).isTrue();
        assertThat(response.getBody().getProvider()).isEqualTo("ollama");
        assertThat(response.getBody().getModel()).isNotBlank();
        assertThat(response.getBody().getMaxInputCharacters()).isGreaterThan(0);
        assertThat(response.getBody().getMaxOutputTokens()).isGreaterThan(0);
        assertThat(response.getBody().getMaxContextMessages()).isGreaterThanOrEqualTo(0);
        assertThat(response.getBody().isDocumentsAvailable()).isFalse();
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("provider");
        assertThat(response.getBody()).contains("model");

        assertThat(response.getBody()).doesNotContain("apiKey");
        assertThat(response.getBody()).doesNotContain("basePrompt");
        assertThat(response.getBody()).doesNotContain("prompt");
        assertThat(response.getBody()).doesNotContain("CHATBOT_OPENAI_API_KEY");
        assertThat(response.getBody()).doesNotContain("CHATBOT_GEMINI_API_KEY");
        assertThat(response.getBody()).doesNotContain("secret");
        assertThat(response.getBody()).doesNotContain("stackTrace");
    }

    @Test
    void testReadContextualConversationHistoryListAuthenticatedReturnsOnlyContextualConversationsOfEngagement() {
        String userId = "customer-1";

        ConversationEntity contextualA = new ConversationEntity(
                "contextual-a-001",
                userId,
                "eng-001",
                ConversationStatus.ACTIVE,
                TYPE_CONTEXTUAL,
                LocalDateTime.now().minusHours(3)
        );
        ConversationEntity contextualB = new ConversationEntity(
                "contextual-b-001",
                userId,
                "eng-001",
                ConversationStatus.CLOSED,
                TYPE_CONTEXTUAL,
                LocalDateTime.now().minusHours(1)
        );
        ConversationEntity contextualOtherEngagement = new ConversationEntity(
                "contextual-other-engagement-001",
                userId,
                "eng-002",
                ConversationStatus.ACTIVE,
                TYPE_CONTEXTUAL,
                LocalDateTime.now().minusMinutes(20)
        );
        ConversationEntity general = new ConversationEntity(
                "general-001",
                userId,
                null,
                ConversationStatus.ACTIVE,
                TYPE_GENERAL,
                LocalDateTime.now().minusMinutes(10)
        );

        this.conversationRepository.saveAll(List.of(contextualA, contextualB, contextualOtherEngagement, general));

        this.messageRepository.saveAll(List.of(
                MessageEntity.builder()
                        .id("msg-contextual-a")
                        .conversationId(contextualA.getId())
                        .senderType(MessageSenderType.USER)
                        .messageType(MessageType.REQUEST)
                        .content("Contextual antigua")
                        .timestamp(LocalDateTime.now().minusHours(2))
                        .sequenceNumber(1)
                        .build(),
                MessageEntity.builder()
                        .id("msg-contextual-b")
                        .conversationId(contextualB.getId())
                        .senderType(MessageSenderType.USER)
                        .messageType(MessageType.REQUEST)
                        .content("Contextual más reciente")
                        .timestamp(LocalDateTime.now().minusMinutes(40))
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);

        assertThat(response.getBody())
                .extracting(ChatbotConversationSummaryDto::getConversationId)
                .containsExactly("contextual-b-001", "contextual-a-001");

        assertThat(response.getBody())
                .extracting(ChatbotConversationSummaryDto::getEngagementLetterId)
                .containsOnly("eng-001");

        assertThat(response.getBody())
                .extracting(ChatbotConversationSummaryDto::getType)
                .containsOnly(TYPE_CONTEXTUAL);
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
                TYPE_CONTEXTUAL,
                LocalDateTime.now().minusHours(1)
        ));

        this.messageRepository.saveAll(List.of(
                MessageEntity.builder()
                        .id("history-msg-2")
                        .conversationId(conversationId)
                        .senderType(MessageSenderType.ASSISTANT)
                        .messageType(MessageType.RESPONSE)
                        .content("Respuesta del asistente")
                        .timestamp(LocalDateTime.now().minusMinutes(9))
                        .sequenceNumber(2)
                        .parentMessageId("history-msg-1")
                        .build(),
                MessageEntity.builder()
                        .id("history-msg-1")
                        .conversationId(conversationId)
                        .senderType(MessageSenderType.USER)
                        .messageType(MessageType.REQUEST)
                        .content("Consulta inicial")
                        .timestamp(LocalDateTime.now().minusMinutes(10))
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConversationId()).isEqualTo(conversationId);
        assertThat(response.getBody().getType()).isEqualTo(TYPE_CONTEXTUAL);
        assertThat(response.getBody().getStatus()).isEqualTo(ConversationStatus.CLOSED.name());
        assertThat(response.getBody().getMessages()).hasSize(2);
        assertThat(response.getBody().getMessages())
                .extracting(message -> message.getSequenceNumber())
                .containsExactly(1, 2);
        assertThat(response.getBody().getMessages())
                .extracting(message -> message.getContent())
                .containsExactly("Consulta inicial", "Respuesta del asistente");
    }

    @Test
    void testReadContextualConversationHistoryListWithoutEngagementLetterReturnsBadRequest() {
        HttpHeaders headers = this.authHeaders("fake-token-contextual-list-bad-request", "customer-1", List.of("customer"));

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CONVERSATIONS + "?type=CONTEXTUAL",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("engagementLetterId es obligatorio para listar conversaciones contextuales");
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
                TYPE_GENERAL,
                LocalDateTime.now()
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
                TYPE_GENERAL,
                LocalDateTime.now()
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
                TYPE_GENERAL,
                LocalDateTime.now()
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConversationId()).isNotBlank();
        assertThat(response.getBody().getMessage()).isEqualTo(ChatbotTestMessages.CLIENT_GENERAL_START_REPLY);
        assertThat(response.getBody().getError()).isNull();
        assertThat(response.getBody().getCreatedAt()).isNotBlank();

        List<ConversationEntity> conversations = this.conversationRepository.findAll();
        assertThat(conversations).hasSize(1);
        assertThat(conversations.getFirst().getUserId()).isEqualTo("customer-1");
        assertThat(conversations.getFirst().getEngagementLetterId()).isNull();
        assertThat(conversations.getFirst().getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(conversations.getFirst().getType()).isEqualTo(TYPE_GENERAL);

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(conversations.getFirst().getId());
        assertThat(messages).hasSize(2);

        MessageEntity firstMessage = messages.get(0);
        assertThat(firstMessage.getSenderType()).isEqualTo(MessageSenderType.USER);
        assertThat(firstMessage.getMessageType()).isEqualTo(MessageType.REQUEST);
        assertThat(firstMessage.getContent()).isEqualTo("Hola chatbot");
        assertThat(firstMessage.getSequenceNumber()).isEqualTo(1);
        assertThat(firstMessage.getTimestamp()).isNotNull();
        assertThat(firstMessage.getParentMessageId()).isNull();

        MessageEntity secondMessage = messages.get(1);
        assertThat(secondMessage.getSenderType()).isEqualTo(MessageSenderType.ASSISTANT);
        assertThat(secondMessage.getMessageType()).isEqualTo(MessageType.RESPONSE);
        assertThat(secondMessage.getContent()).isEqualTo(ChatbotTestMessages.CLIENT_GENERAL_START_REPLY);
        assertThat(secondMessage.getSequenceNumber()).isEqualTo(2);
        assertThat(secondMessage.getTimestamp()).isNotNull();
        assertThat(secondMessage.getParentMessageId()).isEqualTo(firstMessage.getId());
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConversationId()).isNotBlank();
        assertThat(response.getBody().getMessage()).isEqualTo(ChatbotTestMessages.PROFESSIONAL_GENERAL_START_REPLY);
        assertThat(response.getBody().getError()).isNull();
        assertThat(response.getBody().getCreatedAt()).isNotBlank();

        List<ConversationEntity> conversations = this.conversationRepository.findAll();
        assertThat(conversations).hasSize(1);
        assertThat(conversations.getFirst().getUserId()).isEqualTo("admin-1");
        assertThat(conversations.getFirst().getEngagementLetterId()).isNull();
        assertThat(conversations.getFirst().getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(conversations.getFirst().getType()).isEqualTo(TYPE_GENERAL);

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(conversations.getFirst().getId());
        assertThat(messages).hasSize(2);
        assertThat(messages).extracting(MessageEntity::getContent)
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConversationId()).isEqualTo(startResponse.getBody().getConversationId());
        assertThat(response.getBody().getMessage()).isEqualTo(ChatbotTestMessages.CLIENT_GENERAL_STATUS_REPLY);
        assertThat(response.getBody().getError()).isNull();
        assertThat(response.getBody().getCreatedAt()).isNotBlank();

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(startResponse.getBody().getConversationId());
        assertThat(messages).hasSize(4);
        assertThat(messages).extracting(MessageEntity::getContent)
                .containsExactly(
                        "Hola chatbot",
                        ChatbotTestMessages.CLIENT_GENERAL_START_REPLY,
                        "¿Cómo puedo consultar el estado de un encargo?",
                        ChatbotTestMessages.CLIENT_GENERAL_STATUS_REPLY
                );
        assertThat(messages).extracting(MessageEntity::getSequenceNumber)
                .containsExactly(1, 2, 3, 4);
        assertThat(messages).extracting(MessageEntity::getSenderType)
                .containsExactly(
                        MessageSenderType.USER,
                        MessageSenderType.ASSISTANT,
                        MessageSenderType.USER,
                        MessageSenderType.ASSISTANT
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConversationId()).isEqualTo(startResponse.getBody().getConversationId());
        assertThat(response.getBody().getMessage()).isEqualTo(ChatbotTestMessages.PROFESSIONAL_GENERAL_STATUS_REPLY);
        assertThat(response.getBody().getError()).isNull();
        assertThat(response.getBody().getCreatedAt()).isNotBlank();

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(startResponse.getBody().getConversationId());
        assertThat(messages).hasSize(4);
        assertThat(messages).extracting(MessageEntity::getContent)
                .containsExactly(
                        "Necesito revisar el flujo",
                        ChatbotTestMessages.PROFESSIONAL_GENERAL_START_REPLY,
                        "Necesito saber cómo revisar el estado de un encargo",
                        ChatbotTestMessages.PROFESSIONAL_GENERAL_STATUS_REPLY
                );
        assertThat(messages).extracting(MessageEntity::getSequenceNumber)
                .containsExactly(1, 2, 3, 4);
        assertThat(messages).extracting(MessageEntity::getSenderType)
                .containsExactly(
                        MessageSenderType.USER,
                        MessageSenderType.ASSISTANT,
                        MessageSenderType.USER,
                        MessageSenderType.ASSISTANT
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
                TYPE_GENERAL,
                LocalDateTime.now()
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
                TYPE_GENERAL,
                LocalDateTime.now()
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
                TYPE_GENERAL,
                LocalDateTime.now()
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
    void testDeleteConversationAuthenticatedAsOwnerReturnsNoContentAndRemovesMessages() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-to-delete",
                "customer-1",
                null,
                ConversationStatus.CLOSED,
                TYPE_GENERAL,
                LocalDateTime.now()
        )).getId();
        this.messageRepository.saveAll(List.of(
                MessageEntity.builder()
                        .id("message-delete-1")
                        .conversationId(conversationId)
                        .senderType(MessageSenderType.USER)
                        .messageType(MessageType.REQUEST)
                        .content("Hola")
                        .timestamp(LocalDateTime.now().minusMinutes(2))
                        .sequenceNumber(1)
                        .build(),
                MessageEntity.builder()
                        .id("message-delete-2")
                        .conversationId(conversationId)
                        .senderType(MessageSenderType.ASSISTANT)
                        .messageType(MessageType.RESPONSE)
                        .content("Respuesta")
                        .timestamp(LocalDateTime.now().minusMinutes(1))
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
                TYPE_GENERAL,
                LocalDateTime.now()
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
                TYPE_GENERAL,
                LocalDateTime.now()
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
                TYPE_GENERAL,
                LocalDateTime.now()
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
    void testCloseConversationUnauthorizedWithoutToken() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-unauthorized-close",
                "customer-1",
                null,
                ConversationStatus.ACTIVE,
                TYPE_GENERAL,
                LocalDateTime.now()
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConversationId()).isEqualTo(startResponse.getBody().getConversationId());
        assertThat(response.getBody().getMessage()).isEqualTo(ChatbotTestMessages.OUT_OF_CASE_SCOPE_REPLY);
        assertThat(response.getBody().getError()).isNull();

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(startResponse.getBody().getConversationId());

        assertThat(messages).hasSize(2);
        assertThat(messages).extracting(MessageEntity::getContent)
                .containsExactly(
                        "¿Qué pasará con mi otro caso?",
                        ChatbotTestMessages.OUT_OF_CASE_SCOPE_REPLY
                );
        assertThat(messages).extracting(MessageEntity::getSequenceNumber)
                .containsExactly(1, 2);
    }

    @Test
    void testSendMessageInContextualConversationUsesPlatformDataWhenContextIsAvailable() {
        String engagementLetterId = "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000";
        when(this.engagementWebClient.readById(engagementLetterId))
                .thenReturn(new EngagementLetterSummary(
                        UUID.fromString(engagementLetterId),
                        LocalDate.of(2026, 4, 1),
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
                                LocalDate.of(2026, 4, 2),
                                null,
                                List.of("Revisión documental")
                        ))
                ));
        when(this.engagementWebClient.readEventsByEngagementLetterId(engagementLetterId, 0, 5))
                .thenReturn(new EngagementEventPage(List.of(
                        new EngagementEventSummary(
                                "MILESTONE",
                                "OPEN",
                                "Se registró escrito",
                                "Escrito de demanda",
                                LocalDate.of(2026, 4, 10)
                        ),
                        new EngagementEventSummary(
                                "EVENT",
                                "SCHEDULED",
                                "Vista programada",
                                "Vista inicial",
                                LocalDate.of(2026, 4, 15)
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConversationId()).isEqualTo(startResponse.getBody().getConversationId());
        assertThat(response.getBody().getResponseMode()).isEqualTo("CONTEXTUAL_PLATFORM_DATA");
        assertThat(response.getBody().getUsedPlatformData()).isTrue();
        assertThat(response.getBody().getSourcesSummary())
                .anySatisfy(source -> assertThat(source).startsWith("Hoja de encargo"))
                .contains(
                        "Procedimiento: Reclamación civil",
                        "Hito/evento: Se registró escrito [MILESTONE] - OPEN"
                );
        assertThat(response.getBody().getError()).isNull();
        assertThat(response.getBody().getMessage()).contains("Se registró escrito");
        assertThat(response.getBody().getMessage()).contains("Vista programada");

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(startResponse.getBody().getConversationId());

        assertThat(messages).hasSize(2);
        assertThat(messages).extracting(MessageEntity::getContent)
                .containsExactly(
                        "Que hitos recientes tiene el caso",
                        response.getBody().getMessage()
                );
        assertThat(messages).extracting(MessageEntity::getSequenceNumber)
                .containsExactly(1, 2);
    }

    @Test
    void testSendMessageInContextualConversationWhenPlatformContextIsUnavailableReturnsRestrictedReply() {
        when(this.engagementWebClient.readById(anyString()))
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConversationId()).isEqualTo(startResponse.getBody().getConversationId());
        assertThat(response.getBody().getResponseMode()).isEqualTo("CONTEXTUAL_RESTRICTED");
        assertThat(response.getBody().getUsedPlatformData()).isFalse();
        assertThat(response.getBody().getSourcesSummary()).isEmpty();
        assertThat(response.getBody().getError()).isNull();
        assertThat(response.getBody().getMessage()).contains("no he podido recuperar");

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(startResponse.getBody().getConversationId());

        assertThat(messages).hasSize(2);
        assertThat(messages).extracting(MessageEntity::getContent)
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConversationId()).isEqualTo(startResponse.getBody().getConversationId());
        assertThat(response.getBody().getMessage()).isEqualTo(ChatbotTestMessages.MISSING_CASE_CONTEXT_REPLY);
        assertThat(response.getBody().getError()).isNull();

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(startResponse.getBody().getConversationId());

        assertThat(messages).hasSize(4);
        assertThat(messages).extracting(MessageEntity::getContent)
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConversationId()).isEqualTo(startResponse.getBody().getConversationId());
        assertThat(response.getBody().getMessage()).isEqualTo(ChatbotTestMessages.LEGAL_BINDING_ADVICE_REPLY);
        assertThat(response.getBody().getError()).isNull();

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(startResponse.getBody().getConversationId());

        assertThat(messages).hasSize(4);
        assertThat(messages).extracting(MessageEntity::getContent)
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
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:4200");
        assertThat(response.getHeaders().getAccessControlAllowMethods()).contains(HttpMethod.PATCH);
    }

    private HttpHeaders authHeaders(String token, String subject, List<String> roles) {
        Jwt jwt = new Jwt(
                token,
                Instant.now(),
                Instant.now().plusSeconds(300),
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

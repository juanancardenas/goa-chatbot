package es.upm.api.functionaltests;

import es.upm.api.infrastructure.mongodb.daos.ConversationRepository;
import es.upm.api.infrastructure.mongodb.daos.MessageRepository;
import es.upm.api.infrastructure.mongodb.entities.ConversationEntity;
import es.upm.api.infrastructure.mongodb.entities.MessageEntity;
import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.functionaltests.support.ChatbotTestMessages;
import es.upm.api.infrastructure.resources.ChatbotResource;
import es.upm.api.infrastructure.dtos.ChatbotContextualConversationRequestDto;
import es.upm.api.infrastructure.dtos.ChatbotContextualConversationResponseDto;
import es.upm.api.infrastructure.dtos.ChatbotMessageRequestDto;
import es.upm.api.infrastructure.dtos.ChatbotMessageResponseDto;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
                "Segundo mensaje"
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
        assertThat(response.getBody().getMessage()).isEqualTo(ChatbotTestMessages.CLIENT_MESSAGE_REPLY);
        assertThat(response.getBody().getError()).isNull();
        assertThat(response.getBody().getCreatedAt()).isNotBlank();

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(startResponse.getBody().getConversationId());
        assertThat(messages).hasSize(4);
        assertThat(messages).extracting(MessageEntity::getContent)
                .containsExactly(
                        "Hola chatbot",
                        ChatbotTestMessages.CLIENT_GENERAL_START_REPLY,
                        "Segundo mensaje",
                        ChatbotTestMessages.CLIENT_MESSAGE_REPLY
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
                "Segundo mensaje profesional"
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
        assertThat(response.getBody().getMessage()).isEqualTo(ChatbotTestMessages.PROFESSIONAL_MESSAGE_REPLY);
        assertThat(response.getBody().getError()).isNull();
        assertThat(response.getBody().getCreatedAt()).isNotBlank();

        List<MessageEntity> messages = this.messageRepository
                .findByConversationIdOrderBySequenceNumberAsc(startResponse.getBody().getConversationId());
        assertThat(messages).hasSize(4);
        assertThat(messages).extracting(MessageEntity::getContent)
                .containsExactly(
                        "Necesito revisar el flujo",
                        ChatbotTestMessages.PROFESSIONAL_GENERAL_START_REPLY,
                        "Segundo mensaje profesional",
                        ChatbotTestMessages.PROFESSIONAL_MESSAGE_REPLY
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
    void testCloseConversationAlreadyClosedReturnsConflict() {
        String conversationId = this.conversationRepository.save(new ConversationEntity(
                "conversation-already-closed",
                "customer-1",
                null,
                ConversationStatus.CLOSED,
                TYPE_GENERAL,
                LocalDateTime.now()
        )).getId();

        HttpHeaders headers = this.authHeaders("fake-token-close-conflict", "customer-1", List.of("customer"));
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + this.port + ChatbotResource.CHATBOT + ChatbotResource.CLOSE_CONVERSATION
                        .replace("{conversationId}", conversationId),
                HttpMethod.PATCH,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("La conversacion no esta activa");
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

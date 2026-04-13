package es.upm.api.functionaltests;

import es.upm.api.data.daos.ConversationRepository;
import es.upm.api.data.daos.MessageRepository;
import es.upm.api.data.entities.ConversationEntity;
import es.upm.api.data.entities.ConversationStatus;
import es.upm.api.data.entities.MessageEntity;
import es.upm.api.data.entities.MessageSenderType;
import es.upm.api.data.entities.MessageType;
import es.upm.api.resources.ChatbotResource;
import es.upm.api.resources.dtos.ChatbotContextualConversationRequestDto;
import es.upm.api.resources.dtos.ChatbotContextualConversationResponseDto;
import es.upm.api.resources.dtos.ChatbotMessageRequestDto;
import es.upm.api.resources.dtos.ChatbotMessageResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChatbotResourceFT {

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
        this.conversationRepository.deleteAll();
        this.messageRepository.deleteAll();
    }

    @Test
    void testStartContextualConversationAuthenticated() {
        HttpHeaders headers = this.authHeaders("fake-token-contextual", "customer-1");

        ChatbotContextualConversationRequestDto request = new ChatbotContextualConversationRequestDto();
        request.setEngagementLetterId("aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000");

        HttpEntity<ChatbotContextualConversationRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatbotContextualConversationResponseDto> response = restTemplate.exchange(
                "http://localhost:" + port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
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
        assertThat(conversations.getFirst().getType()).isEqualTo("CONTEXTUAL");
    }

    @Test
    void testStartContextualConversationUnauthorizedWithoutToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ChatbotContextualConversationRequestDto request = new ChatbotContextualConversationRequestDto();
        request.setEngagementLetterId("aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000");

        HttpEntity<ChatbotContextualConversationRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
                POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testStartContextualConversationAuthenticatedWhenEngagementLetterIdIsBlank() {
        HttpHeaders headers = this.authHeaders("fake-token-blank", "customer-1");

        ChatbotContextualConversationRequestDto request = new ChatbotContextualConversationRequestDto();
        request.setEngagementLetterId("");

        HttpEntity<ChatbotContextualConversationRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
                POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);

        List<ConversationEntity> conversations = this.conversationRepository.findAll();
        assertThat(conversations).hasSize(1);
        assertThat(conversations.getFirst().getEngagementLetterId()).isNull();
        assertThat(conversations.getFirst().getStatus()).isEqualTo(ConversationStatus.ACTIVE);
    }

    @Test
    void testStartContextualConversationAuthenticatedWithoutEngagementLetterId() {
        HttpHeaders headers = this.authHeaders("fake-token-null", "customer-1");

        ChatbotContextualConversationRequestDto request = new ChatbotContextualConversationRequestDto();

        HttpEntity<ChatbotContextualConversationRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatbotContextualConversationResponseDto> response = restTemplate.exchange(
                "http://localhost:" + port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
                POST,
                entity,
                ChatbotContextualConversationResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConversationId()).isNotBlank();
        assertThat(response.getBody().getEngagementLetterId()).isNull();
        assertThat(response.getBody().getCreatedAt()).isNotBlank();
        assertThat(response.getBody().getError()).isNull();

        List<ConversationEntity> conversations = this.conversationRepository.findAll();
        assertThat(conversations).hasSize(1);
        assertThat(conversations.getFirst().getUserId()).isEqualTo("customer-1");
        assertThat(conversations.getFirst().getEngagementLetterId()).isNull();
        assertThat(conversations.getFirst().getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(conversations.getFirst().getType()).isEqualTo("CONTEXTUAL");
    }

    @Test
    void testStartContextualConversationReusesSameConversationForSameUserAndEngagementLetter() {
        HttpHeaders headers = this.authHeaders("fake-token-reuse", "customer-1");

        ChatbotContextualConversationRequestDto request = new ChatbotContextualConversationRequestDto();
        request.setEngagementLetterId("aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000");

        HttpEntity<ChatbotContextualConversationRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatbotContextualConversationResponseDto> firstResponse = restTemplate.exchange(
                "http://localhost:" + port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
                POST,
                entity,
                ChatbotContextualConversationResponseDto.class
        );

        ResponseEntity<ChatbotContextualConversationResponseDto> secondResponse = restTemplate.exchange(
                "http://localhost:" + port + ChatbotResource.CHATBOT + ChatbotResource.CONTEXTUAL_CONVERSATIONS,
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
    void testStartGeneralConversationAuthenticated() {
        HttpHeaders headers = this.authHeaders("fake-token-general", "customer-1");

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(null, "Hola chatbot");
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatbotMessageResponseDto> response = restTemplate.exchange(
                "http://localhost:" + port + ChatbotResource.CHATBOT + ChatbotResource.GENERAL_CONVERSATIONS,
                POST,
                entity,
                ChatbotMessageResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConversationId()).isNotBlank();
        assertThat(response.getBody().getMessage()).isEqualTo("Hola chatbot");
        assertThat(response.getBody().getError()).isNull();
        assertThat(response.getBody().getCreatedAt()).isNotBlank();

        List<ConversationEntity> conversations = this.conversationRepository.findAll();
        assertThat(conversations).hasSize(1);
        assertThat(conversations.getFirst().getUserId()).isEqualTo("customer-1");
        assertThat(conversations.getFirst().getEngagementLetterId()).isNull();
        assertThat(conversations.getFirst().getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(conversations.getFirst().getType()).isEqualTo("GENERAL");

        List<MessageEntity> messages = this.messageRepository.findAll();
        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst().getConversationId()).isEqualTo(conversations.getFirst().getId());
        assertThat(messages.getFirst().getSenderType()).isEqualTo(MessageSenderType.USER);
        assertThat(messages.getFirst().getMessageType()).isEqualTo(MessageType.REQUEST);
        assertThat(messages.getFirst().getContent()).isEqualTo("Hola chatbot");
        assertThat(messages.getFirst().getSequenceNumber()).isEqualTo(1);
        assertThat(messages.getFirst().getTimestamp()).isNotNull();
        assertThat(messages.getFirst().getParentMessageId()).isNull();
    }

    @Test
    void testStartGeneralConversationUnauthorizedWithoutToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(null, "Hola chatbot");
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + ChatbotResource.CHATBOT + ChatbotResource.GENERAL_CONVERSATIONS,
                POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testSendMessageAuthenticated() {
        HttpHeaders headers = this.authHeaders("fake-token-message", "customer-1");

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(null, "Hola chatbot");
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatbotMessageResponseDto> response = restTemplate.exchange(
                "http://localhost:" + port + ChatbotResource.CHATBOT + ChatbotResource.MESSAGES,
                POST,
                entity,
                ChatbotMessageResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConversationId()).isNotBlank();
        assertThat(response.getBody().getMessage()).isEqualTo("Respuesta simulada del asistente externo");
    }

    @Test
    void testSendMessageUnauthorizedWithoutToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ChatbotMessageRequestDto request = new ChatbotMessageRequestDto(null, "Hola chatbot");
        HttpEntity<ChatbotMessageRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + ChatbotResource.CHATBOT + ChatbotResource.MESSAGES,
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

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + ChatbotResource.CHATBOT + ChatbotResource.MESSAGES,
                HttpMethod.OPTIONS,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:4200");
    }

    private HttpHeaders authHeaders(String token, String subject) {
        Jwt jwt = new Jwt(
                token,
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of(
                        "sub", subject,
                        "roles", List.of("customer")
                )
        );

        when(this.jwtDecoder.decode(token)).thenReturn(jwt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

}

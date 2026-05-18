package es.upm.api.infrastructure.resources;

import es.upm.api.domain.enums.ChatbotResponseMode;
import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.model.chatbot.command.ChatbotContextualConversationCommand;
import es.upm.api.domain.model.chatbot.command.ChatbotMessageCommand;
import es.upm.api.domain.model.chatbot.result.ChatbotConfigurationResult;
import es.upm.api.domain.model.chatbot.result.ChatbotContextualConversationResult;
import es.upm.api.domain.model.chatbot.result.ChatbotConversationHistoryResult;
import es.upm.api.domain.model.chatbot.result.ChatbotConversationSummaryResult;
import es.upm.api.domain.model.chatbot.result.ChatbotHistoryMessageResult;
import es.upm.api.domain.model.chatbot.result.ChatbotMessageResult;
import es.upm.api.domain.model.security.AuthenticatedUserContext;
import es.upm.api.domain.ports.in.CloseConversationUseCase;
import es.upm.api.domain.ports.in.DeleteConversationUseCase;
import es.upm.api.domain.ports.in.EscalateConversationUseCase;
import es.upm.api.domain.ports.in.ReadChatbotConfigurationUseCase;
import es.upm.api.domain.ports.in.ReadConversationHistoryListUseCase;
import es.upm.api.domain.ports.in.ReadConversationHistoryUseCase;
import es.upm.api.domain.ports.in.ReopenConversationUseCase;
import es.upm.api.domain.ports.in.SendChatbotMessageUseCase;
import es.upm.api.domain.ports.in.StartContextualConversationUseCase;
import es.upm.api.domain.ports.in.StartGeneralConversationUseCase;
import es.upm.api.infrastructure.dtos.ChatbotContextualConversationRequestDto;
import es.upm.api.infrastructure.dtos.ChatbotMessageRequestDto;
import es.upm.api.infrastructure.security.AuthenticatedUserContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotResourceTest {

    @Mock
    private ReadConversationHistoryListUseCase readConversationHistoryListUseCase;

    @Mock
    private StartContextualConversationUseCase startContextualConversationUseCase;

    @Mock
    private StartGeneralConversationUseCase startGeneralConversationUseCase;

    @Mock
    private SendChatbotMessageUseCase sendChatbotMessageUseCase;

    @Mock
    private ReadChatbotConfigurationUseCase readChatbotConfigurationUseCase;

    @Mock
    private ReadConversationHistoryUseCase readConversationHistoryUseCase;

    @Mock
    private DeleteConversationUseCase deleteConversationUseCase;

    @Mock
    private CloseConversationUseCase closeConversationUseCase;

    @Mock
    private ReopenConversationUseCase reopenConversationUseCase;

    @Mock
    private EscalateConversationUseCase escalateConversationUseCase;

    @Mock
    private AuthenticatedUserContextResolver authenticatedUserContextResolver;

    @Mock
    private Authentication authentication;

    private ChatbotResource chatbotResource;
    private AuthenticatedUserContext authenticatedUser;

    @BeforeEach
    void setUp() {
        this.authenticatedUser = AuthenticatedUserContext.builder()
                .userId("user-1")
                .profile(ConversationProfileType.CLIENT)
                .build();

        this.chatbotResource = new ChatbotResource(
                this.readConversationHistoryListUseCase,
                this.startContextualConversationUseCase,
                this.startGeneralConversationUseCase,
                this.sendChatbotMessageUseCase,
                this.readChatbotConfigurationUseCase,
                this.readConversationHistoryUseCase,
                this.deleteConversationUseCase,
                this.closeConversationUseCase,
                this.reopenConversationUseCase,
                this.escalateConversationUseCase,
                this.authenticatedUserContextResolver
        );
    }

    @Test
    void readConversationsShouldResolveUserAndCallHistoryListPort() {
        this.mockAuthenticatedUser();
        when(this.readConversationHistoryListUseCase.readConversationHistoryList(
                this.authenticatedUser,
                "GENERAL",
                null
        )).thenReturn(List.of(ChatbotConversationSummaryResult.builder()
                .conversationId("conversation-1")
                .type("GENERAL")
                .status("ACTIVE")
                .createdAt("2026-05-18T10:00")
                .preview("Hola")
                .build()));

        var response = this.chatbotResource.readConversations("GENERAL", null, this.authentication);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getConversationId()).isEqualTo("conversation-1");
        assertThat(response.getFirst().getPreview()).isEqualTo("Hola");
        verify(this.authenticatedUserContextResolver).resolve(this.authentication);
        verify(this.readConversationHistoryListUseCase)
                .readConversationHistoryList(this.authenticatedUser, "GENERAL", null);
    }

    @Test
    void startContextualConversationShouldMapRequestAndCallPort() {
        this.mockAuthenticatedUser();
        ChatbotContextualConversationRequestDto requestDto = new ChatbotContextualConversationRequestDto();
        requestDto.setEngagementLetterId("EL-1");
        when(this.startContextualConversationUseCase.startContextualConversation(
                any(AuthenticatedUserContext.class),
                any(ChatbotContextualConversationCommand.class)
        )).thenReturn(ChatbotContextualConversationResult.builder()
                .conversationId("conversation-ctx")
                .engagementLetterId("EL-1")
                .createdAt("2026-05-18T10:00")
                .build());

        var response = this.chatbotResource.startContextualConversation(requestDto, this.authentication);

        assertThat(response.getConversationId()).isEqualTo("conversation-ctx");
        assertThat(response.getEngagementLetterId()).isEqualTo("EL-1");
        ArgumentCaptor<AuthenticatedUserContext> userCaptor =
                ArgumentCaptor.forClass(AuthenticatedUserContext.class);
        ArgumentCaptor<ChatbotContextualConversationCommand> commandCaptor =
                ArgumentCaptor.forClass(ChatbotContextualConversationCommand.class);
        verify(this.startContextualConversationUseCase)
                .startContextualConversation(userCaptor.capture(), commandCaptor.capture());
        assertThat(userCaptor.getValue()).isSameAs(this.authenticatedUser);
        assertThat(commandCaptor.getValue().getEngagementLetterId()).isEqualTo("EL-1");
    }

    @Test
    void startGeneralConversationShouldMapRequestAndCallPort() {
        this.mockAuthenticatedUser();
        ChatbotMessageRequestDto requestDto = new ChatbotMessageRequestDto(null, "Hola");
        when(this.startGeneralConversationUseCase.startGeneralConversation(
                any(AuthenticatedUserContext.class),
                any(ChatbotMessageCommand.class)
        )).thenReturn(messageResult("conversation-general", "Respuesta general"));

        var response = this.chatbotResource.startGeneralConversation(requestDto, this.authentication);

        assertThat(response.getConversationId()).isEqualTo("conversation-general");
        assertThat(response.getMessage()).isEqualTo("Respuesta general");
        ArgumentCaptor<AuthenticatedUserContext> userCaptor =
                ArgumentCaptor.forClass(AuthenticatedUserContext.class);
        ArgumentCaptor<ChatbotMessageCommand> commandCaptor = ArgumentCaptor.forClass(ChatbotMessageCommand.class);
        verify(this.startGeneralConversationUseCase)
                .startGeneralConversation(userCaptor.capture(), commandCaptor.capture());
        assertThat(userCaptor.getValue()).isSameAs(this.authenticatedUser);
        assertThat(commandCaptor.getValue().getConversationId()).isNull();
        assertThat(commandCaptor.getValue().getMessage()).isEqualTo("Hola");
    }

    @Test
    void sendMessageShouldMapRequestAndCallPort() {
        this.mockAuthenticatedUser();
        ChatbotMessageRequestDto requestDto = new ChatbotMessageRequestDto("conversation-1", "Siguiente mensaje");
        when(this.sendChatbotMessageUseCase.sendMessage(
                any(AuthenticatedUserContext.class),
                any(ChatbotMessageCommand.class)
        )).thenReturn(messageResult("conversation-1", "Respuesta"));

        var response = this.chatbotResource.sendMessage(requestDto, this.authentication);

        assertThat(response.getConversationId()).isEqualTo("conversation-1");
        assertThat(response.getResponseMode()).isEqualTo(ChatbotResponseMode.GENERAL.name());
        ArgumentCaptor<AuthenticatedUserContext> userCaptor =
                ArgumentCaptor.forClass(AuthenticatedUserContext.class);
        ArgumentCaptor<ChatbotMessageCommand> commandCaptor = ArgumentCaptor.forClass(ChatbotMessageCommand.class);
        verify(this.sendChatbotMessageUseCase).sendMessage(userCaptor.capture(), commandCaptor.capture());
        assertThat(userCaptor.getValue()).isSameAs(this.authenticatedUser);
        assertThat(commandCaptor.getValue().getConversationId()).isEqualTo("conversation-1");
        assertThat(commandCaptor.getValue().getMessage()).isEqualTo("Siguiente mensaje");
    }

    @Test
    void readConfigurationStatusShouldCallConfigurationPortWithoutAuthenticationResolver() {
        when(this.readChatbotConfigurationUseCase.readConfigurationStatus())
                .thenReturn(ChatbotConfigurationResult.builder()
                        .enabled(true)
                        .provider("ollama")
                        .model("llama3.2:3b")
                        .maxInputCharacters(1000)
                        .maxOutputTokens(500)
                        .maxContextMessages(10)
                        .documentsAvailable(false)
                        .build());

        var response = this.chatbotResource.readConfigurationStatus();

        assertThat(response.isEnabled()).isTrue();
        assertThat(response.getProvider()).isEqualTo("ollama");
        assertThat(response.getMaxInputCharacters()).isEqualTo(1000);
        verify(this.readChatbotConfigurationUseCase).readConfigurationStatus();
    }

    @Test
    void readConversationHistoryShouldResolveUserAndCallHistoryPort() {
        this.mockAuthenticatedUser();
        when(this.readConversationHistoryUseCase.readConversationHistory(
                this.authenticatedUser,
                "conversation-1",
                2,
                20
        )).thenReturn(ChatbotConversationHistoryResult.builder()
                .conversationId("conversation-1")
                .type("GENERAL")
                .status("ACTIVE")
                .page(2)
                .size(20)
                .hasMore(false)
                .totalMessages(1L)
                .messages(List.of(ChatbotHistoryMessageResult.builder()
                        .id("message-1")
                        .conversationId("conversation-1")
                        .content("Hola")
                        .build()))
                .build());

        var response = this.chatbotResource.readConversationHistory("conversation-1", 2, 20, this.authentication);

        assertThat(response.getConversationId()).isEqualTo("conversation-1");
        assertThat(response.getMessages()).hasSize(1);
        assertThat(response.getMessages().getFirst().getContent()).isEqualTo("Hola");
        verify(this.readConversationHistoryUseCase)
                .readConversationHistory(this.authenticatedUser, "conversation-1", 2, 20);
    }

    @Test
    void deleteConversationShouldResolveUserCallPortAndReturnNoContent() {
        this.mockAuthenticatedUser();
        ResponseEntity<Void> response = this.chatbotResource.deleteConversation("conversation-1", this.authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(this.deleteConversationUseCase).deleteConversation(this.authenticatedUser, "conversation-1");
    }

    @Test
    void closeConversationShouldResolveUserCallPortAndReturnNoContent() {
        this.mockAuthenticatedUser();
        ResponseEntity<Void> response = this.chatbotResource.closeConversation("conversation-1", this.authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(this.closeConversationUseCase).closeConversation(this.authenticatedUser, "conversation-1");
    }

    @Test
    void reopenConversationShouldResolveUserCallPortAndReturnNoContent() {
        this.mockAuthenticatedUser();
        ResponseEntity<Void> response = this.chatbotResource.reopenConversation("conversation-1", this.authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(this.reopenConversationUseCase).reopenConversation(this.authenticatedUser, "conversation-1");
    }

    @Test
    void escalateConversationShouldResolveUserCallPortAndReturnNoContent() {
        this.mockAuthenticatedUser();
        ResponseEntity<Void> response = this.chatbotResource.escalateConversation("conversation-1", this.authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(this.escalateConversationUseCase).escalateConversation(this.authenticatedUser, "conversation-1");
    }

    private void mockAuthenticatedUser() {
        when(this.authenticatedUserContextResolver.resolve(this.authentication)).thenReturn(this.authenticatedUser);
    }

    private static ChatbotMessageResult messageResult(String conversationId, String message) {
        return ChatbotMessageResult.builder()
                .conversationId(conversationId)
                .message(message)
                .createdAt("2026-05-18T10:00")
                .responseMode(ChatbotResponseMode.GENERAL)
                .usedPlatformData(false)
                .sourcesSummary(List.of())
                .build();
    }
}

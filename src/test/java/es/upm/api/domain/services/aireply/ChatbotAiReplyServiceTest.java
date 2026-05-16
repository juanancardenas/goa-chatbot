package es.upm.api.domain.services.aireply;

import es.upm.api.domain.enums.ConversationType;

import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.ai.ChatbotAiRequest;
import es.upm.api.domain.model.ai.ChatbotAiResponse;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.ports.out.ChatbotAiClient;
import es.upm.api.domain.ports.out.ChatbotAiSettings;
import es.upm.api.domain.services.conversation.ChatbotMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotAiReplyServiceTest {

    @Mock
    private ChatbotAiClient chatbotAiClient;

    @Mock
    private ChatbotAiSettings chatbotAiSettings;

    @Mock
    private ChatbotMessageService chatbotMessageService;

    private ChatbotAiReplyService chatbotAiReplyService;

    @BeforeEach
    void setUp() {
        lenient().when(this.chatbotAiSettings.basePrompt()).thenReturn("Prompt base de pruebas");
        lenient().when(this.chatbotAiSettings.model()).thenReturn("llama3.2:3b");
        lenient().when(this.chatbotAiSettings.maxOutputTokens()).thenReturn(500);
        lenient().when(this.chatbotAiSettings.maxContextMessages()).thenReturn(2);
        lenient().when(this.chatbotAiSettings.temperature()).thenReturn(0.2);
        lenient().when(this.chatbotAiSettings.documentsAvailable()).thenReturn(true);

        this.chatbotAiReplyService = new ChatbotAiReplyService(
                this.chatbotAiClient,
                this.chatbotAiSettings,
                this.chatbotMessageService
        );
    }

    @Test
    void generateConfiguredAssistantReplyShouldReturnBaseReplyWhenAiIsDisabled() {
        when(this.chatbotAiSettings.isEnabled()).thenReturn(false);

        String response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                this.generalConversation(),
                ConversationProfileType.PROFESSIONAL,
                "What can you do?",
                "Safe base reply",
                Optional.empty()
        );

        assertThat(response).isEqualTo("Safe base reply");
        verify(this.chatbotAiClient, never()).generate(any(ChatbotAiRequest.class));
        verify(this.chatbotMessageService, never()).readRecentMessagesForPrompt(any(), anyInt());
    }

    @Test
    void generateConfiguredAssistantReplyShouldBuildRequestWithContextAndRecentMessages() {
        Conversation conversation = Conversation.builder()
                .id("conversation-ai-context")
                .userId("customer-9")
                .engagementLetterId("EL-555")
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-555")
                .ownerDisplayName("Ana Ocana")
                .procedureTitles(List.of("Civil claim"))
                .legalTaskSummaries(List.of("Review documentation", "File pleading"))
                .recentEventSummaries(List.of("Pleading filed", "Hearing scheduled"))
                .sourcesSummary(List.of("Engagement letter", "Timeline"))
                .build();

        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-ai-context", 2))
                .thenReturn(List.of("ASSISTANT: Previous reply", "USER: Last question"));
        when(this.chatbotAiClient.generate(any(ChatbotAiRequest.class)))
                .thenReturn(ChatbotAiResponse.builder()
                        .content("  AI contextual reply  ")
                        .provider("ollama")
                        .model("llama3.2:3b")
                        .finishReason("SUCCESS")
                        .build());

        String response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.CLIENT,
                "Which legal tasks are pending?",
                "Safe contextual base reply",
                Optional.of(platformContext)
        );

        ArgumentCaptor<ChatbotAiRequest> requestCaptor = ArgumentCaptor.forClass(ChatbotAiRequest.class);
        verify(this.chatbotAiClient).generate(requestCaptor.capture());

        ChatbotAiRequest aiRequest = requestCaptor.getValue();
        assertThat(response).isEqualTo("AI contextual reply");
        assertThat(aiRequest.getConversationId()).isEqualTo("conversation-ai-context");
        assertThat(aiRequest.getUserId()).isEqualTo("customer-9");
        assertThat(aiRequest.getBasePrompt()).isEqualTo("Prompt base de pruebas");
        assertThat(aiRequest.getRoleProfile()).isEqualTo("CLIENT");
        assertThat(aiRequest.getConversationType()).isEqualTo("CONTEXTUAL");
        assertThat(aiRequest.getModel()).isEqualTo("llama3.2:3b");
        assertThat(aiRequest.getMaxOutputTokens()).isEqualTo(500);
        assertThat(aiRequest.getTemperature()).isEqualTo(0.2);
        assertThat(aiRequest.getDocumentsAvailable()).isTrue();
        assertThat(aiRequest.getRecentMessages()).containsExactly(
                "ASSISTANT: Previous reply",
                "USER: Last question"
        );
        assertThat(aiRequest.getPlatformContext()).contains("EngagementLetterId: EL-555");
        assertThat(aiRequest.getPlatformContext()).contains("Cliente/propietario visible: Ana Ocana");
        assertThat(aiRequest.getPlatformContext()).contains("Review documentation");
        assertThat(aiRequest.getPlatformContext()).contains("Pleading filed");
        assertThat(aiRequest.getPlatformContext()).contains("Engagement letter");
        assertThat(aiRequest.getUserMessage()).contains("Pregunta actual del usuario:");
        assertThat(aiRequest.getUserMessage()).contains("Which legal tasks are pending?");
        assertThat(aiRequest.getUserMessage()).contains("Safe contextual base reply");
        assertThat(aiRequest.getUserMessage()).contains("encargo activo: EL-555");
    }

    @Test
    void generateConfiguredAssistantReplyShouldUseConversationEngagementWhenNoPlatformContextExists() {
        Conversation conversation = Conversation.builder()
                .id("conversation-context-no-platform")
                .userId("customer-1")
                .engagementLetterId("EL-999")
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-context-no-platform", 2))
                .thenReturn(List.of());
        when(this.chatbotAiClient.generate(any(ChatbotAiRequest.class)))
                .thenReturn(ChatbotAiResponse.builder()
                        .content("AI reply without platform context")
                        .finishReason("SUCCESS")
                        .build());

        String response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.CLIENT,
                "Any update?",
                "Safe fallback base reply",
                Optional.empty()
        );

        ArgumentCaptor<ChatbotAiRequest> requestCaptor = ArgumentCaptor.forClass(ChatbotAiRequest.class);
        verify(this.chatbotAiClient).generate(requestCaptor.capture());

        assertThat(response).isEqualTo("AI reply without platform context");
        assertThat(requestCaptor.getValue().getPlatformContext())
                .isEqualTo("No hay contexto de plataforma disponible.");
        assertThat(requestCaptor.getValue().getUserMessage()).contains("encargo activo: EL-999");
    }

    @Test
    void generateConfiguredAssistantReplyShouldUseUnavailableFallbacksWhenPromptValuesAreMissing() {
        Conversation conversation = Conversation.builder()
                .id("conversation-context-missing-values")
                .userId("customer-2")
                .engagementLetterId(" ")
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();

        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-context-missing-values", 2))
                .thenReturn(List.of());
        when(this.chatbotAiClient.generate(any(ChatbotAiRequest.class)))
                .thenReturn(ChatbotAiResponse.builder()
                        .content("AI reply with unavailable fallbacks")
                        .finishReason("SUCCESS")
                        .build());

        String response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.CLIENT,
                null,
                " ",
                Optional.empty()
        );

        ArgumentCaptor<ChatbotAiRequest> requestCaptor = ArgumentCaptor.forClass(ChatbotAiRequest.class);
        verify(this.chatbotAiClient).generate(requestCaptor.capture());

        assertThat(response).isEqualTo("AI reply with unavailable fallbacks");
        assertThat(requestCaptor.getValue().getPlatformContext())
                .isEqualTo("No hay contexto de plataforma disponible.");
        assertThat(requestCaptor.getValue().getUserMessage())
                .contains("encargo activo: No disponible")
                .contains("Pregunta actual del usuario:")
                .contains("Respuesta base segura generada por GOA:")
                .contains("No disponible");
    }

    @Test
    void generateConfiguredAssistantReplyShouldBuildUnavailablePlatformContextWhenContextFieldsAreMissing() {
        Conversation conversation = this.generalConversation();
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId(" ")
                .ownerDisplayName(null)
                .procedureTitles(null)
                .legalTaskSummaries(List.of())
                .recentEventSummaries(null)
                .sourcesSummary(List.of())
                .build();

        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-general", 2))
                .thenReturn(List.of());
        when(this.chatbotAiClient.generate(any(ChatbotAiRequest.class)))
                .thenReturn(ChatbotAiResponse.builder()
                        .content("AI reply with platform fallbacks")
                        .finishReason("SUCCESS")
                        .build());

        String response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.of(platformContext)
        );

        ArgumentCaptor<ChatbotAiRequest> requestCaptor = ArgumentCaptor.forClass(ChatbotAiRequest.class);
        verify(this.chatbotAiClient).generate(requestCaptor.capture());

        assertThat(response).isEqualTo("AI reply with platform fallbacks");
        assertThat(requestCaptor.getValue().getPlatformContext())
                .contains("EngagementLetterId: No disponible")
                .contains("Cliente/propietario visible: No disponible")
                .contains("Procedimientos: No disponible")
                .contains("Tareas legales:")
                .contains("Eventos recientes:")
                .contains("Fuentes internas disponibles:")
                .contains("No disponible");
    }

    @Test
    void generateConfiguredAssistantReplyShouldReturnBaseReplyWhenAiResponseIsInvalid() {
        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-general", 2))
                .thenReturn(List.of());
        when(this.chatbotAiClient.generate(any(ChatbotAiRequest.class)))
                .thenReturn(null)
                .thenReturn(ChatbotAiResponse.builder().error("AI_PROVIDER_ERROR").build())
                .thenReturn(ChatbotAiResponse.builder().content(null).build())
                .thenReturn(ChatbotAiResponse.builder().content("   ").build());

        Conversation conversation = this.generalConversation();

        assertThat(this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        )).isEqualTo("Safe base reply");
        assertThat(this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        )).isEqualTo("Safe base reply");
        assertThat(this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        )).isEqualTo("Safe base reply");
        assertThat(this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        )).isEqualTo("Safe base reply");

        verify(this.chatbotAiClient, times(4)).generate(any(ChatbotAiRequest.class));
    }

    @Test
    void generateConfiguredAssistantReplyShouldReturnBaseReplyWhenAiClientThrowsException() {
        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-general", 2))
                .thenReturn(List.of());
        when(this.chatbotAiClient.generate(any(ChatbotAiRequest.class)))
                .thenThrow(new RuntimeException("provider unavailable"));

        String response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                this.generalConversation(),
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        );

        assertThat(response).isEqualTo("Safe base reply");
    }

    @Test
    void generateConfiguredAssistantReplyShouldReturnBaseReplyWhenRecentMessagesCannotBeRead() {
        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-general", 2))
                .thenThrow(new RuntimeException("history unavailable"));

        String response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                this.generalConversation(),
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        );

        assertThat(response).isEqualTo("Safe base reply");
        verify(this.chatbotAiClient, never()).generate(any(ChatbotAiRequest.class));
    }

    private Conversation generalConversation() {
        return Conversation.builder()
                .id("conversation-general")
                .userId("professional-1")
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();
    }
}

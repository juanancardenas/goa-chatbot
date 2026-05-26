package es.upm.api.domain.services.reply.ai.prompt;

import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.ai.ChatbotAiRequest;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.ports.out.ChatbotAiSettings;
import es.upm.api.domain.services.conversation.ChatbotMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotAiRequestBuilderTest {

    @Mock
    private ChatbotAiSettings chatbotAiSettings;

    @Mock
    private ChatbotMessageService chatbotMessageService;

    private ChatbotAiRequestBuilder chatbotAiRequestBuilder;

    @BeforeEach
    void setUp() {
        lenient().when(this.chatbotAiSettings.basePrompt()).thenReturn("Prompt base de pruebas");
        lenient().when(this.chatbotAiSettings.model()).thenReturn("llama3.2:3b");
        lenient().when(this.chatbotAiSettings.maxOutputTokens()).thenReturn(500);
        lenient().when(this.chatbotAiSettings.maxContextMessages()).thenReturn(2);
        lenient().when(this.chatbotAiSettings.temperature()).thenReturn(0.2);
        lenient().when(this.chatbotAiSettings.documentsAvailable()).thenReturn(true);

        this.chatbotAiRequestBuilder = new ChatbotAiRequestBuilder(
                this.chatbotAiSettings,
                this.chatbotMessageService
        );
    }

    @Test
    void buildShouldCreateContextualRequestWithPlatformContextAndRecentMessages() {
        Conversation conversation = this.contextualConversation("conversation-ai-context", "EL-555");
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-555")
                .ownerDisplayName("Ana Ocana")
                .procedureTitles(List.of("Civil claim"))
                .legalTaskSummaries(List.of("Review documentation", "File pleading"))
                .recentEventSummaries(List.of("Pleading filed", "Hearing scheduled"))
                .sourcesSummary(List.of("Engagement letter", "Timeline"))
                .build();

        when(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-ai-context", 2))
                .thenReturn(List.of("ASSISTANT: Previous reply", "USER: Last question"));

        ChatbotAiRequest aiRequest = this.chatbotAiRequestBuilder.build(
                conversation,
                ConversationProfileType.CLIENT,
                "Which legal tasks are pending?",
                "Safe contextual base reply",
                Optional.of(platformContext)
        );

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
        assertThat(aiRequest.getPlatformContext())
                .contains("EngagementLetterId: EL-555")
                .contains("Cliente/propietario visible: Ana Ocana")
                .contains("Civil claim")
                .contains("Review documentation")
                .contains("Pleading filed")
                .contains("Engagement letter");
        assertThat(aiRequest.getUserMessage())
                .contains("Pregunta actual del usuario:")
                .contains("Which legal tasks are pending?")
                .contains("Safe contextual base reply")
                .contains("encargo activo: EL-555");
        verify(this.chatbotMessageService).readRecentMessagesForPrompt("conversation-ai-context", 2);
    }

    @Test
    void buildShouldUseConversationEngagementWhenNoPlatformContextExists() {
        Conversation conversation = this.contextualConversation("conversation-context-no-platform", "EL-999");
        when(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-context-no-platform", 2))
                .thenReturn(List.of());

        ChatbotAiRequest aiRequest = this.chatbotAiRequestBuilder.build(
                conversation,
                ConversationProfileType.CLIENT,
                "Any update?",
                "Safe fallback base reply",
                Optional.empty()
        );

        assertThat(aiRequest.getPlatformContext()).isEqualTo("No hay contexto de plataforma disponible.");
        assertThat(aiRequest.getUserMessage()).contains("encargo activo: EL-999");
    }

    @Test
    void buildShouldUsePlatformEngagementAsActiveEngagementWhenContextExists() {
        Conversation conversation = this.contextualConversation("conversation-context-platform-id", "EL-CONVERSATION");
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-PLATFORM")
                .ownerDisplayName("Ana Ocana")
                .build();
        when(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-context-platform-id", 2))
                .thenReturn(List.of());

        ChatbotAiRequest aiRequest = this.chatbotAiRequestBuilder.build(
                conversation,
                ConversationProfileType.CLIENT,
                "Any update?",
                "Safe base reply",
                Optional.of(platformContext)
        );

        assertThat(aiRequest.getUserMessage())
                .contains("encargo activo: EL-PLATFORM")
                .doesNotContain("encargo activo: EL-CONVERSATION");
        assertThat(aiRequest.getPlatformContext()).contains("EngagementLetterId: EL-PLATFORM");
    }

    @Test
    void buildShouldUseUnavailableFallbacksWhenPromptValuesAreMissing() {
        Conversation conversation = this.contextualConversation("conversation-context-missing-values", " ");
        when(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-context-missing-values", 2))
                .thenReturn(List.of());

        ChatbotAiRequest aiRequest = this.chatbotAiRequestBuilder.build(
                conversation,
                ConversationProfileType.CLIENT,
                null,
                " ",
                Optional.empty()
        );

        assertThat(aiRequest.getPlatformContext()).isEqualTo("No hay contexto de plataforma disponible.");
        assertThat(aiRequest.getUserMessage())
                .contains("encargo activo: No disponible")
                .contains("Pregunta actual del usuario:")
                .contains("Respuesta base segura generada por GOA:")
                .contains("No disponible");
    }

    @Test
    void buildShouldTrimPromptValuesBeforeEmbeddingThemInUserMessage() {
        Conversation conversation = this.contextualConversation("conversation-context-trim", "  EL-TRIM  ");
        when(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-context-trim", 2))
                .thenReturn(List.of());

        ChatbotAiRequest aiRequest = this.chatbotAiRequestBuilder.build(
                conversation,
                ConversationProfileType.CLIENT,
                "  Question with spaces  ",
                "  Safe base reply with spaces  ",
                Optional.empty()
        );

        assertThat(aiRequest.getUserMessage())
                .contains("Question with spaces")
                .contains("Safe base reply with spaces")
                .contains("encargo activo: EL-TRIM")
                .doesNotContain("  Question with spaces  ")
                .doesNotContain("  Safe base reply with spaces  ")
                .doesNotContain("encargo activo:   EL-TRIM  ");
    }

    @Test
    void buildShouldBuildUnavailablePlatformContextWhenContextFieldsAreMissing() {
        Conversation conversation = this.generalConversation();
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId(" ")
                .ownerDisplayName(null)
                .procedureTitles(null)
                .legalTaskSummaries(List.of())
                .recentEventSummaries(null)
                .sourcesSummary(List.of())
                .build();
        when(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-general", 2))
                .thenReturn(List.of());

        ChatbotAiRequest aiRequest = this.chatbotAiRequestBuilder.build(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.of(platformContext)
        );

        assertThat(aiRequest.getPlatformContext())
                .contains("EngagementLetterId: No disponible")
                .contains("Cliente/propietario visible: No disponible")
                .contains("Procedimientos: No disponible")
                .contains("Tareas legales:")
                .contains("Eventos recientes:")
                .contains("Fuentes internas disponibles:")
                .contains("No disponible");
    }

    @Test
    void buildShouldTrimScalarPlatformContextFields() {
        Conversation conversation = this.generalConversation();
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId("  EL-777  ")
                .ownerDisplayName("  Maria Lopez  ")
                .procedureTitles(List.of("Procedure A"))
                .legalTaskSummaries(List.of("Task A"))
                .recentEventSummaries(List.of("Event A"))
                .sourcesSummary(List.of("Source A"))
                .build();
        when(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-general", 2))
                .thenReturn(List.of());

        ChatbotAiRequest aiRequest = this.chatbotAiRequestBuilder.build(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.of(platformContext)
        );

        assertThat(aiRequest.getPlatformContext())
                .contains("EngagementLetterId: EL-777")
                .contains("Cliente/propietario visible: Maria Lopez")
                .doesNotContain("  EL-777  ")
                .doesNotContain("  Maria Lopez  ");
    }

    @Test
    void buildShouldNotAddContextualRulesForGeneralConversation() {
        when(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-general", 2))
                .thenReturn(List.of());

        ChatbotAiRequest aiRequest = this.chatbotAiRequestBuilder.build(
                this.generalConversation(),
                ConversationProfileType.PROFESSIONAL,
                "General question",
                "Safe base reply",
                Optional.empty()
        );

        assertThat(aiRequest.getUserMessage())
                .contains("Pregunta actual del usuario:")
                .doesNotContain("Reglas adicionales para chat contextual")
                .doesNotContain("encargo activo:");
    }

    @Test
    void buildShouldCopyRuntimeAiSettingsIntoRequest() {
        when(this.chatbotAiSettings.basePrompt()).thenReturn("Prompt runtime");
        when(this.chatbotAiSettings.model()).thenReturn("runtime-model");
        when(this.chatbotAiSettings.maxOutputTokens()).thenReturn(1200);
        when(this.chatbotAiSettings.maxContextMessages()).thenReturn(4);
        when(this.chatbotAiSettings.temperature()).thenReturn(0.7);
        when(this.chatbotAiSettings.documentsAvailable()).thenReturn(false);
        when(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-general", 4))
                .thenReturn(List.of("USER: Runtime context"));

        ChatbotAiRequest aiRequest = this.chatbotAiRequestBuilder.build(
                this.generalConversation(),
                ConversationProfileType.PROFESSIONAL,
                "General question",
                "Safe base reply",
                Optional.empty()
        );

        assertThat(aiRequest.getBasePrompt()).isEqualTo("Prompt runtime");
        assertThat(aiRequest.getModel()).isEqualTo("runtime-model");
        assertThat(aiRequest.getMaxOutputTokens()).isEqualTo(1200);
        assertThat(aiRequest.getTemperature()).isEqualTo(0.7);
        assertThat(aiRequest.getDocumentsAvailable()).isFalse();
        assertThat(aiRequest.getRecentMessages()).containsExactly("USER: Runtime context");
        verify(this.chatbotMessageService).readRecentMessagesForPrompt("conversation-general", 4);
    }

    private Conversation generalConversation() {
        return Conversation.builder()
                .id("conversation-general")
                .userId("professional-1")
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();
    }

    private Conversation contextualConversation(String conversationId, String engagementLetterId) {
        return Conversation.builder()
                .id(conversationId)
                .userId("customer-9")
                .engagementLetterId(engagementLetterId)
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();
    }
}

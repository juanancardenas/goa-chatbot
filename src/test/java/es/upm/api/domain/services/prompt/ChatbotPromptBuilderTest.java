package es.upm.api.domain.services.prompt;

import es.upm.api.domain.model.ai.ChatbotAiRequest;
import es.upm.api.domain.ports.out.ChatbotAiSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotPromptBuilderTest {
    private ChatbotPromptBuilder chatbotPromptBuilder;

    @BeforeEach
    void setUp() {
        this.chatbotPromptBuilder = new ChatbotPromptBuilder(new TestChatbotAiSettings());
    }

    @Test
    void shouldBuildPromptForGeneralConversation() {
        ChatbotAiRequest request = ChatbotAiRequest.builder()
                .basePrompt("Prompt personalizado")
                .roleProfile("PROFESSIONAL")
                .conversationType("GENERAL")
                .documentsAvailable(false)
                .platformContext("No hay contexto de plataforma disponible.")
                .recentMessages(List.of())
                .build();

        String prompt = this.chatbotPromptBuilder.buildSystemPrompt(request);

        assertThat(prompt).contains("Prompt personalizado");
        assertThat(prompt).contains("[TIPO DE CONVERSACIÓN]");
        assertThat(prompt).contains("Conversación general");
        assertThat(prompt).contains("apoyo conversacional jurídico seguro");
        assertThat(prompt).contains("No sustituyes a un abogado real");
        assertThat(prompt).contains("Rol conversacional: PROFESSIONAL");
    }

    @Test
    void shouldBuildPromptForContextualConversation() {
        ChatbotAiRequest request = ChatbotAiRequest.builder()
                .basePrompt("Prompt contextual")
                .roleProfile("CLIENT")
                .conversationType("CONTEXTUAL")
                .documentsAvailable(false)
                .platformContext("EngagementLetterId: engagement-123")
                .recentMessages(List.of())
                .build();

        String prompt = this.chatbotPromptBuilder.buildSystemPrompt(request);

        assertThat(prompt).contains("Prompt contextual");
        assertThat(prompt).contains("Conversación contextual");
        assertThat(prompt).contains("Usa el contexto del encargo asociado");
        assertThat(prompt).contains("No inventes tareas legales");
        assertThat(prompt).contains("No respondas con datos de otros encargos");
        assertThat(prompt).contains("Cierra cada respuesta con una pregunta breve de seguimiento");
        assertThat(prompt).contains("EngagementLetterId: engagement-123");
        assertThat(prompt).contains("Rol conversacional: CLIENT");
    }

    @Test
    void shouldGuideAiToUseLegalTasksFromPlatformContext() {
        ChatbotAiRequest request = ChatbotAiRequest.builder()
                .basePrompt("Prompt base de pruebas")
                .roleProfile("PROFESSIONAL")
                .conversationType("CONTEXTUAL")
                .documentsAvailable(false)
                .platformContext("""
                    EngagementLetterId: engagement-001
                    Procedimientos: Procedimiento de herencia

                    Tareas Legales:
                    Procedimiento de herencia: Estudio de antecedentes y documentación.
                    Procedimiento de herencia: Asesoramiento jurídico.
                    """)
                .recentMessages(List.of())
                .build();

        String prompt = this.chatbotPromptBuilder.buildSystemPrompt(request);

        assertThat(prompt).contains("Si existe una sección \"Legal Tasks\"");
        assertThat(prompt).contains("úsala como fuente principal");
        assertThat(prompt).contains("Procedimiento de herencia: Estudio de antecedentes y documentación.");
        assertThat(prompt).contains("Procedimiento de herencia: Asesoramiento jurídico.");
    }

    @Test
    void shouldIncludeScopeRules() {
        ChatbotAiRequest request = ChatbotAiRequest.builder()
                .conversationType("GENERAL")
                .roleProfile("PROFESSIONAL")
                .documentsAvailable(false)
                .platformContext("No disponible")
                .recentMessages(List.of())
                .build();

        String prompt = this.chatbotPromptBuilder.buildSystemPrompt(request);

        assertThat(prompt).contains("[RESTRICCIÓN DE ÁMBITO]");
        assertThat(prompt).contains("No proporciones asesoramiento legal vinculante");
        assertThat(prompt).contains("No inventes información");
    }

    @Test
    void shouldIncludeDocumentsUnavailableRules() {
        ChatbotAiRequest request = ChatbotAiRequest.builder()
                .conversationType("CONTEXTUAL")
                .roleProfile("CLIENT")
                .documentsAvailable(false)
                .platformContext("Contexto mínimo")
                .recentMessages(List.of())
                .build();

        String prompt = this.chatbotPromptBuilder.buildSystemPrompt(request);

        assertThat(prompt).contains("El servicio documental no está disponible actualmente");
        assertThat(prompt).contains("No afirmes haber leído documentos reales");
        assertThat(prompt).contains("No inventes contenido documental");
    }

    @Test
    void shouldIncludeDocumentsAvailableRules() {
        ChatbotAiRequest request = ChatbotAiRequest.builder()
                .conversationType("CONTEXTUAL")
                .roleProfile("PROFESSIONAL")
                .documentsAvailable(true)
                .platformContext("Contexto mínimo")
                .recentMessages(List.of())
                .build();

        String prompt = this.chatbotPromptBuilder.buildSystemPrompt(request);

        assertThat(prompt).contains("El servicio documental está disponible");
        assertThat(prompt).contains("Usa únicamente documentos autorizados");
    }

    @Test
    void shouldLimitRecentMessagesUsingConfiguredLimit() {
        ChatbotAiRequest request = ChatbotAiRequest.builder()
                .conversationType("GENERAL")
                .roleProfile("PROFESSIONAL")
                .documentsAvailable(false)
                .platformContext("Contexto mínimo")
                .recentMessages(List.of(
                        "USER: primer mensaje",
                        "ASSISTANT: primera respuesta",
                        "USER: último mensaje"
                ))
                .build();

        String prompt = this.chatbotPromptBuilder.buildSystemPrompt(request);

        assertThat(prompt).doesNotContain("USER: primer mensaje");
        assertThat(prompt).contains("ASSISTANT: primera respuesta");
        assertThat(prompt).contains("USER: último mensaje");
    }

    @Test
    void shouldNotInventPlatformDataWhenContextIsMissing() {
        ChatbotAiRequest request = ChatbotAiRequest.builder()
                .conversationType("CONTEXTUAL")
                .roleProfile("CLIENT")
                .documentsAvailable(false)
                .platformContext(" ")
                .recentMessages(List.of())
                .build();

        String prompt = this.chatbotPromptBuilder.buildSystemPrompt(request);

        assertThat(prompt).contains("[CONTEXTO DE PLATAFORMA]");
        assertThat(prompt).contains("No disponible");
        assertThat(prompt).contains("no inventes datos de plataforma");
    }

    @Test
    void shouldUseSettingsBasePromptDefaultConversationTypeAndRoleFallbacks() {
        ChatbotAiRequest request = ChatbotAiRequest.builder()
                .basePrompt(" ")
                .conversationType(null)
                .roleProfile(" ")
                .documentsAvailable(null)
                .platformContext(null)
                .recentMessages(null)
                .build();

        String prompt = this.chatbotPromptBuilder.buildSystemPrompt(request);

        assertThat(prompt).contains("Prompt base de pruebas para GOA.");
        assertThat(prompt).contains("ConversaciÃ³n general");
        assertThat(prompt).contains("Rol conversacional: No disponible");
        assertThat(prompt).contains("El servicio documental no estÃ¡ disponible actualmente");
        assertThat(prompt).contains("[HISTORIAL RECIENTE]");
        assertThat(prompt).contains("No hay mensajes recientes disponibles");
    }

    @Test
    void shouldTreatUnknownConversationTypeAsGeneralConversation() {
        ChatbotAiRequest request = ChatbotAiRequest.builder()
                .basePrompt("Prompt base")
                .conversationType("UNKNOWN")
                .roleProfile("CLIENT")
                .documentsAvailable(false)
                .platformContext("No disponible")
                .recentMessages(List.of())
                .build();

        String prompt = this.chatbotPromptBuilder.buildSystemPrompt(request);

        assertThat(prompt).contains("ConversaciÃ³n general");
        assertThat(prompt).doesNotContain("ConversaciÃ³n contextual");
    }

    private static class TestChatbotAiSettings implements ChatbotAiSettings {

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public String provider() {
            return "ollama";
        }

        @Override
        public String model() {
            return "llama3.2:3b";
        }

        @Override
        public int maxInputCharacters() {
            return 1000;
        }

        @Override
        public int maxOutputTokens() {
            return 500;
        }

        @Override
        public int maxContextMessages() {
            return 2;
        }

        @Override
        public boolean documentsAvailable() {
            return false;
        }

        @Override
        public String basePrompt() {
            return "Prompt base de pruebas para GOA.";
        }

        @Override
        public double temperature() {
            return 0.2;
        }
    }
}

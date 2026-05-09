package es.upm.api.domain.services.ai;

import es.upm.api.configurations.ChatbotAiProperties;
import es.upm.api.domain.model.ai.ChatbotAiRequest;
import es.upm.api.domain.model.ai.ChatbotAiResponse;
import es.upm.api.domain.services.prompt.ChatbotPromptBuilder;
import es.upm.api.infrastructure.ai.SpringAiChatbotClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringAiChatbotClientTest {
    private static final String AI_UNAVAILABLE_REPLY =
            "Ahora mismo no puedo generar una respuesta con IA. Inténtalo de nuevo más tarde.";

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec chatClientRequestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private ChatbotAiProperties chatbotAiProperties;

    @Mock
    private ChatbotPromptBuilder chatbotPromptTemplate;

    private SpringAiChatbotClient springAiChatbotClient;

    @BeforeEach
    void setUp() {
        when(this.chatClientBuilder.build()).thenReturn(this.chatClient);
        when(this.chatbotAiProperties.normalizedProvider()).thenReturn("ollama");
        when(this.chatbotAiProperties.getModel()).thenReturn("llama3.2:3b");

        this.springAiChatbotClient = new SpringAiChatbotClient(
                this.chatClientBuilder,
                this.chatbotAiProperties,
                this.chatbotPromptTemplate
        );
    }

    @Test
    void generateShouldReturnDisabledResponseWhenAiIsDisabled() {
        when(this.chatbotAiProperties.isEnabled()).thenReturn(false);

        ChatbotAiResponse response = this.springAiChatbotClient.generate(ChatbotAiRequest.builder().build());

        assertThat(response.getContent()).isEqualTo(AI_UNAVAILABLE_REPLY);
        assertThat(response.getProvider()).isEqualTo("ollama");
        assertThat(response.getModel()).isEqualTo("llama3.2:3b");
        assertThat(response.getFinishReason()).isEqualTo("ERROR");
        assertThat(response.getError()).isEqualTo("AI_PROVIDER_ERROR");
        verify(this.chatClient, never()).prompt();
        verify(this.chatbotPromptTemplate, never()).buildSystemPrompt(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void generateShouldReturnSuccessfulResponseWhenProviderReturnsContent() {
        ChatbotAiRequest request = ChatbotAiRequest.builder()
                .userMessage("Necesito ayuda")
                .build();

        when(this.chatbotAiProperties.isEnabled()).thenReturn(true);
        when(this.chatbotPromptTemplate.buildSystemPrompt(request)).thenReturn("prompt del sistema");
        when(this.chatClient.prompt()).thenReturn(this.chatClientRequestSpec);
        when(this.chatClientRequestSpec.system("prompt del sistema")).thenReturn(this.chatClientRequestSpec);
        when(this.chatClientRequestSpec.user("Necesito ayuda")).thenReturn(this.chatClientRequestSpec);
        when(this.chatClientRequestSpec.call()).thenReturn(this.callResponseSpec);
        when(this.callResponseSpec.content()).thenReturn("  Respuesta IA  ");

        ChatbotAiResponse response = this.springAiChatbotClient.generate(request);

        assertThat(response.getContent()).isEqualTo("Respuesta IA");
        assertThat(response.getProvider()).isEqualTo("ollama");
        assertThat(response.getModel()).isEqualTo("llama3.2:3b");
        assertThat(response.getFinishReason()).isEqualTo("SUCCESS");
        assertThat(response.getError()).isNull();
    }

    @Test
    void generateShouldUseFallbackContentWhenProviderReturnsBlankText() {
        ChatbotAiRequest request = ChatbotAiRequest.builder()
                .userMessage("Necesito ayuda")
                .build();

        when(this.chatbotAiProperties.isEnabled()).thenReturn(true);
        when(this.chatbotPromptTemplate.buildSystemPrompt(request)).thenReturn("prompt del sistema");
        when(this.chatClient.prompt()).thenReturn(this.chatClientRequestSpec);
        when(this.chatClientRequestSpec.system("prompt del sistema")).thenReturn(this.chatClientRequestSpec);
        when(this.chatClientRequestSpec.user("Necesito ayuda")).thenReturn(this.chatClientRequestSpec);
        when(this.chatClientRequestSpec.call()).thenReturn(this.callResponseSpec);
        when(this.callResponseSpec.content()).thenReturn("   ");

        ChatbotAiResponse response = this.springAiChatbotClient.generate(request);

        assertThat(response.getContent()).isEqualTo(AI_UNAVAILABLE_REPLY);
        assertThat(response.getFinishReason()).isEqualTo("SUCCESS");
        assertThat(response.getError()).isNull();
    }

    @Test
    void generateShouldReturnErrorResponseWhenProviderThrowsException() {
        ChatbotAiRequest request = ChatbotAiRequest.builder()
                .userMessage("Necesito ayuda")
                .build();

        when(this.chatbotAiProperties.isEnabled()).thenReturn(true);
        when(this.chatbotPromptTemplate.buildSystemPrompt(request)).thenReturn("prompt del sistema");
        when(this.chatClient.prompt()).thenReturn(this.chatClientRequestSpec);
        when(this.chatClientRequestSpec.system("prompt del sistema")).thenReturn(this.chatClientRequestSpec);
        when(this.chatClientRequestSpec.user("Necesito ayuda")).thenReturn(this.chatClientRequestSpec);
        when(this.chatClientRequestSpec.call()).thenThrow(new RuntimeException("provider down"));

        ChatbotAiResponse response = this.springAiChatbotClient.generate(request);

        assertThat(response.getContent()).isEqualTo(AI_UNAVAILABLE_REPLY);
        assertThat(response.getProvider()).isEqualTo("ollama");
        assertThat(response.getModel()).isEqualTo("llama3.2:3b");
        assertThat(response.getFinishReason()).isEqualTo("ERROR");
        assertThat(response.getError()).isEqualTo("AI_PROVIDER_ERROR");
    }
}

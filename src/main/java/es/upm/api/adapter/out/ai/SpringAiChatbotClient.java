package es.upm.api.adapter.out.ai;

import es.upm.api.configuration.ChatbotAiProperties;
import es.upm.api.domain.model.ai.ChatbotAiRequest;
import es.upm.api.domain.model.ai.ChatbotAiResponse;
import es.upm.api.domain.ports.out.ChatbotAiClient;
import es.upm.api.domain.services.prompt.ChatbotPromptBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SpringAiChatbotClient implements ChatbotAiClient {
    private static final String FINISH_REASON_SUCCESS = "SUCCESS";
    private static final String FINISH_REASON_ERROR = "ERROR";
    private static final String AI_PROVIDER_ERROR = "AI_PROVIDER_ERROR";
    private static final String AI_UNAVAILABLE_REPLY = """
            Ahora mismo no puedo generar una respuesta con IA. Inténtalo de nuevo más tarde.
            """;

    private final ChatClient chatClient;
    private final ChatbotAiProperties chatbotAiProperties;
    private final ChatbotPromptBuilder chatbotPromptTemplate;

    public SpringAiChatbotClient(
            ChatClient.Builder chatClientBuilder,
            ChatbotAiProperties chatbotAiProperties,
            ChatbotPromptBuilder chatbotPromptTemplate
    ) {
        this.chatClient = chatClientBuilder.build();
        this.chatbotAiProperties = chatbotAiProperties;
        this.chatbotPromptTemplate = chatbotPromptTemplate;
    }

    @Override
    public ChatbotAiResponse generate(ChatbotAiRequest request) {
        if (!this.chatbotAiProperties.isEnabled()) {
            return this.buildDisabledResponse();
        }

        try {
            String systemPrompt = this.chatbotPromptTemplate.buildSystemPrompt(request);

            String content = this.chatClient.prompt()
                    .system(systemPrompt)
                    .user(request.getUserMessage())
                    .call()
                    .content();

            return ChatbotAiResponse.builder()
                    .content(this.safeContent(content))
                    .provider(this.chatbotAiProperties.normalizedProvider())
                    .model(this.chatbotAiProperties.getModel())
                    .finishReason(FINISH_REASON_SUCCESS)
                    .build();

        } catch (Exception exception) {
            log.warn(
                    "AI provider error while generating chatbot response. provider={}, model={}, error={}: {}",
                    this.chatbotAiProperties.normalizedProvider(),
                    this.chatbotAiProperties.getModel(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
            return this.buildErrorResponse();
        }
    }

    private ChatbotAiResponse buildDisabledResponse() {
        return ChatbotAiResponse.builder()
                .content(AI_UNAVAILABLE_REPLY.trim())
                .provider(this.chatbotAiProperties.normalizedProvider())
                .model(this.chatbotAiProperties.getModel())
                .finishReason(FINISH_REASON_ERROR)
                .error(AI_PROVIDER_ERROR)
                .build();
    }

    private ChatbotAiResponse buildErrorResponse() {
        return ChatbotAiResponse.builder()
                .content(AI_UNAVAILABLE_REPLY.trim())
                .provider(this.chatbotAiProperties.normalizedProvider())
                .model(this.chatbotAiProperties.getModel())
                .finishReason(FINISH_REASON_ERROR)
                .error(AI_PROVIDER_ERROR)
                .build();
    }

    private String safeContent(String content) {
        if (content == null || content.isBlank()) {
            return AI_UNAVAILABLE_REPLY.trim();
        }

        return content.trim();
    }
}
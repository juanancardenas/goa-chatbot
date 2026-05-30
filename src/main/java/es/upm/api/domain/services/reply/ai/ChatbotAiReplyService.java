package es.upm.api.domain.services.reply.ai;

import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.ai.ChatbotAiRequest;
import es.upm.api.domain.model.ai.ChatbotAiResponse;
import es.upm.api.domain.model.chatbot.reply.ChatbotAiReplyResult;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.ports.out.ChatbotAiClient;
import es.upm.api.domain.ports.out.ChatbotAiSettings;
import es.upm.api.domain.services.reply.ai.prompt.ChatbotAiRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ChatbotAiReplyService {

    private final ChatbotAiClient chatbotAiClient;
    private final ChatbotAiSettings chatbotAiSettings;
    private final ChatbotAiRequestBuilder chatbotAiRequestBuilder;
    private static final Logger log = LoggerFactory.getLogger(ChatbotAiReplyService.class);

    public ChatbotAiReplyService(
            ChatbotAiClient chatbotAiClient,
            ChatbotAiSettings chatbotAiSettings,
            ChatbotAiRequestBuilder chatbotAiRequestBuilder
    ) {
        this.chatbotAiClient = chatbotAiClient;
        this.chatbotAiSettings = chatbotAiSettings;
        this.chatbotAiRequestBuilder = chatbotAiRequestBuilder;
    }

    public ChatbotAiReplyResult generateConfiguredAssistantReply(
            Conversation conversation,
            ConversationProfileType profile,
            String userMessage,
            String baseReply,
            Optional<ChatbotPlatformContext> platformContext
    ) {
        if (!this.chatbotAiSettings.isEnabled()) {
            return ChatbotAiReplyResult.withoutAi(baseReply);
        }

        try {
            ChatbotAiRequest aiRequest = this.chatbotAiRequestBuilder.build(
                    conversation,
                    profile,
                    userMessage,
                    baseReply,
                    platformContext
            );

            ChatbotAiResponse aiResponse = this.chatbotAiClient.generate(aiRequest);

            if (aiResponse == null) {
                log.warn(
                        "AI reply generation returned null response. conversationId={}",
                        conversation.getId()
                );

                return ChatbotAiReplyResult.withoutAi(baseReply);
            }

            if (aiResponse.getError() != null) {
                log.warn(
                        "AI reply generation returned error. conversationId={}, error={}",
                        conversation.getId(),
                        aiResponse.getError()
                );

                return ChatbotAiReplyResult.withoutAi(baseReply);
            }

            if (aiResponse.getContent() == null || aiResponse.getContent().isBlank()) {
                log.warn(
                        "AI reply generation returned blank content. conversationId={}",
                        conversation.getId()
                );

                return ChatbotAiReplyResult.withoutAi(baseReply);
            }

            return ChatbotAiReplyResult.withAi(aiResponse.getContent().trim());

        } catch (RuntimeException exception) {
            log.warn(
                    "AI reply generation failed. conversationId={}, aiEnabled={}, reason={}",
                    conversation.getId(),
                    this.chatbotAiSettings.isEnabled(),
                    exception.getMessage()
            );

            return ChatbotAiReplyResult.withoutAi(baseReply);
        }
    }
}

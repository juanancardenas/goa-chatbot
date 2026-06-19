package es.upm.api.domain.services.reply.ai;

import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.ai.ChatbotAiRequest;
import es.upm.api.domain.model.ai.ChatbotAiResponse;
import es.upm.api.domain.model.chatbot.reply.ChatbotAiReplyResult;
import es.upm.api.domain.model.metrics.ChatbotAiMetric;
import es.upm.api.domain.model.metrics.ChatbotFallbackMetric;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.ports.out.ChatbotAiClient;
import es.upm.api.domain.ports.out.ChatbotAiSettings;
import es.upm.api.domain.ports.out.ChatbotMetricsRecorder;
import es.upm.api.domain.services.reply.ai.prompt.ChatbotAiRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.Optional;

@Service
public class ChatbotAiReplyService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotAiReplyService.class);

    private final ChatbotAiClient chatbotAiClient;
    private final ChatbotAiSettings chatbotAiSettings;
    private final ChatbotAiRequestBuilder chatbotAiRequestBuilder;
    private final ChatbotMetricsRecorder chatbotMetricsRecorder;
    private final Clock clock;

    public ChatbotAiReplyService(
            ChatbotAiClient chatbotAiClient,
            ChatbotAiSettings chatbotAiSettings,
            ChatbotAiRequestBuilder chatbotAiRequestBuilder,
            ChatbotMetricsRecorder chatbotMetricsRecorder,
            Clock clock
    ) {
        this.chatbotAiClient = chatbotAiClient;
        this.chatbotAiSettings = chatbotAiSettings;
        this.chatbotAiRequestBuilder = chatbotAiRequestBuilder;
        this.chatbotMetricsRecorder = chatbotMetricsRecorder;
        this.clock = clock;
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

        long startTime = this.clock.millis();
        String conversationId = conversation.getId();
        String provider = this.chatbotAiSettings.provider();
        String model = this.chatbotAiSettings.model();

        try {
            ChatbotAiRequest aiRequest = this.chatbotAiRequestBuilder.build(
                    conversation,
                    profile,
                    userMessage,
                    baseReply,
                    platformContext
            );

            ChatbotAiResponse aiResponse = this.generateWithTimeout(aiRequest);
            long durationMs = this.clock.millis() - startTime;

            if (aiResponse == null) {
                log.warn(
                        "AI reply generation returned null response. conversationId={}",
                        conversationId
                );

                return this.fallbackReply(
                        conversationId,
                        provider,
                        model,
                        durationMs,
                        baseReply,
                        "NULL_AI_RESPONSE",
                        "AI_NULL_RESPONSE"
                );
            }

            if (aiResponse.getError() != null) {
                String errorType = this.safeErrorType(aiResponse.getError());
                log.warn(
                        "AI reply generation returned error. conversationId={}, errorType={}",
                        conversationId,
                        errorType
                );

                return this.fallbackReply(
                        conversationId,
                        provider,
                        model,
                        durationMs,
                        baseReply,
                        errorType,
                        "AI_RESPONSE_ERROR"
                );
            }

            if (aiResponse.getContent() == null || aiResponse.getContent().isBlank()) {
                log.warn(
                        "AI reply generation returned blank content. conversationId={}",
                        conversationId
                );

                return this.fallbackReply(
                        conversationId,
                        provider,
                        model,
                        durationMs,
                        baseReply,
                        "EMPTY_AI_RESPONSE",
                        "AI_EMPTY_RESPONSE"
                );
            }

            this.recordAiMetricSafely(
                    this.aiMetric(conversationId, provider, model, durationMs, true, false, null)
            );

            return ChatbotAiReplyResult.withAi(aiResponse.getContent().trim());

        } catch (RuntimeException exception) {
            long durationMs = this.clock.millis() - startTime;
            String errorType = exception.getClass().getSimpleName();

            log.warn(
                    "AI reply generation failed. conversationId={}, aiEnabled={}, reason={}",
                    conversationId,
                    this.chatbotAiSettings.isEnabled(),
                    errorType
            );

            return this.fallbackReply(
                    conversationId,
                    provider,
                    model,
                    durationMs,
                    baseReply,
                    errorType,
                    "AI_EXCEPTION"
            );
        }
    }

    private ChatbotAiReplyResult fallbackReply(
            String conversationId,
            String provider,
            String model,
            long durationMs,
            String baseReply,
            String errorType,
            String fallbackType
    ) {
        this.recordAiMetricSafely(
                this.aiMetric(conversationId, provider, model, durationMs, false, true, errorType)
        );
        this.recordFallbackMetricSafely(
                this.fallbackMetric(conversationId, fallbackType, errorType)
        );

        return ChatbotAiReplyResult.withoutAi(baseReply);
    }

    private ChatbotAiMetric aiMetric(
            String conversationId,
            String provider,
            String model,
            long durationMs,
            boolean success,
            boolean fallback,
            String errorType
    ) {
        return ChatbotAiMetric.builder()
                .conversationId(conversationId)
                .provider(provider)
                .model(model)
                .durationMs(durationMs)
                .success(success)
                .fallback(fallback)
                .errorType(errorType)
                .createdAt(LocalDateTime.now(this.clock))
                .build();
    }

    private ChatbotAiResponse generateWithTimeout(ChatbotAiRequest aiRequest) {
        try {
            return CompletableFuture
                    .supplyAsync(() -> this.chatbotAiClient.generate(aiRequest))
                    .orTimeout(this.aiTimeoutSeconds(), TimeUnit.SECONDS)
                    .join();

        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();

            if (cause instanceof TimeoutException) {
                throw new IllegalStateException("AI_TIMEOUT", cause);
            }

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new IllegalStateException("AI_PROVIDER_ERROR", cause);
        }
    }

    private int aiTimeoutSeconds() {
        return Math.max(1, this.chatbotAiSettings.timeoutSeconds());
    }


    private ChatbotFallbackMetric fallbackMetric(
            String conversationId,
            String fallbackType,
            String reason
    ) {
        return ChatbotFallbackMetric.builder()
                .conversationId(conversationId)
                .fallbackType(fallbackType)
                .reason(reason)
                .createdAt(LocalDateTime.now(this.clock))
                .build();
    }

    private void recordAiMetricSafely(ChatbotAiMetric metric) {
        try {
            this.chatbotMetricsRecorder.recordAiCall(metric);
        } catch (RuntimeException exception) {
            log.warn(
                    "AI metric recording failed. conversationId={}, reason={}",
                    metric.getConversationId(),
                    exception.getClass().getSimpleName()
            );
        }
    }

    private void recordFallbackMetricSafely(ChatbotFallbackMetric metric) {
        try {
            this.chatbotMetricsRecorder.recordFallback(metric);
        } catch (RuntimeException exception) {
            log.warn(
                    "Fallback metric recording failed. conversationId={}, reason={}",
                    metric.getConversationId(),
                    exception.getClass().getSimpleName()
            );
        }
    }

    private String safeErrorType(String error) {
        if (error.isBlank()) {
            return "AI_RESPONSE_ERROR";
        }

        return error.trim();
    }
}

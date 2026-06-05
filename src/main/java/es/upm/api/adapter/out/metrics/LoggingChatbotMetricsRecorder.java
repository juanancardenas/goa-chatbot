package es.upm.api.adapter.out.metrics;

import es.upm.api.domain.model.metrics.ChatbotAiMetric;
import es.upm.api.domain.model.metrics.ChatbotEscalationMetric;
import es.upm.api.domain.model.metrics.ChatbotFallbackMetric;
import es.upm.api.domain.model.metrics.ChatbotMessageMetric;
import es.upm.api.domain.model.metrics.ChatbotModerationMetric;
import es.upm.api.domain.ports.out.ChatbotMetricsRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingChatbotMetricsRecorder implements ChatbotMetricsRecorder {

    @Override
    public void recordMessageHandled(ChatbotMessageMetric metric) {
        if (metric == null) {
            log.warn("chatbot_metric_type=message_handled status=ignored reason=null_metric");
            return;
        }

        try {
            log.info(
                    "chatbot_metric_type=message_handled conversationId={} requestMessageId={} userId={} conversationType={} responseMode={} usedAi={} usedPlatformData={} durationMs={} success={} createdAt={}",
                    metric.getConversationId(),
                    metric.getRequestMessageId(),
                    metric.getUserId(),
                    metric.getConversationType(),
                    metric.getResponseMode(),
                    metric.isUsedAi(),
                    metric.isUsedPlatformData(),
                    metric.getDurationMs(),
                    metric.isSuccess(),
                    metric.getCreatedAt()
            );
        } catch (Exception exception) {
            log.warn("chatbot_metric_type=message_handled status=failed reason={}", exception.getClass().getSimpleName());
        }
    }

    @Override
    public void recordAiCall(ChatbotAiMetric metric) {
        if (metric == null) {
            log.warn("chatbot_metric_type=ai_call status=ignored reason=null_metric");
            return;
        }

        try {
            log.info(
                    "chatbot_metric_type=ai_call conversationId={} provider={} model={} durationMs={} success={} fallback={} errorType={} createdAt={}",
                    metric.getConversationId(),
                    metric.getProvider(),
                    metric.getModel(),
                    metric.getDurationMs(),
                    metric.isSuccess(),
                    metric.isFallback(),
                    metric.getErrorType(),
                    metric.getCreatedAt()
            );
        } catch (Exception exception) {
            log.warn("chatbot_metric_type=ai_call status=failed reason={}", exception.getClass().getSimpleName());
        }
    }

    @Override
    public void recordEscalation(ChatbotEscalationMetric metric) {
        if (metric == null) {
            log.warn("chatbot_metric_type=escalation status=ignored reason=null_metric");
            return;
        }

        try {
            log.info(
                    "chatbot_metric_type=escalation conversationId={} userId={} success={} errorType={} createdAt={}",
                    metric.getConversationId(),
                    metric.getUserId(),
                    metric.isSuccess(),
                    metric.getErrorType(),
                    metric.getCreatedAt()
            );
        } catch (Exception exception) {
            log.warn("chatbot_metric_type=escalation status=failed reason={}", exception.getClass().getSimpleName());
        }
    }

    @Override
    public void recordFallback(ChatbotFallbackMetric metric) {
        if (metric == null) {
            log.warn("chatbot_metric_type=fallback status=ignored reason=null_metric");
            return;
        }

        try {
            log.info(
                    "chatbot_metric_type=fallback conversationId={} fallbackType={} reason={} createdAt={}",
                    metric.getConversationId(),
                    metric.getFallbackType(),
                    metric.getReason(),
                    metric.getCreatedAt()
            );
        } catch (Exception exception) {
            log.warn("chatbot_metric_type=fallback status=failed reason={}", exception.getClass().getSimpleName());
        }
    }

    @Override
    public void recordModeration(ChatbotModerationMetric metric) {
        if (metric == null) {
            log.warn("chatbot_metric_type=moderation status=ignored reason=null_metric");
            return;
        }

        try {
            log.info(
                    "chatbot_metric_type=moderation conversationId={} userId={} action={} reason={} containsPii={} blocked={} usedAi={} createdAt={}",
                    metric.getConversationId(),
                    metric.getUserId(),
                    metric.getAction(),
                    metric.getReason(),
                    metric.isContainsPii(),
                    metric.isBlocked(),
                    metric.getUsedAi(),
                    metric.getCreatedAt()
            );
        } catch (Exception exception) {
            log.warn("chatbot_metric_type=moderation status=failed reason={}", exception.getClass().getSimpleName());
        }
    }
}

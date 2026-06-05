package es.upm.api.domain.ports.out;

import es.upm.api.domain.model.metrics.ChatbotAiMetric;
import es.upm.api.domain.model.metrics.ChatbotEscalationMetric;
import es.upm.api.domain.model.metrics.ChatbotFallbackMetric;
import es.upm.api.domain.model.metrics.ChatbotMessageMetric;
import es.upm.api.domain.model.metrics.ChatbotModerationMetric;

public interface ChatbotMetricsRecorder {

    void recordMessageHandled(ChatbotMessageMetric metric);

    void recordAiCall(ChatbotAiMetric metric);

    void recordEscalation(ChatbotEscalationMetric metric);

    void recordFallback(ChatbotFallbackMetric metric);

    void recordModeration(ChatbotModerationMetric metric);
}

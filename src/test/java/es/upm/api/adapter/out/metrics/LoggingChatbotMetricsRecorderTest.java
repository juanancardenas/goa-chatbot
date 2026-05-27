package es.upm.api.adapter.out.metrics;

import es.upm.api.domain.enums.ChatbotResponseMode;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.model.metrics.ChatbotAiMetric;
import es.upm.api.domain.model.metrics.ChatbotEscalationMetric;
import es.upm.api.domain.model.metrics.ChatbotFallbackMetric;
import es.upm.api.domain.model.metrics.ChatbotMessageMetric;
import es.upm.api.domain.ports.out.ChatbotMetricsRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class LoggingChatbotMetricsRecorderTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 5, 26, 13, 45);

    private final LoggingChatbotMetricsRecorder recorder = new LoggingChatbotMetricsRecorder();

    @Test
    void shouldImplementChatbotMetricsRecorderPort() {
        assertThat(this.recorder).isInstanceOf(ChatbotMetricsRecorder.class);
    }

    @Test
    void recordMessageHandledShouldLogMessageMetricFields(CapturedOutput output) {
        ChatbotMessageMetric metric = ChatbotMessageMetric.builder()
                .conversationId("conversation-1")
                .requestMessageId("message-1")
                .userId("user-1")
                .conversationType(ConversationType.CONTEXTUAL)
                .responseMode(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA)
                .usedAi(true)
                .usedPlatformData(true)
                .durationMs(450L)
                .success(true)
                .createdAt(CREATED_AT)
                .build();

        this.recorder.recordMessageHandled(metric);

        assertThat(output)
                .contains("chatbot_metric_type=message_handled")
                .contains("conversationId=conversation-1")
                .contains("requestMessageId=message-1")
                .contains("userId=user-1")
                .contains("conversationType=CONTEXTUAL")
                .contains("responseMode=CONTEXTUAL_PLATFORM_DATA")
                .contains("usedAi=true")
                .contains("usedPlatformData=true")
                .contains("durationMs=450")
                .contains("success=true")
                .contains("createdAt=2026-05-26T13:45");
    }

    @Test
    void recordAiCallShouldLogAiMetricFields(CapturedOutput output) {
        ChatbotAiMetric metric = ChatbotAiMetric.builder()
                .conversationId("conversation-1")
                .provider("openai")
                .model("gpt-test")
                .durationMs(90L)
                .success(false)
                .fallback(true)
                .errorType("TimeoutException")
                .createdAt(CREATED_AT)
                .build();

        this.recorder.recordAiCall(metric);

        assertThat(output)
                .contains("chatbot_metric_type=ai_call")
                .contains("conversationId=conversation-1")
                .contains("provider=openai")
                .contains("model=gpt-test")
                .contains("durationMs=90")
                .contains("success=false")
                .contains("fallback=true")
                .contains("errorType=TimeoutException")
                .contains("createdAt=2026-05-26T13:45");
    }

    @Test
    void recordEscalationShouldLogEscalationMetricFields(CapturedOutput output) {
        ChatbotEscalationMetric metric = ChatbotEscalationMetric.builder()
                .conversationId("conversation-1")
                .userId("user-1")
                .success(false)
                .errorType("BadGatewayException")
                .createdAt(CREATED_AT)
                .build();

        this.recorder.recordEscalation(metric);

        assertThat(output)
                .contains("chatbot_metric_type=escalation")
                .contains("conversationId=conversation-1")
                .contains("userId=user-1")
                .contains("success=false")
                .contains("errorType=BadGatewayException")
                .contains("createdAt=2026-05-26T13:45");
    }

    @Test
    void recordFallbackShouldLogFallbackMetricFields(CapturedOutput output) {
        ChatbotFallbackMetric metric = ChatbotFallbackMetric.builder()
                .conversationId("conversation-1")
                .fallbackType("AI_PROVIDER")
                .reason("Provider error")
                .createdAt(CREATED_AT)
                .build();

        this.recorder.recordFallback(metric);

        assertThat(output)
                .contains("chatbot_metric_type=fallback")
                .contains("conversationId=conversation-1")
                .contains("fallbackType=AI_PROVIDER")
                .contains("reason=Provider error")
                .contains("createdAt=2026-05-26T13:45");
    }

    @Test
    void recordMethodsShouldIgnoreNullMetrics(CapturedOutput output) {
        this.recorder.recordMessageHandled(null);
        this.recorder.recordAiCall(null);
        this.recorder.recordEscalation(null);
        this.recorder.recordFallback(null);

        assertThat(output)
                .contains("chatbot_metric_type=message_handled status=ignored reason=null_metric")
                .contains("chatbot_metric_type=ai_call status=ignored reason=null_metric")
                .contains("chatbot_metric_type=escalation status=ignored reason=null_metric")
                .contains("chatbot_metric_type=fallback status=ignored reason=null_metric");
    }
}

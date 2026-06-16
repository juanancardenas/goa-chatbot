package es.upm.api.domain.services.reply.ai;

import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.ConversationType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotAiReplyServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-01-01T10:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private ChatbotAiClient chatbotAiClient;

    @Mock
    private ChatbotAiSettings chatbotAiSettings;

    @Mock
    private ChatbotAiRequestBuilder chatbotAiRequestBuilder;

    @Mock
    private ChatbotMetricsRecorder chatbotMetricsRecorder;

    private ChatbotAiReplyService chatbotAiReplyService;

    @BeforeEach
    void setUp() {
        this.chatbotAiReplyService = new ChatbotAiReplyService(
                this.chatbotAiClient,
                this.chatbotAiSettings,
                this.chatbotAiRequestBuilder,
                this.chatbotMetricsRecorder,
                FIXED_CLOCK
        );
    }

    @Test
    void generateConfiguredAssistantReplyShouldReturnBaseReplyWhenAiIsDisabled() {
        when(this.chatbotAiSettings.isEnabled()).thenReturn(false);

        ChatbotAiReplyResult response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                this.generalConversation(),
                ConversationProfileType.PROFESSIONAL,
                "What can you do?",
                "Safe base reply",
                Optional.empty()
        );

        assertThat(response.getAssistantReply()).isEqualTo("Safe base reply");
        assertThat(response.isUsedAi()).isFalse();
        verify(this.chatbotAiRequestBuilder, never()).build(
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verify(this.chatbotAiClient, never()).generate(any());
        verify(this.chatbotMetricsRecorder, never()).recordAiCall(any());
        verify(this.chatbotMetricsRecorder, never()).recordFallback(any());
    }

    @Test
    void generateConfiguredAssistantReplyShouldDelegateRequestBuildAndReturnTrimmedAiContent() {
        Conversation conversation = this.contextualConversation();
        Optional<ChatbotPlatformContext> platformContext = Optional.of(ChatbotPlatformContext.builder()
                .engagementLetterId("EL-555")
                .build());
        ChatbotAiRequest aiRequest = ChatbotAiRequest.builder()
                .conversationId("conversation-contextual")
                .userMessage("Prompt user message")
                .build();

        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.chatbotAiSettings.provider()).thenReturn("ollama");
        when(this.chatbotAiSettings.model()).thenReturn("llama3.2:3b");
        when(this.chatbotAiRequestBuilder.build(
                conversation,
                ConversationProfileType.CLIENT,
                "Which legal tasks are pending?",
                "Safe contextual base reply",
                platformContext
        )).thenReturn(aiRequest);
        when(this.chatbotAiClient.generate(aiRequest)).thenReturn(ChatbotAiResponse.builder()
                .content("  AI contextual reply  ")
                .finishReason("SUCCESS")
                .build());

        ChatbotAiReplyResult response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.CLIENT,
                "Which legal tasks are pending?",
                "Safe contextual base reply",
                platformContext
        );

        assertThat(response.getAssistantReply()).isEqualTo("AI contextual reply");
        assertThat(response.isUsedAi()).isTrue();
        verify(this.chatbotAiRequestBuilder).build(
                conversation,
                ConversationProfileType.CLIENT,
                "Which legal tasks are pending?",
                "Safe contextual base reply",
                platformContext
        );
        verify(this.chatbotAiClient).generate(aiRequest);

        ArgumentCaptor<ChatbotAiMetric> aiMetricCaptor = ArgumentCaptor.forClass(ChatbotAiMetric.class);
        verify(this.chatbotMetricsRecorder).recordAiCall(aiMetricCaptor.capture());
        ChatbotAiMetric metric = aiMetricCaptor.getValue();
        assertThat(metric.getConversationId()).isEqualTo("conversation-contextual");
        assertThat(metric.getProvider()).isEqualTo("ollama");
        assertThat(metric.getModel()).isEqualTo("llama3.2:3b");
        assertThat(metric.isSuccess()).isTrue();
        assertThat(metric.isFallback()).isFalse();
        assertThat(metric.getErrorType()).isNull();
        assertThat(metric.getDurationMs()).isGreaterThanOrEqualTo(0);
        assertThat(metric.getCreatedAt()).isNotNull();
        verify(this.chatbotMetricsRecorder, never()).recordFallback(any());
    }

    @Test
    void generateConfiguredAssistantReplyShouldReturnBaseReplyWhenAiResponseIsInvalid() {
        Conversation conversation = this.generalConversation();
        ChatbotAiRequest aiRequest = ChatbotAiRequest.builder().conversationId("conversation-general").build();

        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.chatbotAiRequestBuilder.build(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        )).thenReturn(aiRequest);
        when(this.chatbotAiClient.generate(aiRequest))
                .thenReturn(null)
                .thenReturn(ChatbotAiResponse.builder().error("AI_PROVIDER_ERROR").build())
                .thenReturn(ChatbotAiResponse.builder().content(null).build())
                .thenReturn(ChatbotAiResponse.builder().content("   ").build());

        ChatbotAiReplyResult nullResponseResult = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        );
        ChatbotAiReplyResult errorResponseResult = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        );
        ChatbotAiReplyResult nullContentResult = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        );
        ChatbotAiReplyResult blankContentResult = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        );

        assertThat(nullResponseResult.getAssistantReply()).isEqualTo("Safe base reply");
        assertThat(errorResponseResult.getAssistantReply()).isEqualTo("Safe base reply");
        assertThat(nullContentResult.getAssistantReply()).isEqualTo("Safe base reply");
        assertThat(blankContentResult.getAssistantReply()).isEqualTo("Safe base reply");
        assertThat(nullResponseResult.isUsedAi()).isFalse();
        assertThat(errorResponseResult.isUsedAi()).isFalse();
        assertThat(nullContentResult.isUsedAi()).isFalse();
        assertThat(blankContentResult.isUsedAi()).isFalse();

        verify(this.chatbotAiRequestBuilder, times(4)).build(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        );
        verify(this.chatbotAiClient, times(4)).generate(aiRequest);

        ArgumentCaptor<ChatbotAiMetric> aiMetricCaptor = ArgumentCaptor.forClass(ChatbotAiMetric.class);
        verify(this.chatbotMetricsRecorder, times(4)).recordAiCall(aiMetricCaptor.capture());
        assertThat(aiMetricCaptor.getAllValues())
                .extracting(ChatbotAiMetric::isSuccess)
                .containsExactly(false, false, false, false);
        assertThat(aiMetricCaptor.getAllValues())
                .extracting(ChatbotAiMetric::isFallback)
                .containsExactly(true, true, true, true);
        assertThat(aiMetricCaptor.getAllValues())
                .extracting(ChatbotAiMetric::getErrorType)
                .containsExactly("NULL_AI_RESPONSE", "AI_PROVIDER_ERROR", "EMPTY_AI_RESPONSE", "EMPTY_AI_RESPONSE");

        ArgumentCaptor<ChatbotFallbackMetric> fallbackMetricCaptor = ArgumentCaptor.forClass(ChatbotFallbackMetric.class);
        verify(this.chatbotMetricsRecorder, times(4)).recordFallback(fallbackMetricCaptor.capture());
        assertThat(fallbackMetricCaptor.getAllValues())
                .extracting(ChatbotFallbackMetric::getFallbackType)
                .containsExactly("AI_NULL_RESPONSE", "AI_RESPONSE_ERROR", "AI_EMPTY_RESPONSE", "AI_EMPTY_RESPONSE");
        assertThat(fallbackMetricCaptor.getAllValues())
                .extracting(ChatbotFallbackMetric::getReason)
                .containsExactly("NULL_AI_RESPONSE", "AI_PROVIDER_ERROR", "EMPTY_AI_RESPONSE", "EMPTY_AI_RESPONSE");
        assertThat(fallbackMetricCaptor.getAllValues())
                .allSatisfy(metric -> assertThat(metric.getCreatedAt()).isNotNull());
    }

    @Test
    void generateConfiguredAssistantReplyShouldUseDefaultErrorTypeWhenAiResponseErrorIsBlank() {
        Conversation conversation = this.generalConversation();
        ChatbotAiRequest aiRequest = ChatbotAiRequest.builder().conversationId("conversation-general").build();

        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.chatbotAiRequestBuilder.build(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        )).thenReturn(aiRequest);
        when(this.chatbotAiClient.generate(aiRequest))
                .thenReturn(ChatbotAiResponse.builder().error("   ").build());

        ChatbotAiReplyResult response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        );

        assertThat(response.getAssistantReply()).isEqualTo("Safe base reply");
        assertThat(response.isUsedAi()).isFalse();

        ArgumentCaptor<ChatbotAiMetric> aiMetricCaptor = ArgumentCaptor.forClass(ChatbotAiMetric.class);
        verify(this.chatbotMetricsRecorder).recordAiCall(aiMetricCaptor.capture());
        assertThat(aiMetricCaptor.getValue().getErrorType()).isEqualTo("AI_RESPONSE_ERROR");
        assertThat(aiMetricCaptor.getValue().isFallback()).isTrue();

        ArgumentCaptor<ChatbotFallbackMetric> fallbackMetricCaptor = ArgumentCaptor.forClass(ChatbotFallbackMetric.class);
        verify(this.chatbotMetricsRecorder).recordFallback(fallbackMetricCaptor.capture());
        assertThat(fallbackMetricCaptor.getValue().getFallbackType()).isEqualTo("AI_RESPONSE_ERROR");
        assertThat(fallbackMetricCaptor.getValue().getReason()).isEqualTo("AI_RESPONSE_ERROR");
    }

    @Test
    void generateConfiguredAssistantReplyShouldReturnBaseReplyWhenRequestBuilderThrowsException() {
        Conversation conversation = this.generalConversation();

        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.chatbotAiRequestBuilder.build(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        )).thenThrow(new RuntimeException("history unavailable"));

        ChatbotAiReplyResult response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        );

        assertThat(response.getAssistantReply()).isEqualTo("Safe base reply");
        assertThat(response.isUsedAi()).isFalse();
        verify(this.chatbotAiClient, never()).generate(any());

        ArgumentCaptor<ChatbotAiMetric> aiMetricCaptor = ArgumentCaptor.forClass(ChatbotAiMetric.class);
        verify(this.chatbotMetricsRecorder).recordAiCall(aiMetricCaptor.capture());
        assertThat(aiMetricCaptor.getValue().isSuccess()).isFalse();
        assertThat(aiMetricCaptor.getValue().isFallback()).isTrue();
        assertThat(aiMetricCaptor.getValue().getErrorType()).isEqualTo("RuntimeException");
        verify(this.chatbotMetricsRecorder).recordFallback(any(ChatbotFallbackMetric.class));
    }

    @Test
    void generateConfiguredAssistantReplyShouldReturnBaseReplyWhenAiClientThrowsException() {
        Conversation conversation = this.generalConversation();
        ChatbotAiRequest aiRequest = ChatbotAiRequest.builder().conversationId("conversation-general").build();

        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.chatbotAiRequestBuilder.build(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        )).thenReturn(aiRequest);
        when(this.chatbotAiClient.generate(aiRequest)).thenThrow(new RuntimeException("provider unavailable"));

        ChatbotAiReplyResult response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        );

        assertThat(response.getAssistantReply()).isEqualTo("Safe base reply");
        assertThat(response.isUsedAi()).isFalse();

        ArgumentCaptor<ChatbotAiMetric> aiMetricCaptor = ArgumentCaptor.forClass(ChatbotAiMetric.class);
        verify(this.chatbotMetricsRecorder).recordAiCall(aiMetricCaptor.capture());
        assertThat(aiMetricCaptor.getValue().isSuccess()).isFalse();
        assertThat(aiMetricCaptor.getValue().isFallback()).isTrue();
        assertThat(aiMetricCaptor.getValue().getErrorType()).isEqualTo("RuntimeException");
        verify(this.chatbotMetricsRecorder).recordFallback(any(ChatbotFallbackMetric.class));
    }

    @Test
    void generateConfiguredAssistantReplyShouldReturnAiContentWhenAiMetricRecorderFails() {
        Conversation conversation = this.generalConversation();
        ChatbotAiRequest aiRequest = ChatbotAiRequest.builder().conversationId("conversation-general").build();

        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.chatbotAiRequestBuilder.build(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        )).thenReturn(aiRequest);
        when(this.chatbotAiClient.generate(aiRequest)).thenReturn(ChatbotAiResponse.builder()
                .content("AI final reply")
                .build());
        doThrow(new RuntimeException("metrics unavailable"))
                .when(this.chatbotMetricsRecorder)
                .recordAiCall(any(ChatbotAiMetric.class));

        ChatbotAiReplyResult response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        );

        assertThat(response.getAssistantReply()).isEqualTo("AI final reply");
        assertThat(response.isUsedAi()).isTrue();
        verify(this.chatbotMetricsRecorder).recordAiCall(any(ChatbotAiMetric.class));
        verify(this.chatbotMetricsRecorder, never()).recordFallback(any());
    }

    @Test
    void generateConfiguredAssistantReplyShouldKeepFallbackWhenMetricsRecorderFails() {
        Conversation conversation = this.generalConversation();
        ChatbotAiRequest aiRequest = ChatbotAiRequest.builder().conversationId("conversation-general").build();

        when(this.chatbotAiSettings.isEnabled()).thenReturn(true);
        when(this.chatbotAiRequestBuilder.build(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        )).thenReturn(aiRequest);
        when(this.chatbotAiClient.generate(aiRequest)).thenReturn(null);
        doThrow(new RuntimeException("metrics unavailable"))
                .when(this.chatbotMetricsRecorder)
                .recordAiCall(any(ChatbotAiMetric.class));
        doThrow(new RuntimeException("fallback metrics unavailable"))
                .when(this.chatbotMetricsRecorder)
                .recordFallback(any(ChatbotFallbackMetric.class));

        ChatbotAiReplyResult response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        );

        assertThat(response.getAssistantReply()).isEqualTo("Safe base reply");
        assertThat(response.isUsedAi()).isFalse();
        verify(this.chatbotMetricsRecorder).recordAiCall(any(ChatbotAiMetric.class));
        verify(this.chatbotMetricsRecorder).recordFallback(any(ChatbotFallbackMetric.class));
    }

    private Conversation generalConversation() {
        return Conversation.builder()
                .id("conversation-general")
                .userId("professional-1")
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();
    }

    private Conversation contextualConversation() {
        return Conversation.builder()
                .id("conversation-contextual")
                .userId("customer-9")
                .engagementLetterId("EL-555")
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, 4, 19, 10, 30))
                .build();
    }
}

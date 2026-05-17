package es.upm.api.domain.services.reply.ai;

import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.ai.ChatbotAiRequest;
import es.upm.api.domain.model.ai.ChatbotAiResponse;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.ports.out.ChatbotAiClient;
import es.upm.api.domain.ports.out.ChatbotAiSettings;
import es.upm.api.domain.services.reply.ai.prompt.ChatbotAiRequestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotAiReplyServiceTest {

    @Mock
    private ChatbotAiClient chatbotAiClient;

    @Mock
    private ChatbotAiSettings chatbotAiSettings;

    @Mock
    private ChatbotAiRequestBuilder chatbotAiRequestBuilder;

    private ChatbotAiReplyService chatbotAiReplyService;

    @BeforeEach
    void setUp() {
        this.chatbotAiReplyService = new ChatbotAiReplyService(
                this.chatbotAiClient,
                this.chatbotAiSettings,
                this.chatbotAiRequestBuilder
        );
    }

    @Test
    void generateConfiguredAssistantReplyShouldReturnBaseReplyWhenAiIsDisabled() {
        when(this.chatbotAiSettings.isEnabled()).thenReturn(false);

        String response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                this.generalConversation(),
                ConversationProfileType.PROFESSIONAL,
                "What can you do?",
                "Safe base reply",
                Optional.empty()
        );

        assertThat(response).isEqualTo("Safe base reply");
        verify(this.chatbotAiRequestBuilder, never()).build(
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verify(this.chatbotAiClient, never()).generate(any());
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

        String response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.CLIENT,
                "Which legal tasks are pending?",
                "Safe contextual base reply",
                platformContext
        );

        assertThat(response).isEqualTo("AI contextual reply");
        verify(this.chatbotAiRequestBuilder).build(
                conversation,
                ConversationProfileType.CLIENT,
                "Which legal tasks are pending?",
                "Safe contextual base reply",
                platformContext
        );
        verify(this.chatbotAiClient).generate(aiRequest);
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

        assertThat(this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        )).isEqualTo("Safe base reply");
        assertThat(this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        )).isEqualTo("Safe base reply");
        assertThat(this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        )).isEqualTo("Safe base reply");
        assertThat(this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        )).isEqualTo("Safe base reply");

        verify(this.chatbotAiRequestBuilder, times(4)).build(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        );
        verify(this.chatbotAiClient, times(4)).generate(aiRequest);
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

        String response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        );

        assertThat(response).isEqualTo("Safe base reply");
        verify(this.chatbotAiClient, never()).generate(any());
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

        String response = this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "Question",
                "Safe base reply",
                Optional.empty()
        );

        assertThat(response).isEqualTo("Safe base reply");
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

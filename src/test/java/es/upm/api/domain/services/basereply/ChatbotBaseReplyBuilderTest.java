package es.upm.api.domain.services.basereply;

import es.upm.api.domain.common.ChatbotResponseMessages;
import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.enums.PlatformQuestionType;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.services.classification.ChatbotQuestionClassifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotBaseReplyBuilderTest {

    @Mock
    private ChatbotQuestionClassifier chatbotQuestionClassifier;

    @Mock
    private ChatbotCourtesyReplyBuilder chatbotCourtesyReplyBuilder;

    @Mock
    private ChatbotGeneralReplyBuilder chatbotGeneralReplyBuilder;

    @Mock
    private ChatbotContextualFallbackReplyBuilder chatbotContextualFallbackReplyBuilder;

    @Mock
    private ChatbotPlatformReplyBuilder chatbotPlatformReplyBuilder;

    @InjectMocks
    private ChatbotBaseReplyBuilder chatbotBaseReplyBuilder;

    @Test
    void generalStartReplyShouldDelegateToGeneralReplyBuilder() {
        when(this.chatbotGeneralReplyBuilder.generalStartReply(ConversationProfileType.PROFESSIONAL))
                .thenReturn(ChatbotResponseMessages.PROFESSIONAL_GENERAL_START_REPLY);

        assertThat(this.chatbotBaseReplyBuilder.generalStartReply(ConversationProfileType.PROFESSIONAL))
                .isEqualTo(ChatbotResponseMessages.PROFESSIONAL_GENERAL_START_REPLY);

        verify(this.chatbotGeneralReplyBuilder).generalStartReply(ConversationProfileType.PROFESSIONAL);
    }

    @Test
    void isCourtesyMessageShouldDelegateToCourtesyReplyBuilder() {
        when(this.chatbotCourtesyReplyBuilder.isCourtesyMessage("Muchas gracias")).thenReturn(true);

        assertThat(this.chatbotBaseReplyBuilder.isCourtesyMessage("Muchas gracias")).isTrue();

        verify(this.chatbotCourtesyReplyBuilder).isCourtesyMessage("Muchas gracias");
    }

    @Test
    void courtesyReplyShouldDelegateToCourtesyReplyBuilder() {
        when(this.chatbotCourtesyReplyBuilder.courtesyReply(ConversationProfileType.CLIENT))
                .thenReturn(ChatbotResponseMessages.CLIENT_COURTESY_REPLY);

        assertThat(this.chatbotBaseReplyBuilder.courtesyReply(ConversationProfileType.CLIENT))
                .isEqualTo(ChatbotResponseMessages.CLIENT_COURTESY_REPLY);

        verify(this.chatbotCourtesyReplyBuilder).courtesyReply(ConversationProfileType.CLIENT);
    }

    @Test
    void generalFaqReplyShouldDelegateToGeneralReplyBuilder() {
        when(this.chatbotGeneralReplyBuilder.generalFaqReply(
                ConversationProfileType.CLIENT,
                "Cual es el estado de mi caso"
        )).thenReturn(ChatbotResponseMessages.CLIENT_GENERAL_STATUS_REPLY);

        assertThat(this.chatbotBaseReplyBuilder.generalFaqReply(
                ConversationProfileType.CLIENT,
                "Cual es el estado de mi caso"
        )).isEqualTo(ChatbotResponseMessages.CLIENT_GENERAL_STATUS_REPLY);

        verify(this.chatbotGeneralReplyBuilder).generalFaqReply(
                ConversationProfileType.CLIENT,
                "Cual es el estado de mi caso"
        );
    }

    @Test
    void contextualReplyShouldDelegateToContextualFallbackReplyBuilder() {
        when(this.chatbotContextualFallbackReplyBuilder.contextualFallbackReply(
                ConversationProfileType.CLIENT,
                "Dame contexto del caso"
        )).thenReturn(ChatbotResponseMessages.CONTEXTUAL_PLATFORM_DATA_UNAVAILABLE_REPLY);

        assertThat(this.chatbotBaseReplyBuilder.contextualReply(
                ConversationProfileType.CLIENT,
                "Dame contexto del caso"
        )).isEqualTo(ChatbotResponseMessages.CONTEXTUAL_PLATFORM_DATA_UNAVAILABLE_REPLY);

        verify(this.chatbotContextualFallbackReplyBuilder).contextualFallbackReply(
                ConversationProfileType.CLIENT,
                "Dame contexto del caso"
        );
    }

    @Test
    void contextualPlatformReplyShouldClassifyAndDelegateToPlatformReplyBuilder() {
        Conversation conversation = this.conversation("conversation-platform");
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder().build();
        when(this.chatbotQuestionClassifier.classify("Que tareas legales tiene el encargo"))
                .thenReturn(PlatformQuestionType.LEGAL_TASKS);
        when(this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                conversation,
                platformContext,
                PlatformQuestionType.LEGAL_TASKS
        )).thenReturn("Respuesta plataforma");

        assertThat(this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                "Que tareas legales tiene el encargo",
                conversation,
                platformContext
        )).isEqualTo("Respuesta plataforma");

        verify(this.chatbotPlatformReplyBuilder).contextualPlatformReply(
                ConversationProfileType.CLIENT,
                conversation,
                platformContext,
                PlatformQuestionType.LEGAL_TASKS
        );
    }

    @Test
    void contextualPlatformReplyShouldDelegateWithGeneralContextWhenClassifierReturnsNull() {
        Conversation conversation = this.conversation("conversation-platform-null");
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder().build();
        when(this.chatbotQuestionClassifier.classify("Dame informacion relacionada"))
                .thenReturn(null);
        when(this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                conversation,
                platformContext,
                PlatformQuestionType.GENERAL_CONTEXT
        )).thenReturn("Respuesta general contextual");

        assertThat(this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                "Dame informacion relacionada",
                conversation,
                platformContext
        )).isEqualTo("Respuesta general contextual");

        verify(this.chatbotPlatformReplyBuilder).contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                conversation,
                platformContext,
                PlatformQuestionType.GENERAL_CONTEXT
        );
    }

    private Conversation conversation(String id) {
        return Conversation.builder()
                .id(id)
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, 5, 15, 10, 0))
                .build();
    }
}

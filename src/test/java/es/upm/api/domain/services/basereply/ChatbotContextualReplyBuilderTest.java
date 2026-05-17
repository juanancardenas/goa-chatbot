package es.upm.api.domain.services.basereply;

import es.upm.api.domain.common.ChatbotResponseMessages;
import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.PlatformQuestionType;
import es.upm.api.domain.services.classification.ChatbotQuestionClassifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotContextualReplyBuilderTest {

    @Mock
    private ChatbotQuestionClassifier chatbotQuestionClassifier;

    @InjectMocks
    private ChatbotContextualReplyBuilder chatbotContextualReplyBuilder;

    @Test
    void contextualReplyShouldReturnUnavailableReplyWhenClassifierReturnsNull() {
        when(this.chatbotQuestionClassifier.classify("Dame contexto del caso")).thenReturn(null);

        assertThat(this.chatbotContextualReplyBuilder.contextualReply(
                ConversationProfileType.CLIENT,
                "Dame contexto del caso"
        )).isEqualTo(ChatbotResponseMessages.CONTEXTUAL_PLATFORM_DATA_UNAVAILABLE_REPLY);
    }

    @Test
    void contextualReplyShouldReturnReplyByQuestionTypeAndProfile() {
        when(this.chatbotQuestionClassifier.classify("Que hitos recientes tiene el caso"))
                .thenReturn(PlatformQuestionType.TIMELINE_EVENTS);

        assertThat(this.chatbotContextualReplyBuilder.contextualReply(
                ConversationProfileType.PROFESSIONAL,
                "Que hitos recientes tiene el caso"
        )).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_EVENTS_REPLY);
    }

    @ParameterizedTest
    @MethodSource("contextUnavailableReplies")
    void contextualReplyShouldReturnUnavailableReplyByQuestionTypeAndProfile(
            ConversationProfileType profile,
            PlatformQuestionType questionType,
            String expectedReply
    ) {
        String message = "Pregunta sin contexto " + profile + " " + questionType;
        when(this.chatbotQuestionClassifier.classify(message)).thenReturn(questionType);

        assertThat(this.chatbotContextualReplyBuilder.contextualReply(profile, message))
                .isEqualTo(expectedReply);
    }

    private static Stream<Arguments> contextUnavailableReplies() {
        return Stream.of(
                Arguments.of(
                        ConversationProfileType.CLIENT,
                        PlatformQuestionType.ENGAGEMENT_STATUS,
                        ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_STATUS_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.PROFESSIONAL,
                        PlatformQuestionType.ENGAGEMENT_STATUS,
                        ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_STATUS_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.CLIENT,
                        PlatformQuestionType.TIMELINE_EVENTS,
                        ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_EVENTS_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.CLIENT,
                        PlatformQuestionType.DOCUMENTS,
                        ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_DOCUMENTS_STUB_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.PROFESSIONAL,
                        PlatformQuestionType.DOCUMENTS,
                        ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_DOCUMENTS_STUB_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.CLIENT,
                        PlatformQuestionType.LEGAL_TASKS,
                        ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_LEGAL_TASKS_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.PROFESSIONAL,
                        PlatformQuestionType.LEGAL_TASKS,
                        ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_LEGAL_TASKS_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.CLIENT,
                        PlatformQuestionType.GENERAL_CONTEXT,
                        ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_GENERAL_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.PROFESSIONAL,
                        PlatformQuestionType.GENERAL_CONTEXT,
                        ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_GENERAL_REPLY
                )
        );
    }
}

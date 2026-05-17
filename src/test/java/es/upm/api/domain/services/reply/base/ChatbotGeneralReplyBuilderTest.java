package es.upm.api.domain.services.reply.base;

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
class ChatbotGeneralReplyBuilderTest {

    @Mock
    private ChatbotQuestionClassifier chatbotQuestionClassifier;

    @InjectMocks
    private ChatbotGeneralReplyBuilder chatbotGeneralReplyBuilder;

    @Test
    void generalStartReplyShouldReturnReplyByProfile() {
        assertThat(this.chatbotGeneralReplyBuilder.generalStartReply(ConversationProfileType.CLIENT))
                .isEqualTo(ChatbotResponseMessages.CLIENT_GENERAL_START_REPLY);
        assertThat(this.chatbotGeneralReplyBuilder.generalStartReply(ConversationProfileType.PROFESSIONAL))
                .isEqualTo(ChatbotResponseMessages.PROFESSIONAL_GENERAL_START_REPLY);
    }

    @Test
    void generalFaqReplyShouldReturnSpecificStatusReplyWhenMessageTargetsCurrentCase() {
        when(this.chatbotQuestionClassifier.classify("Cual es el estado de mi caso"))
                .thenReturn(PlatformQuestionType.ENGAGEMENT_STATUS);

        assertThat(this.chatbotGeneralReplyBuilder.generalFaqReply(
                ConversationProfileType.CLIENT,
                "Cual es el estado de mi caso"
        )).isEqualTo(ChatbotResponseMessages.CLIENT_GENERAL_STATUS_REPLY);
    }

    @Test
    void generalFaqReplyShouldReturnExampleTimelineReplyWhenQuestionIsGeneric() {
        when(this.chatbotQuestionClassifier.classify("Que plazos o hitos tiene esto"))
                .thenReturn(PlatformQuestionType.TIMELINE_EVENTS);

        assertThat(this.chatbotGeneralReplyBuilder.generalFaqReply(
                ConversationProfileType.PROFESSIONAL,
                "Que plazos o hitos tiene esto"
        )).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_GENERAL_TIMELINE_EXAMPLE_REPLY);
    }

    @ParameterizedTest
    @MethodSource("generalFaqReplies")
    void generalFaqReplyShouldReturnReplyByQuestionTypeProfileAndSpecificity(
            ConversationProfileType profile,
            PlatformQuestionType questionType,
            String message,
            String expectedReply
    ) {
        when(this.chatbotQuestionClassifier.classify(message)).thenReturn(questionType);

        assertThat(this.chatbotGeneralReplyBuilder.generalFaqReply(profile, message))
                .isEqualTo(expectedReply);
    }

    @Test
    void generalFaqReplyShouldFallbackToGeneralContextWhenClassifierReturnsNull() {
        when(this.chatbotQuestionClassifier.classify("Explicame que puedes hacer")).thenReturn(null);

        assertThat(this.chatbotGeneralReplyBuilder.generalFaqReply(
                ConversationProfileType.PROFESSIONAL,
                "Explicame que puedes hacer"
        )).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_GENERAL_CONTEXT_REPLY);
    }

    @ParameterizedTest
    @MethodSource("specificEngagementMessages")
    void generalFaqReplyShouldTreatCaseReferencesAsSpecificEngagementData(String message) {
        when(this.chatbotQuestionClassifier.classify(message)).thenReturn(PlatformQuestionType.ENGAGEMENT_STATUS);

        assertThat(this.chatbotGeneralReplyBuilder.generalFaqReply(ConversationProfileType.PROFESSIONAL, message))
                .isEqualTo(ChatbotResponseMessages.PROFESSIONAL_GENERAL_STATUS_REPLY);
    }

    private static Stream<Arguments> generalFaqReplies() {
        return Stream.of(
                Arguments.of(
                        ConversationProfileType.CLIENT,
                        PlatformQuestionType.ENGAGEMENT_STATUS,
                        "Que estado tiene una hoja de encargo",
                        ChatbotResponseMessages.CLIENT_GENERAL_STATUS_EXAMPLE_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.PROFESSIONAL,
                        PlatformQuestionType.ENGAGEMENT_STATUS,
                        null,
                        ChatbotResponseMessages.PROFESSIONAL_GENERAL_STATUS_EXAMPLE_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.CLIENT,
                        PlatformQuestionType.TIMELINE_EVENTS,
                        "Que hitos hay en este expediente",
                        ChatbotResponseMessages.CLIENT_GENERAL_TIMELINE_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.PROFESSIONAL,
                        PlatformQuestionType.TIMELINE_EVENTS,
                        "Que hitos tiene mi encargo",
                        ChatbotResponseMessages.PROFESSIONAL_GENERAL_TIMELINE_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.CLIENT,
                        PlatformQuestionType.TIMELINE_EVENTS,
                        "Que hitos se suelen manejar",
                        ChatbotResponseMessages.CLIENT_GENERAL_TIMELINE_EXAMPLE_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.CLIENT,
                        PlatformQuestionType.LEGAL_TASKS,
                        "Que tareas legales tiene mi encargo",
                        ChatbotResponseMessages.CLIENT_GENERAL_LEGAL_TASKS_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.CLIENT,
                        PlatformQuestionType.LEGAL_TASKS,
                        "Que tareas legales existen",
                        ChatbotResponseMessages.CLIENT_GENERAL_LEGAL_TASKS_EXAMPLE_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.PROFESSIONAL,
                        PlatformQuestionType.LEGAL_TASKS,
                        "Que tareas legales existen",
                        ChatbotResponseMessages.PROFESSIONAL_GENERAL_LEGAL_TASKS_EXAMPLE_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.PROFESSIONAL,
                        PlatformQuestionType.LEGAL_TASKS,
                        "Que tareas legales tiene este caso",
                        ChatbotResponseMessages.PROFESSIONAL_GENERAL_LEGAL_TASKS_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.CLIENT,
                        PlatformQuestionType.DOCUMENTS,
                        "Que documentos puedo consultar",
                        ChatbotResponseMessages.CLIENT_GENERAL_DOCUMENTS_STUB_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.PROFESSIONAL,
                        PlatformQuestionType.DOCUMENTS,
                        "Que documentos puedo consultar",
                        ChatbotResponseMessages.PROFESSIONAL_GENERAL_DOCUMENTS_STUB_REPLY
                ),
                Arguments.of(
                        ConversationProfileType.CLIENT,
                        PlatformQuestionType.GENERAL_CONTEXT,
                        "Que puedes explicar",
                        ChatbotResponseMessages.CLIENT_GENERAL_CONTEXT_REPLY
                )
        );
    }

    private static Stream<String> specificEngagementMessages() {
        return Stream.of(
                "Dame el estado de este encargo",
                "Dame el estado de mi hoja de encargo",
                "Dame el estado del expediente",
                "Dame el estado de este expediente"
        );
    }
}

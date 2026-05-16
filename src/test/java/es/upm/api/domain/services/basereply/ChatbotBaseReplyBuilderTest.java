package es.upm.api.domain.services.basereply;

import es.upm.api.domain.enums.ConversationType;

import es.upm.api.domain.common.ChatbotResponseMessages;
import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.PlatformQuestionType;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.platform.ChatbotDocumentContext;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.services.classification.ChatbotQuestionClassifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotBaseReplyBuilderTest {

    @Mock
    private ChatbotQuestionClassifier chatbotQuestionClassifier;

    @Mock
    private ChatbotDocumentContextService chatbotDocumentContextService;

    @InjectMocks
    private ChatbotBaseReplyBuilder chatbotBaseReplyBuilder;

    @Test
    void generalStartReplyShouldReturnReplyByProfile() {
        assertThat(this.chatbotBaseReplyBuilder.generalStartReply(ConversationProfileType.CLIENT))
                .isEqualTo(ChatbotResponseMessages.CLIENT_GENERAL_START_REPLY);
        assertThat(this.chatbotBaseReplyBuilder.generalStartReply(ConversationProfileType.PROFESSIONAL))
                .isEqualTo(ChatbotResponseMessages.PROFESSIONAL_GENERAL_START_REPLY);
    }

    @Test
    void courtesyHelpersShouldDetectCourtesyAndReturnReplyByProfile() {
        assertThat(this.chatbotBaseReplyBuilder.isCourtesyMessage("Muchas gracias por todo")).isTrue();
        assertThat(this.chatbotBaseReplyBuilder.isCourtesyMessage("Necesito ayuda con mi expediente")).isFalse();
        assertThat(this.chatbotBaseReplyBuilder.courtesyReply(ConversationProfileType.CLIENT))
                .isEqualTo(ChatbotResponseMessages.CLIENT_COURTESY_REPLY);
        assertThat(this.chatbotBaseReplyBuilder.courtesyReply(ConversationProfileType.PROFESSIONAL))
                .isEqualTo(ChatbotResponseMessages.PROFESSIONAL_COURTESY_REPLY);
    }

    @ParameterizedTest
    @MethodSource("courtesyMessages")
    void isCourtesyMessageShouldDetectSupportedCourtesyExpressions(String message) {
        assertThat(this.chatbotBaseReplyBuilder.isCourtesyMessage(message)).isTrue();
    }

    @Test
    void isCourtesyMessageShouldReturnFalseWhenMessageIsNullOrBlank() {
        assertThat(this.chatbotBaseReplyBuilder.isCourtesyMessage(null)).isFalse();
        assertThat(this.chatbotBaseReplyBuilder.isCourtesyMessage("   ")).isFalse();
    }

    @Test
    void contextualFallbackReplyShouldReturnUnavailableReplyWhenClassifierReturnsNull() {
        when(this.chatbotQuestionClassifier.classify("Dame contexto del caso")).thenReturn(null);

        assertThat(this.chatbotBaseReplyBuilder.contextualFallbackReply(
                ConversationProfileType.CLIENT,
                "Dame contexto del caso"
        )).isEqualTo(ChatbotResponseMessages.CONTEXTUAL_PLATFORM_DATA_UNAVAILABLE_REPLY);
    }

    @Test
    void contextualFallbackReplyShouldReturnReplyByQuestionTypeAndProfile() {
        when(this.chatbotQuestionClassifier.classify("Que hitos recientes tiene el caso"))
                .thenReturn(PlatformQuestionType.TIMELINE_EVENTS);

        assertThat(this.chatbotBaseReplyBuilder.contextualFallbackReply(
                ConversationProfileType.PROFESSIONAL,
                "Que hitos recientes tiene el caso"
        )).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_EVENTS_REPLY);
    }

    @ParameterizedTest
    @MethodSource("contextUnavailableReplies")
    void contextualFallbackReplyShouldReturnUnavailableReplyByQuestionTypeAndProfile(
            ConversationProfileType profile,
            PlatformQuestionType questionType,
            String expectedReply
    ) {
        String message = "Pregunta sin contexto " + profile + " " + questionType;
        when(this.chatbotQuestionClassifier.classify(message)).thenReturn(questionType);

        assertThat(this.chatbotBaseReplyBuilder.contextualFallbackReply(profile, message))
                .isEqualTo(expectedReply);
    }

    @Test
    void generalFaqReplyShouldReturnSpecificStatusReplyWhenMessageTargetsCurrentCase() {
        when(this.chatbotQuestionClassifier.classify("Cual es el estado de mi caso"))
                .thenReturn(PlatformQuestionType.ENGAGEMENT_STATUS);

        assertThat(this.chatbotBaseReplyBuilder.generalFaqReply(
                ConversationProfileType.CLIENT,
                "Cual es el estado de mi caso"
        )).isEqualTo(ChatbotResponseMessages.CLIENT_GENERAL_STATUS_REPLY);
    }

    @Test
    void generalFaqReplyShouldReturnExampleTimelineReplyWhenQuestionIsGeneric() {
        when(this.chatbotQuestionClassifier.classify("Que plazos o hitos tiene esto"))
                .thenReturn(PlatformQuestionType.TIMELINE_EVENTS);

        assertThat(this.chatbotBaseReplyBuilder.generalFaqReply(
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

        assertThat(this.chatbotBaseReplyBuilder.generalFaqReply(profile, message))
                .isEqualTo(expectedReply);
    }

    @Test
    void generalFaqReplyShouldFallbackToGeneralContextWhenClassifierReturnsNull() {
        when(this.chatbotQuestionClassifier.classify("Explicame que puedes hacer")).thenReturn(null);

        assertThat(this.chatbotBaseReplyBuilder.generalFaqReply(
                ConversationProfileType.PROFESSIONAL,
                "Explicame que puedes hacer"
        )).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_GENERAL_CONTEXT_REPLY);
    }

    @Test
    void contextualPlatformReplyShouldReturnLegalTasksListWhenTasksExist() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .legalTaskSummaries(List.of("Estudio de antecedentes", "Asesoramiento juridico"))
                .build();
        when(this.chatbotQuestionClassifier.classify("Cuales son las tareas legales"))
                .thenReturn(PlatformQuestionType.LEGAL_TASKS);

        String reply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                "Cuales son las tareas legales",
                this.conversation("conversation-1"),
                platformContext
        );

        assertThat(reply).contains("Tareas");
        assertThat(reply).contains("Estudio de antecedentes");
        assertThat(reply).contains("Asesoramiento juridico");
    }

    @Test
    void contextualPlatformReplyShouldReturnNoLegalTasksReplyWhenTasksAreMissing() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .legalTaskSummaries(List.of())
                .build();
        when(this.chatbotQuestionClassifier.classify("Que tareas legales tiene el encargo"))
                .thenReturn(PlatformQuestionType.LEGAL_TASKS);

        String reply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                "Que tareas legales tiene el encargo",
                this.conversation("conversation-no-tasks"),
                platformContext
        );

        assertThat(reply).isEqualTo(ChatbotResponseMessages.CLIENT_CONTEXTUAL_NO_LEGAL_TASKS_REPLY);
    }

    @Test
    void contextualPlatformReplyShouldReturnProfessionalNoLegalTasksReplyWhenTasksAreMissing() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder().build();
        when(this.chatbotQuestionClassifier.classify("Que tareas profesionales hay"))
                .thenReturn(PlatformQuestionType.LEGAL_TASKS);

        String reply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                "Que tareas profesionales hay",
                this.conversation("conversation-no-professional-tasks"),
                platformContext
        );

        assertThat(reply).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_NO_LEGAL_TASKS_REPLY);
    }

    @Test
    void contextualPlatformReplyShouldReturnNoEventsReplyWhenTimelineHasNoEventsOrProcedures() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .recentEventSummaries(List.of())
                .procedureTitles(List.of())
                .build();
        when(this.chatbotQuestionClassifier.classify("Que hitos tiene mi caso"))
                .thenReturn(PlatformQuestionType.TIMELINE_EVENTS);

        String reply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                "Que hitos tiene mi caso",
                this.conversation("conversation-2"),
                platformContext
        );

        assertThat(reply).isEqualTo(ChatbotResponseMessages.CLIENT_CONTEXTUAL_NO_EVENTS_REPLY);
    }

    @Test
    void contextualPlatformReplyShouldAppendEventsAndProceduresForTimelineQuestion() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .recentEventSummaries(List.of("Demanda admitida", "Vista senalada"))
                .procedureTitles(List.of("Procedimiento ordinario"))
                .build();
        when(this.chatbotQuestionClassifier.classify("Que hitos recientes tiene el expediente"))
                .thenReturn(PlatformQuestionType.TIMELINE_EVENTS);

        String reply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                "Que hitos recientes tiene el expediente",
                this.conversation("conversation-timeline"),
                platformContext
        );

        assertThat(reply).contains("Demanda admitida");
        assertThat(reply).contains("Vista senalada");
        assertThat(reply).contains("Procedimiento ordinario");
    }

    @Test
    void contextualPlatformReplyShouldAppendClientEventsAndProceduresForTimelineQuestion() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .recentEventSummaries(List.of("Providencia recibida"))
                .procedureTitles(List.of("Juicio verbal"))
                .build();
        when(this.chatbotQuestionClassifier.classify("Que eventos tiene este caso"))
                .thenReturn(PlatformQuestionType.TIMELINE_EVENTS);

        String reply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                "Que eventos tiene este caso",
                this.conversation("conversation-client-timeline"),
                platformContext
        );

        assertThat(reply).contains("Providencia recibida");
        assertThat(reply).contains("Juicio verbal");
    }

    @Test
    void contextualPlatformReplyShouldReturnProfessionalNoEventsReplyWhenTimelineHasNoEvents() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder().build();
        when(this.chatbotQuestionClassifier.classify("Que eventos recientes hay"))
                .thenReturn(PlatformQuestionType.TIMELINE_EVENTS);

        String reply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                "Que eventos recientes hay",
                this.conversation("conversation-professional-no-events"),
                platformContext
        );

        assertThat(reply).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_NO_EVENTS_REPLY);
    }

    @Test
    void contextualPlatformReplyShouldBuildStatusReplyWithProcedures() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-STATUS")
                .ownerDisplayName("Laura")
                .procedureTitles(List.of("Recurso administrativo"))
                .build();
        when(this.chatbotQuestionClassifier.classify("Cual es el estado de este encargo"))
                .thenReturn(PlatformQuestionType.ENGAGEMENT_STATUS);

        String reply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                "Cual es el estado de este encargo",
                this.conversation("conversation-status"),
                platformContext
        );

        assertThat(reply).contains("EL-STATUS");
        assertThat(reply).contains("Laura");
        assertThat(reply).contains("Recurso administrativo");
    }

    @Test
    void contextualPlatformReplyShouldAppendVisibleDocumentsForDocumentsQuestion() {
        Conversation conversation = this.conversation("conversation-docs");
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .procedureTitles(List.of("Reclamacion civil"))
                .build();
        when(this.chatbotQuestionClassifier.classify("Que documentos veo en mi caso"))
                .thenReturn(PlatformQuestionType.DOCUMENTS);
        when(this.chatbotDocumentContextService.loadDocumentContext(conversation))
                .thenReturn(ChatbotDocumentContext.builder()
                        .available(true)
                        .authorizedSourceConfigured(true)
                        .visibleDocumentTitles(List.of("Contrato", "Poder"))
                        .build());

        String reply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                "Que documentos veo en mi caso",
                conversation,
                platformContext
        );

        assertThat(reply).contains(ChatbotResponseMessages.CLIENT_CONTEXTUAL_DOCUMENTS_STUB_REPLY);
        assertThat(reply).contains("Reclamacion civil");
        assertThat(reply).contains("Documentos visibles preparados");
        assertThat(reply).contains("Contrato");
        assertThat(reply).contains("Poder");
    }

    @Test
    void contextualPlatformReplyShouldReturnDocumentsStubWhenDocumentContextIsMissing() {
        Conversation conversation = this.conversation("conversation-docs-missing");
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .procedureTitles(List.of())
                .build();
        when(this.chatbotQuestionClassifier.classify("Que documentos hay"))
                .thenReturn(PlatformQuestionType.DOCUMENTS);
        when(this.chatbotDocumentContextService.loadDocumentContext(conversation)).thenReturn(null);

        String reply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                "Que documentos hay",
                conversation,
                platformContext
        );

        assertThat(reply).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_DOCUMENTS_STUB_REPLY);
    }

    @Test
    void contextualPlatformReplyShouldReturnDocumentsStubWhenVisibleDocumentTitlesAreEmpty() {
        Conversation conversation = this.conversation("conversation-docs-empty");
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder().build();
        when(this.chatbotQuestionClassifier.classify("Documentos disponibles"))
                .thenReturn(PlatformQuestionType.DOCUMENTS);
        when(this.chatbotDocumentContextService.loadDocumentContext(conversation))
                .thenReturn(ChatbotDocumentContext.builder()
                        .available(true)
                        .authorizedSourceConfigured(true)
                        .visibleDocumentTitles(List.of())
                        .build());

        String reply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                "Documentos disponibles",
                conversation,
                platformContext
        );

        assertThat(reply).isEqualTo(ChatbotResponseMessages.CLIENT_CONTEXTUAL_DOCUMENTS_STUB_REPLY);
    }

    @Test
    void contextualPlatformReplyShouldAppendProfessionalProceduresForDocumentsQuestion() {
        Conversation conversation = this.conversation("conversation-docs-professional-procedures");
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .procedureTitles(List.of("Apelacion"))
                .build();
        when(this.chatbotQuestionClassifier.classify("Documentos del expediente"))
                .thenReturn(PlatformQuestionType.DOCUMENTS);
        when(this.chatbotDocumentContextService.loadDocumentContext(conversation)).thenReturn(null);

        String reply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                "Documentos del expediente",
                conversation,
                platformContext
        );

        assertThat(reply).contains(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_DOCUMENTS_STUB_REPLY);
        assertThat(reply).contains("Apelacion");
    }

    @Test
    void contextualPlatformReplyShouldBuildGeneralContextReplyCombiningAvailableSections() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-555")
                .ownerDisplayName("Ana")
                .procedureTitles(List.of("Reclamacion civil"))
                .recentEventSummaries(List.of("Escrito presentado"))
                .legalTaskSummaries(List.of("Revisar documentacion"))
                .build();
        when(this.chatbotQuestionClassifier.classify("Dame un resumen del caso"))
                .thenReturn(PlatformQuestionType.GENERAL_CONTEXT);

        String reply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                "Dame un resumen del caso",
                this.conversation("conversation-3"),
                platformContext
        );

        assertThat(reply).contains(ChatbotResponseMessages.CLIENT_CONTEXTUAL_GENERAL_SUMMARY_REPLY);
        assertThat(reply).contains("EL-555");
        assertThat(reply).contains("Escrito presentado");
        assertThat(reply).contains("Revisar documentacion");
    }

    @Test
    void contextualPlatformReplyShouldBuildGeneralContextReplyWhenClassifierReturnsNull() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-GENERAL")
                .ownerDisplayName("Carlos")
                .recentEventSummaries(List.of())
                .legalTaskSummaries(List.of())
                .build();
        when(this.chatbotQuestionClassifier.classify("Dame informacion relacionada"))
                .thenReturn(null);

        String reply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                "Dame informacion relacionada",
                this.conversation("conversation-general-null"),
                platformContext
        );

        assertThat(reply).contains(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_GENERAL_SUMMARY_REPLY);
        assertThat(reply).contains("EL-GENERAL");
        assertThat(reply).contains("Carlos");
        assertThat(reply).contains(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_NO_EVENTS_REPLY);
    }

    @Test
    void contextualPlatformReplyShouldBuildProfessionalGeneralContextWithEvents() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-PRO")
                .ownerDisplayName("Marta")
                .recentEventSummaries(List.of("Resolucion notificada"))
                .legalTaskSummaries(List.of())
                .build();
        when(this.chatbotQuestionClassifier.classify("Resumen profesional del caso"))
                .thenReturn(PlatformQuestionType.GENERAL_CONTEXT);

        String reply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                "Resumen profesional del caso",
                this.conversation("conversation-professional-general"),
                platformContext
        );

        assertThat(reply).contains(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_GENERAL_SUMMARY_REPLY);
        assertThat(reply).contains("Resolucion notificada");
    }

    @Test
    void contextualPlatformReplyShouldBuildClientGeneralContextWithoutEvents() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-CLIENT")
                .ownerDisplayName("Diego")
                .build();
        when(this.chatbotQuestionClassifier.classify("Resumen sin eventos"))
                .thenReturn(PlatformQuestionType.GENERAL_CONTEXT);

        String reply = this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                "Resumen sin eventos",
                this.conversation("conversation-client-general-no-events"),
                platformContext
        );

        assertThat(reply).contains(ChatbotResponseMessages.CLIENT_CONTEXTUAL_GENERAL_SUMMARY_REPLY);
        assertThat(reply).contains(ChatbotResponseMessages.CLIENT_CONTEXTUAL_NO_EVENTS_REPLY);
    }

    private static Stream<String> courtesyMessages() {
        return Stream.of(
                "por favor revisalo",
                "te quiero ayudar",
                "te amo por ayudar",
                "buen dia",
                "buenas tardes",
                "hasta luego",
                "nos vemos"
        );
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

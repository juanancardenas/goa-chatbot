package es.upm.api.domain.services.basereply;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

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

    private Conversation conversation(String id) {
        return Conversation.builder()
                .id(id)
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type("CONTEXTUAL")
                .createdAt(LocalDateTime.of(2026, 5, 15, 10, 0))
                .build();
    }
}

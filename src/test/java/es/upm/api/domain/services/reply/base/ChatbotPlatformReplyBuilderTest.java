package es.upm.api.domain.services.reply.base;

import es.upm.api.domain.common.ChatbotResponseMessages;
import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.enums.PlatformQuestionType;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.platform.ChatbotDocumentContext;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.services.reply.context.ChatbotDocumentContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotPlatformReplyBuilderTest {

    @Mock
    private ChatbotDocumentContextService chatbotDocumentContextService;

    private ChatbotPlatformReplyBuilder chatbotPlatformReplyBuilder;

    @BeforeEach
    void setUp() {
        this.chatbotPlatformReplyBuilder = new ChatbotPlatformReplyBuilder(this.chatbotDocumentContextService);
    }

    @Test
    void contextualPlatformReplyShouldReturnLegalTasksListWhenTasksExist() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .legalTaskSummaries(List.of("Estudio de antecedentes", "Asesoramiento juridico"))
                .build();

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                this.conversation("conversation-1"),
                platformContext,
                PlatformQuestionType.LEGAL_TASKS
        );

        assertThat(reply)
                .contains("Tareas")
                .contains("Estudio de antecedentes")
                .contains("Asesoramiento juridico");
    }

    @Test
    void contextualPlatformReplyShouldReturnNoLegalTasksReplyWhenTasksAreMissing() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .legalTaskSummaries(List.of())
                .build();

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                this.conversation("conversation-no-tasks"),
                platformContext,
                PlatformQuestionType.LEGAL_TASKS
        );

        assertThat(reply).isEqualTo(ChatbotResponseMessages.CLIENT_CONTEXTUAL_NO_LEGAL_TASKS_REPLY);
    }

    @Test
    void contextualPlatformReplyShouldReturnProfessionalNoLegalTasksReplyWhenTasksAreMissing() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder().build();

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                this.conversation("conversation-no-professional-tasks"),
                platformContext,
                PlatformQuestionType.LEGAL_TASKS
        );

        assertThat(reply).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_NO_LEGAL_TASKS_REPLY);
    }

    @Test
    void contextualPlatformReplyShouldReturnNoEventsReplyWhenTimelineHasNoEventsOrProcedures() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .recentEventSummaries(List.of())
                .procedureTitles(List.of())
                .build();

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                this.conversation("conversation-2"),
                platformContext,
                PlatformQuestionType.TIMELINE_EVENTS
        );

        assertThat(reply).isEqualTo(ChatbotResponseMessages.CLIENT_CONTEXTUAL_NO_EVENTS_REPLY);
    }

    @Test
    void contextualPlatformReplyShouldAppendEventsAndProceduresForTimelineQuestion() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .recentEventSummaries(List.of("Demanda admitida", "Vista senalada"))
                .procedureTitles(List.of("Procedimiento ordinario"))
                .build();

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                this.conversation("conversation-timeline"),
                platformContext,
                PlatformQuestionType.TIMELINE_EVENTS
        );

        assertThat(reply)
                .contains("Demanda admitida")
                .contains("Vista senalada")
                .contains("Procedimiento ordinario");
    }

    @Test
    void contextualPlatformReplyShouldAppendClientEventsAndProceduresForTimelineQuestion() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .recentEventSummaries(List.of("Providencia recibida"))
                .procedureTitles(List.of("Juicio verbal"))
                .build();

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                this.conversation("conversation-client-timeline"),
                platformContext,
                PlatformQuestionType.TIMELINE_EVENTS
        );

        assertThat(reply)
                .contains("Providencia recibida")
                .contains("Juicio verbal");
    }

    @Test
    void contextualPlatformReplyShouldReturnProfessionalNoEventsReplyWhenTimelineHasNoEvents() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder().build();

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                this.conversation("conversation-professional-no-events"),
                platformContext,
                PlatformQuestionType.TIMELINE_EVENTS
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

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                this.conversation("conversation-status"),
                platformContext,
                PlatformQuestionType.ENGAGEMENT_STATUS
        );

        assertThat(reply)
                .contains("EL-STATUS")
                .contains("Laura")
                .contains("Recurso administrativo");
    }

    @Test
    void contextualPlatformReplyShouldBuildStatusReplyWithoutProceduresWhenProcedureTitlesAreEmpty() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-STATUS-EMPTY")
                .ownerDisplayName("Laura")
                .procedureTitles(List.of())
                .build();

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                this.conversation("conversation-status-empty-procedures"),
                platformContext,
                PlatformQuestionType.ENGAGEMENT_STATUS
        );

        assertThat(reply)
                .contains("EL-STATUS-EMPTY")
                .contains("Laura")
                .doesNotContain("Procedimientos");
    }

    @Test
    void contextualPlatformReplyShouldAppendVisibleDocumentsForDocumentsQuestion() {
        Conversation conversation = this.conversation("conversation-docs");
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .procedureTitles(List.of("Reclamacion civil"))
                .build();
        when(this.chatbotDocumentContextService.loadDocumentContext(conversation))
                .thenReturn(ChatbotDocumentContext.builder()
                        .available(true)
                        .authorizedSourceConfigured(true)
                        .visibleDocumentTitles(List.of("Contrato", "Poder"))
                        .build());

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                conversation,
                platformContext,
                PlatformQuestionType.DOCUMENTS
        );

        assertThat(reply)
                .contains(ChatbotResponseMessages.CLIENT_CONTEXTUAL_DOCUMENTS_STUB_REPLY)
                .contains("Reclamacion civil")
                .contains("Documentos visibles preparados")
                .contains("Contrato")
                .contains("Poder");
    }

    @Test
    void contextualPlatformReplyShouldReturnDocumentsStubWhenDocumentContextIsMissing() {
        Conversation conversation = this.conversation("conversation-docs-missing");
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .procedureTitles(List.of())
                .build();
        when(this.chatbotDocumentContextService.loadDocumentContext(conversation)).thenReturn(null);

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                conversation,
                platformContext,
                PlatformQuestionType.DOCUMENTS
        );

        assertThat(reply).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_DOCUMENTS_STUB_REPLY);
    }

    @Test
    void contextualPlatformReplyShouldReturnDocumentsStubWhenVisibleDocumentTitlesAreEmpty() {
        Conversation conversation = this.conversation("conversation-docs-empty");
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder().build();
        when(this.chatbotDocumentContextService.loadDocumentContext(conversation))
                .thenReturn(ChatbotDocumentContext.builder()
                        .available(true)
                        .authorizedSourceConfigured(true)
                        .visibleDocumentTitles(List.of())
                        .build());

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                conversation,
                platformContext,
                PlatformQuestionType.DOCUMENTS
        );

        assertThat(reply).isEqualTo(ChatbotResponseMessages.CLIENT_CONTEXTUAL_DOCUMENTS_STUB_REPLY);
    }

    @Test
    void contextualPlatformReplyShouldReturnDocumentsStubWhenVisibleDocumentTitlesAreNull() {
        Conversation conversation = this.conversation("conversation-docs-null-titles");
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder().build();
        when(this.chatbotDocumentContextService.loadDocumentContext(conversation))
                .thenReturn(ChatbotDocumentContext.builder()
                        .available(true)
                        .authorizedSourceConfigured(true)
                        .visibleDocumentTitles(null)
                        .build());

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                conversation,
                platformContext,
                PlatformQuestionType.DOCUMENTS
        );

        assertThat(reply).isEqualTo(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_DOCUMENTS_STUB_REPLY);
    }

    @Test
    void contextualPlatformReplyShouldAppendProfessionalProceduresForDocumentsQuestion() {
        Conversation conversation = this.conversation("conversation-docs-professional-procedures");
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .procedureTitles(List.of("Apelacion"))
                .build();
        when(this.chatbotDocumentContextService.loadDocumentContext(conversation)).thenReturn(null);

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                conversation,
                platformContext,
                PlatformQuestionType.DOCUMENTS
        );

        assertThat(reply)
                .contains(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_DOCUMENTS_STUB_REPLY)
                .contains("Apelacion");
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

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                this.conversation("conversation-3"),
                platformContext,
                PlatformQuestionType.GENERAL_CONTEXT
        );

        assertThat(reply)
                .contains(ChatbotResponseMessages.CLIENT_CONTEXTUAL_GENERAL_SUMMARY_REPLY)
                .contains("EL-555")
                .contains("Escrito presentado")
                .contains("Revisar documentacion");
    }

    @Test
    void contextualPlatformReplyShouldBuildGeneralContextReplyWithoutEvents() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-GENERAL")
                .ownerDisplayName("Carlos")
                .recentEventSummaries(List.of())
                .legalTaskSummaries(List.of())
                .build();

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                this.conversation("conversation-general-null"),
                platformContext,
                PlatformQuestionType.GENERAL_CONTEXT
        );

        assertThat(reply)
                .contains(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_GENERAL_SUMMARY_REPLY)
                .contains("EL-GENERAL")
                .contains("Carlos")
                .contains(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_NO_EVENTS_REPLY);
    }

    @Test
    void contextualPlatformReplyShouldBuildProfessionalGeneralContextWithEvents() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-PRO")
                .ownerDisplayName("Marta")
                .recentEventSummaries(List.of("Resolucion notificada"))
                .legalTaskSummaries(List.of())
                .build();

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.PROFESSIONAL,
                this.conversation("conversation-professional-general"),
                platformContext,
                PlatformQuestionType.GENERAL_CONTEXT
        );

        assertThat(reply)
                .contains(ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_GENERAL_SUMMARY_REPLY)
                .contains("Resolucion notificada");
    }

    @Test
    void contextualPlatformReplyShouldBuildClientGeneralContextWithoutEvents() {
        ChatbotPlatformContext platformContext = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-CLIENT")
                .ownerDisplayName("Diego")
                .build();

        String reply = this.chatbotPlatformReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                this.conversation("conversation-client-general-no-events"),
                platformContext,
                PlatformQuestionType.GENERAL_CONTEXT
        );

        assertThat(reply)
                .contains(ChatbotResponseMessages.CLIENT_CONTEXTUAL_GENERAL_SUMMARY_REPLY)
                .contains(ChatbotResponseMessages.CLIENT_CONTEXTUAL_NO_EVENTS_REPLY);
    }

    private Conversation conversation(String id) {
        return Conversation.builder()
                .id(id)
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 15, 10, 0))
                .build();
    }
}

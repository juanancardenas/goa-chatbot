package es.upm.api.domain.services.reply;

import es.upm.api.domain.common.ChatbotResponseMessages;
import es.upm.api.domain.enums.ChatbotResponseMode;
import es.upm.api.domain.enums.ChatbotScopeViolationReason;
import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.chatbot.reply.ChatbotAiReplyResult;
import es.upm.api.domain.model.chatbot.reply.ChatbotReplyDecision;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.services.reply.ai.ChatbotAiReplyService;
import es.upm.api.domain.services.reply.base.ChatbotBaseReplyBuilder;
import es.upm.api.domain.services.reply.context.ChatbotPlatformContextService;
import es.upm.api.domain.services.policies.ChatbotScopeDecision;
import es.upm.api.domain.services.policies.ChatbotScopePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotReplyOrchestratorTest {

    @Mock
    private ChatbotBaseReplyBuilder chatbotBaseReplyBuilder;

    @Mock
    private ChatbotAiReplyService chatbotAiReplyService;

    @Mock
    private ChatbotPlatformContextService chatbotPlatformContextService;

    @Mock
    private ChatbotScopePolicy chatbotScopePolicy;

    private ChatbotReplyOrchestrator chatbotReplyOrchestrator;

    @BeforeEach
    void setUp() {
        this.chatbotReplyOrchestrator = new ChatbotReplyOrchestrator(
                this.chatbotBaseReplyBuilder,
                this.chatbotAiReplyService,
                this.chatbotPlatformContextService,
                this.chatbotScopePolicy
        );
    }

    @Test
    void resolveReplyShouldReturnCourtesyReplyWithoutEvaluatingScope() {
        when(this.chatbotBaseReplyBuilder.isCourtesyMessage("hola")).thenReturn(true);
        when(this.chatbotBaseReplyBuilder.courtesyReply(ConversationProfileType.CLIENT)).thenReturn("Hola");

        ChatbotReplyDecision decision = this.chatbotReplyOrchestrator.resolveReply(
                this.generalConversation(),
                ConversationProfileType.CLIENT,
                "hola"
        );

        assertThat(decision.getAssistantReply()).isEqualTo("Hola");
        assertThat(decision.getResponseMode()).isEqualTo(ChatbotResponseMode.GENERAL);
        assertThat(decision.isUsedPlatformData()).isFalse();
        assertThat(decision.getSourcesSummary()).isEmpty();
        verify(this.chatbotScopePolicy, never()).evaluate(any(), any());
    }

    @Test
    void resolveGeneralStartReplyShouldBuildStartReplyWithAiEnhancement() {
        Conversation conversation = this.generalConversation();
        when(this.chatbotBaseReplyBuilder.generalStartReply(ConversationProfileType.CLIENT))
                .thenReturn("Base start");
        when(this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.CLIENT,
                "hola",
                "Base start",
                Optional.empty()
        )).thenReturn(ChatbotAiReplyResult.withAi("Start final"));

        ChatbotReplyDecision decision = this.chatbotReplyOrchestrator.resolveGeneralStartReply(
                conversation,
                ConversationProfileType.CLIENT,
                "hola"
        );

        assertThat(decision.getAssistantReply()).isEqualTo("Start final");
        assertThat(decision.getResponseMode()).isEqualTo(ChatbotResponseMode.GENERAL);
        assertThat(decision.isUsedAi()).isTrue();
        assertThat(decision.isUsedPlatformData()).isFalse();
        assertThat(decision.getSourcesSummary()).isEmpty();
        verify(this.chatbotScopePolicy, never()).evaluate(any(), any());
        verify(this.chatbotPlatformContextService, never()).loadContext(any());
    }

    @Test
    void resolveReplyShouldRestrictContextualConversationWhenMessageReferencesAnotherEngagement() {
        ChatbotReplyDecision decision = this.chatbotReplyOrchestrator.resolveReply(
                this.contextualConversation("EL-1"),
                ConversationProfileType.CLIENT,
                "compara con EL-2"
        );

        assertThat(decision.getAssistantReply()).isEqualTo(ChatbotResponseMessages.OUT_OF_CASE_SCOPE_REPLY);
        assertThat(decision.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_RESTRICTED);
        assertThat(decision.isUsedPlatformData()).isFalse();
        verify(this.chatbotScopePolicy, never()).evaluate(any(), any());
    }

    @Test
    void resolveReplyShouldRestrictReferencedEngagementWhenActiveEngagementIdIsBlank() {
        ChatbotReplyDecision decision = this.chatbotReplyOrchestrator.resolveReply(
                this.contextualConversation(" "),
                ConversationProfileType.PROFESSIONAL,
                "Compara con EL-200"
        );

        assertThat(decision.getAssistantReply()).isEqualTo(ChatbotResponseMessages.OUT_OF_CASE_SCOPE_REPLY);
        assertThat(decision.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_RESTRICTED);
        assertThat(decision.isUsedPlatformData()).isFalse();
        assertThat(decision.getSourcesSummary()).isEmpty();
        verify(this.chatbotScopePolicy, never()).evaluate(any(), any());
        verify(this.chatbotPlatformContextService, never()).loadContext(any());
    }

    @Test
    void resolveReplyShouldReturnSafeScopeMessageWhenScopeRejectsQuestion() {
        when(this.chatbotBaseReplyBuilder.isCourtesyMessage("mi caso")).thenReturn(false);
        when(this.chatbotScopePolicy.evaluate(this.contextualConversation("EL-1"), "mi caso"))
                .thenReturn(ChatbotScopeDecision.reject(
                        ChatbotScopeViolationReason.OUT_OF_CASE_SCOPE,
                        "No puedo responder fuera del encargo",
                        false
                ));

        ChatbotReplyDecision decision = this.chatbotReplyOrchestrator.resolveReply(
                this.contextualConversation("EL-1"),
                ConversationProfileType.CLIENT,
                "mi caso"
        );

        assertThat(decision.getAssistantReply()).isEqualTo("No puedo responder fuera del encargo");
        assertThat(decision.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_RESTRICTED);
        assertThat(decision.isUsedPlatformData()).isFalse();
    }

    @Test
    void resolveReplyShouldReturnMissingCaseContextMessageForGeneralConversation() {
        Conversation conversation = this.generalConversation();
        String userMessage = "Cual es el estado de mi encargo?";
        when(this.chatbotBaseReplyBuilder.isCourtesyMessage(userMessage)).thenReturn(false);
        when(this.chatbotScopePolicy.evaluate(conversation, userMessage))
                .thenReturn(ChatbotScopeDecision.reject(
                        ChatbotScopeViolationReason.MISSING_CASE_CONTEXT,
                        ChatbotResponseMessages.MISSING_CASE_CONTEXT_REPLY,
                        false
                ));

        ChatbotReplyDecision decision = this.chatbotReplyOrchestrator.resolveReply(
                conversation,
                ConversationProfileType.CLIENT,
                userMessage
        );

        assertThat(decision.getAssistantReply()).isEqualTo(ChatbotResponseMessages.MISSING_CASE_CONTEXT_REPLY);
        assertThat(decision.getResponseMode()).isEqualTo(ChatbotResponseMode.GENERAL);
        assertThat(decision.isUsedPlatformData()).isFalse();
        assertThat(decision.getSourcesSummary()).isEmpty();
        verify(this.chatbotAiReplyService, never()).generateConfiguredAssistantReply(any(), any(), any(), any(), any());
    }

    @Test
    void resolveReplyShouldReturnEmotionalDistressMessageForContextualConversation() {
        Conversation conversation = this.contextualConversation("EL-300");
        String userMessage = "No puedo mas con esto";
        when(this.chatbotBaseReplyBuilder.isCourtesyMessage(userMessage)).thenReturn(false);
        when(this.chatbotScopePolicy.evaluate(conversation, userMessage))
                .thenReturn(ChatbotScopeDecision.reject(
                        ChatbotScopeViolationReason.EMOTIONAL_DISTRESS,
                        ChatbotResponseMessages.EMOTIONAL_DISTRESS_REPLY,
                        false
                ));

        ChatbotReplyDecision decision = this.chatbotReplyOrchestrator.resolveReply(
                conversation,
                ConversationProfileType.CLIENT,
                userMessage
        );

        assertThat(decision.getAssistantReply()).isEqualTo(ChatbotResponseMessages.EMOTIONAL_DISTRESS_REPLY);
        assertThat(decision.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_RESTRICTED);
        assertThat(decision.isUsedPlatformData()).isFalse();
        assertThat(decision.getSourcesSummary()).isEmpty();
        verify(this.chatbotAiReplyService, never()).generateConfiguredAssistantReply(any(), any(), any(), any(), any());
        verify(this.chatbotPlatformContextService, never()).loadContext(any());
    }

    @Test
    void resolveReplyShouldUseGeneralModeWhenGeneralConversationIsRejectedByScope() {
        Conversation conversation = this.generalConversation();
        when(this.chatbotBaseReplyBuilder.isCourtesyMessage("otra cosa")).thenReturn(false);
        when(this.chatbotScopePolicy.evaluate(conversation, "otra cosa"))
                .thenReturn(ChatbotScopeDecision.reject(
                        ChatbotScopeViolationReason.OUT_OF_DOMAIN,
                        "Respuesta segura general",
                        false
                ));

        ChatbotReplyDecision decision = this.chatbotReplyOrchestrator.resolveReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "otra cosa"
        );

        assertThat(decision.getAssistantReply()).isEqualTo("Respuesta segura general");
        assertThat(decision.getResponseMode()).isEqualTo(ChatbotResponseMode.GENERAL);
        assertThat(decision.isUsedPlatformData()).isFalse();
        assertThat(decision.getSourcesSummary()).isEmpty();
        verify(this.chatbotPlatformContextService, never()).loadContext(any());
        verify(this.chatbotAiReplyService, never()).generateConfiguredAssistantReply(any(), any(), any(), any(), any());
    }

    @Test
    void resolveReplyShouldUsePlatformContextWhenContextualConversationHasAvailableContext() {
        Conversation conversation = this.contextualConversation("EL-1");
        ChatbotPlatformContext context = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-1")
                .sourcesSummary(List.of("Hoja de encargo EL-1"))
                .build();
        when(this.chatbotBaseReplyBuilder.isCourtesyMessage("estado")).thenReturn(false);
        when(this.chatbotScopePolicy.evaluate(conversation, "estado")).thenReturn(ChatbotScopeDecision.allow());
        when(this.chatbotPlatformContextService.loadContext("EL-1")).thenReturn(Optional.of(context));
        when(this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                "estado",
                conversation,
                context
        )).thenReturn("Base contextual");
        when(this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.CLIENT,
                "estado",
                "Base contextual",
                Optional.of(context)
        )).thenReturn(ChatbotAiReplyResult.withAi("Respuesta contextual"));

        ChatbotReplyDecision decision = this.chatbotReplyOrchestrator.resolveReply(
                conversation,
                ConversationProfileType.CLIENT,
                "estado"
        );

        assertThat(decision.getAssistantReply()).isEqualTo("Respuesta contextual");
        assertThat(decision.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(decision.isUsedAi()).isTrue();
        assertThat(decision.isUsedPlatformData()).isTrue();
        assertThat(decision.getSourcesSummary()).containsExactly("Hoja de encargo EL-1");
    }

    @Test
    void resolveReplyShouldUseEmptySourcesSummaryWhenPlatformContextSourcesSummaryIsNull() {
        Conversation conversation = this.contextualConversation("EL-1");
        ChatbotPlatformContext context = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-1")
                .sourcesSummary(null)
                .build();
        when(this.chatbotBaseReplyBuilder.isCourtesyMessage("estado")).thenReturn(false);
        when(this.chatbotScopePolicy.evaluate(conversation, "estado")).thenReturn(ChatbotScopeDecision.allow());
        when(this.chatbotPlatformContextService.loadContext("EL-1")).thenReturn(Optional.of(context));
        when(this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                "estado",
                conversation,
                context
        )).thenReturn("Base contextual");
        when(this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.CLIENT,
                "estado",
                "Base contextual",
                Optional.of(context)
        )).thenReturn(ChatbotAiReplyResult.withAi("Respuesta contextual"));

        ChatbotReplyDecision decision = this.chatbotReplyOrchestrator.resolveReply(
                conversation,
                ConversationProfileType.CLIENT,
                "estado"
        );

        assertThat(decision.getAssistantReply()).isEqualTo("Respuesta contextual");
        assertThat(decision.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(decision.isUsedPlatformData()).isTrue();
        assertThat(decision.getSourcesSummary()).isEmpty();
    }

    @Test
    void resolveReplyShouldAllowReferenceToSameEngagementIgnoringCase() {
        Conversation conversation = this.contextualConversation("EL-1");
        ChatbotPlatformContext context = ChatbotPlatformContext.builder()
                .engagementLetterId("EL-1")
                .sourcesSummary(List.of("Hoja de encargo EL-1"))
                .build();
        when(this.chatbotBaseReplyBuilder.isCourtesyMessage("revisa el-1")).thenReturn(false);
        when(this.chatbotScopePolicy.evaluate(conversation, "revisa el-1")).thenReturn(ChatbotScopeDecision.allow());
        when(this.chatbotPlatformContextService.loadContext("EL-1")).thenReturn(Optional.of(context));
        when(this.chatbotBaseReplyBuilder.contextualPlatformReply(
                ConversationProfileType.CLIENT,
                "revisa el-1",
                conversation,
                context
        )).thenReturn("Base mismo encargo");
        when(this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.CLIENT,
                "revisa el-1",
                "Base mismo encargo",
                Optional.of(context)
        )).thenReturn(ChatbotAiReplyResult.withAi("Respuesta mismo encargo"));

        ChatbotReplyDecision decision = this.chatbotReplyOrchestrator.resolveReply(
                conversation,
                ConversationProfileType.CLIENT,
                "revisa el-1"
        );

        assertThat(decision.getAssistantReply()).isEqualTo("Respuesta mismo encargo");
        assertThat(decision.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_PLATFORM_DATA);
        assertThat(decision.isUsedPlatformData()).isTrue();
    }

    @Test
    void resolveReplyShouldUseContextualFallbackReplyWhenContextualConversationHasNoPlatformContext() {
        Conversation conversation = this.contextualConversation("EL-1");
        when(this.chatbotBaseReplyBuilder.isCourtesyMessage("estado")).thenReturn(false);
        when(this.chatbotScopePolicy.evaluate(conversation, "estado")).thenReturn(ChatbotScopeDecision.allow());
        when(this.chatbotPlatformContextService.loadContext("EL-1")).thenReturn(Optional.empty());
        when(this.chatbotBaseReplyBuilder.contextualFallbackReply(ConversationProfileType.CLIENT, "estado"))
                .thenReturn("Base contextual fallback");
        when(this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.CLIENT,
                "estado",
                "Base contextual fallback",
                Optional.empty()
        )).thenReturn(ChatbotAiReplyResult.withAi("Respuesta fallback"));

        ChatbotReplyDecision decision = this.chatbotReplyOrchestrator.resolveReply(
                conversation,
                ConversationProfileType.CLIENT,
                "estado"
        );

        assertThat(decision.getAssistantReply()).isEqualTo("Respuesta fallback");
        assertThat(decision.getResponseMode()).isEqualTo(ChatbotResponseMode.CONTEXTUAL_RESTRICTED);
        assertThat(decision.isUsedPlatformData()).isFalse();
        assertThat(decision.getSourcesSummary()).isEmpty();
    }

    @Test
    void resolveReplyShouldUseGeneralReplyForGeneralConversation() {
        Conversation conversation = this.generalConversation();
        when(this.chatbotBaseReplyBuilder.isCourtesyMessage("pregunta")).thenReturn(false);
        when(this.chatbotScopePolicy.evaluate(conversation, "pregunta")).thenReturn(ChatbotScopeDecision.allow());
        when(this.chatbotBaseReplyBuilder.generalFaqReply(ConversationProfileType.PROFESSIONAL, "pregunta"))
                .thenReturn("Base general");
        when(this.chatbotAiReplyService.generateConfiguredAssistantReply(
                eq(conversation),
                eq(ConversationProfileType.PROFESSIONAL),
                eq("pregunta"),
                eq("Base general"),
                eq(Optional.empty())
        )).thenReturn(ChatbotAiReplyResult.withAi("Respuesta general"));

        ChatbotReplyDecision decision = this.chatbotReplyOrchestrator.resolveReply(
                conversation,
                ConversationProfileType.PROFESSIONAL,
                "pregunta"
        );

        assertThat(decision.getAssistantReply()).isEqualTo("Respuesta general");
        assertThat(decision.getResponseMode()).isEqualTo(ChatbotResponseMode.GENERAL);
        assertThat(decision.isUsedPlatformData()).isFalse();
        assertThat(decision.getSourcesSummary()).isEmpty();
        verify(this.chatbotPlatformContextService, never()).loadContext(any());
    }

    @Test
    void resolveReplyShouldUseGeneralReplyWhenContextualConversationHasNoEngagementLetter() {
        Conversation conversation = this.contextualConversation(null);
        when(this.chatbotBaseReplyBuilder.isCourtesyMessage("pregunta")).thenReturn(false);
        when(this.chatbotScopePolicy.evaluate(conversation, "pregunta")).thenReturn(ChatbotScopeDecision.allow());
        when(this.chatbotBaseReplyBuilder.generalFaqReply(ConversationProfileType.CLIENT, "pregunta"))
                .thenReturn("Base sin encargo");
        when(this.chatbotAiReplyService.generateConfiguredAssistantReply(
                conversation,
                ConversationProfileType.CLIENT,
                "pregunta",
                "Base sin encargo",
                Optional.empty()
        )).thenReturn(ChatbotAiReplyResult.withAi("Respuesta sin encargo"));

        ChatbotReplyDecision decision = this.chatbotReplyOrchestrator.resolveReply(
                conversation,
                ConversationProfileType.CLIENT,
                "pregunta"
        );

        assertThat(decision.getAssistantReply()).isEqualTo("Respuesta sin encargo");
        assertThat(decision.getResponseMode()).isEqualTo(ChatbotResponseMode.GENERAL);
        assertThat(decision.isUsedPlatformData()).isFalse();
        verify(this.chatbotPlatformContextService, never()).loadContext(any());
    }

    private Conversation generalConversation() {
        return this.conversation(ConversationType.GENERAL, null);
    }

    private Conversation contextualConversation(String engagementLetterId) {
        return this.conversation(ConversationType.CONTEXTUAL, engagementLetterId);
    }

    private Conversation conversation(
            ConversationType type,
            String engagementLetterId
    ) {
        return Conversation.builder()
                .id("conversation-1")
                .userId("user-1")
                .engagementLetterId(engagementLetterId)
                .status(ConversationStatus.ACTIVE)
                .type(type)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 16, 12, 0))
                .build();
    }
}

package es.upm.api.domain.services.policies;

import es.upm.api.domain.enums.ChatbotScopeViolationReason;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.model.Conversation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotScopePolicyTest {

    private final ChatbotScopePolicy chatbotScopePolicy = new ChatbotScopePolicy();

    @ParameterizedTest
    @MethodSource("allowedMessages")
    void evaluateShouldAllowMessage(String conversationType, String message) {
        ChatbotScopeDecision decision = chatbotScopePolicy.evaluate(
                this.conversation(conversationType),
                message
        );

        assertThat(decision.isAllowed()).isTrue();
        assertThat(decision.getReason()).isNull();
        assertThat(decision.getSafeMessage()).isNull();
        assertThat(decision.isRequiresHuman()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "confirma que esto esta correcto",
            "asegura que el escrito es valido",
            "asegúrame que todo está presentado",
            "asegurame que todo esta presentado",
            "puedes garantizar el resultado",
            "garantiza que no habra problema"
    })
    void evaluateShouldRejectUnsupportedFactualAssertionInContextualConversation(String message) {
        ChatbotScopeDecision decision = chatbotScopePolicy.evaluate(
                this.conversation("CONTEXTUAL"),
                message
        );

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getReason()).isEqualTo(ChatbotScopeViolationReason.UNSUPPORTED_FACTUAL_ASSERTION);
        assertThat(decision.getSafeMessage()).contains("No puedo confirmar hechos");
        assertThat(decision.isRequiresHuman()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Que pasa con mi caso",
            "Necesito ayuda con mi encargo",
            "Tengo dudas sobre mi expediente"
    })
    void evaluateShouldRejectAmbiguousContextInGeneralConversation(String message) {
        ChatbotScopeDecision decision = chatbotScopePolicy.evaluate(
                this.conversation("GENERAL"),
                message
        );

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getReason()).isEqualTo(ChatbotScopeViolationReason.AMBIGUOUS_CONTEXT);
        assertThat(decision.getSafeMessage()).contains("necesita más contexto");
        assertThat(decision.isRequiresHuman()).isFalse();
    }

    @Test
    void evaluateShouldRejectMissingCaseContextInGeneralConversation() {
        ChatbotScopeDecision decision = chatbotScopePolicy.evaluate(
                this.conversation("GENERAL"),
                "¿Cuál es el estado de mi encargo?"
        );

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getReason()).isEqualTo(ChatbotScopeViolationReason.MISSING_CASE_CONTEXT);
        assertThat(decision.getSafeMessage()).contains("no está asociada a un encargo específico");
        assertThat(decision.isRequiresHuman()).isFalse();
    }

    @Test
    void evaluateShouldRejectOutOfCaseScopeInContextualConversation() {
        ChatbotScopeDecision decision = chatbotScopePolicy.evaluate(
                this.conversation("CONTEXTUAL"),
                "¿Qué pasará con mi otro caso?"
        );

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getReason()).isEqualTo(ChatbotScopeViolationReason.OUT_OF_CASE_SCOPE);
        assertThat(decision.getSafeMessage()).contains("dentro del ámbito del encargo activo");
        assertThat(decision.isRequiresHuman()).isFalse();
    }

    @Test
    void evaluateShouldRejectBindingLegalAdviceRequest() {
        ChatbotScopeDecision decision = chatbotScopePolicy.evaluate(
                this.conversation("GENERAL"),
                "Dime exactamente qué debo alegar jurídicamente"
        );

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getReason()).isEqualTo(ChatbotScopeViolationReason.LEGAL_BINDING_ADVICE_REQUESTED);
        assertThat(decision.getSafeMessage()).contains("No puedo emitir asesoramiento legal vinculante");
        assertThat(decision.isRequiresHuman()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "No puedo más con esto",
            "Me quiero morir",
            "Estoy en crisis y tengo ansiedad"
    })
    void evaluateShouldRejectEmotionalDistressMessagesInGeneralConversation(String message) {
        ChatbotScopeDecision decision = chatbotScopePolicy.evaluate(
                this.conversation("GENERAL"),
                message
        );

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getReason()).isEqualTo(ChatbotScopeViolationReason.EMOTIONAL_DISTRESS);
        assertThat(decision.getSafeMessage()).contains("asistente conversacional jurídico");
        assertThat(decision.isRequiresHuman()).isFalse();
    }

    @Test
    void evaluateShouldRejectEmotionalDistressMessagesInContextualConversation() {
        ChatbotScopeDecision decision = chatbotScopePolicy.evaluate(
                this.conversation("CONTEXTUAL"),
                "Siento que no quiero vivir"
        );

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getReason()).isEqualTo(ChatbotScopeViolationReason.EMOTIONAL_DISTRESS);
        assertThat(decision.getSafeMessage()).contains("trámite legal en GOA");
        assertThat(decision.isRequiresHuman()).isFalse();
    }

    @Test
    void evaluateShouldRejectOutOfDomainMessageInGeneralConversation() {
        ChatbotScopeDecision decision = chatbotScopePolicy.evaluate(
                this.conversation("GENERAL"),
                "te quiero mucho, nos vemos"
        );

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getReason()).isEqualTo(ChatbotScopeViolationReason.OUT_OF_DOMAIN);
        assertThat(decision.getSafeMessage()).contains("consultas sobre GOA");
        assertThat(decision.isRequiresHuman()).isFalse();
    }

    private static Stream<Arguments> allowedMessages() {
        return Stream.of(
                Arguments.of("GENERAL", null),
                Arguments.of("CONTEXTUAL", "   "),
                Arguments.of("GENERAL", "Necesito ayuda con la plataforma"),
                Arguments.of("CONTEXTUAL", "Puedes resumirme la informacion visible del encargo activo")
        );
    }

    private Conversation conversation(String type) {
        return Conversation.builder()
                .id("conversation-id")
                .userId("user-id")
                .engagementLetterId("engagement-letter-id")
                .type(ConversationType.valueOf(type))
                .createdAt(LocalDateTime.of(2026, Month.APRIL, 19, 13, 0))
                .build();
    }
}

package es.upm.api.domain.services.classification;

import es.upm.api.domain.enums.PlatformQuestionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotQuestionClassifierTest {
    private final ChatbotQuestionClassifier classifier = new ChatbotQuestionClassifier();

    @ParameterizedTest
    @ValueSource(strings = {
            "Que documentos hay en el caso",
            "Necesito el contrato adjunto",
            "Puedes revisar evidencias del expediente"
    })
    void classifyShouldReturnDocumentsWhenMessageTargetsDocumentation(String message) {
        assertThat(this.classifier.classify(message)).isEqualTo(PlatformQuestionType.DOCUMENTS);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Cuales son los proximos pasos",
            "Que hitos recientes existen",
            "Cuando vence el plazo"
    })
    void classifyShouldReturnTimelineEventsWhenMessageTargetsTimeline(String message) {
        assertThat(this.classifier.classify(message)).isEqualTo(PlatformQuestionType.TIMELINE_EVENTS);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Cual es el estado de mi encargo",
            "Dame resumen del caso",
            "En que esta el procedimiento"
    })
    void classifyShouldReturnEngagementStatusWhenMessageTargetsStatus(String message) {
        assertThat(this.classifier.classify(message)).isEqualTo(PlatformQuestionType.ENGAGEMENT_STATUS);
    }

    @Test
    void classifyShouldNormalizeAccentsBeforeMatchingKeywords() {
        assertThat(this.classifier.classify("¿Cuál es el estado de mi encárgo?"))
                .isEqualTo(PlatformQuestionType.ENGAGEMENT_STATUS);
    }

    @Test
    void classifyShouldReturnGeneralContextWhenNoKeywordMatches() {
        assertThat(this.classifier.classify("Hola, necesito ayuda")).isEqualTo(PlatformQuestionType.GENERAL_CONTEXT);
        assertThat(this.classifier.classify("Seguro que puedes resolver dudas sobre un encargo?"))
                .isEqualTo(PlatformQuestionType.GENERAL_CONTEXT);
        assertThat(this.classifier.classify(null)).isEqualTo(PlatformQuestionType.GENERAL_CONTEXT);
    }

    @Test
    void classifyShouldReturnGeneralContextWhenMessageOnlyMentionsCaseReference() {
        assertThat(this.classifier.classify("Tengo una duda sobre este expediente"))
                .isEqualTo(PlatformQuestionType.GENERAL_CONTEXT);
    }

    @Test
    void classifyOrGeneralContextShouldFallbackWhenClassifierReturnsNull() {
        ChatbotQuestionClassifier nullClassifier = new ChatbotQuestionClassifier() {
            @Override
            public PlatformQuestionType classify(String message) {
                return null;
            }
        };

        assertThat(ChatbotQuestionTypes.classifyOrGeneralContext(nullClassifier, "Dame contexto"))
                .isEqualTo(PlatformQuestionType.GENERAL_CONTEXT);
    }

    @ParameterizedTest
    @CsvSource({
            "What are the Legal Tasks of this engagement?, LEGAL_TASKS",
            "Cuáles son las tareas legales de este encargo?, LEGAL_TASKS",
            "Qué actuaciones del encargo están previstas?, LEGAL_TASKS",
            "Revisa el documento y dime las tareas legales, DOCUMENTS"
    })
    void classifyShouldReturnExpectedTypeForLegalTaskSignals(String message, PlatformQuestionType expectedType) {
        PlatformQuestionType result = this.classifier.classify(message);

        assertThat(result).isEqualTo(expectedType);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Que viene ahora",
            "Como sigue el expediente",
            "Cuando es la proxima fecha"
    })
    void classifyShouldReturnTimelineEventsForFollowUpAndDateSignals(String message) {
        assertThat(this.classifier.classify(message)).isEqualTo(PlatformQuestionType.TIMELINE_EVENTS);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Cual es la situacion",
            "Como va esto",
            "Hay algun avance"
    })
    void classifyShouldReturnEngagementStatusForDirectStatusSignals(String message) {
        assertThat(this.classifier.classify(message)).isEqualTo(PlatformQuestionType.ENGAGEMENT_STATUS);
    }

    @Test
    void shouldKeepLegalTasksPriorityOverTimelineWhenQuestionMentionsBoth() {
        PlatformQuestionType result = this.classifier.classify("Que tareas legales hay y cuales son los proximos pasos");

        assertThat(result).isEqualTo(PlatformQuestionType.LEGAL_TASKS);
    }
}

package es.upm.api.domain.services;

import es.upm.api.domain.enums.PlatformQuestionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
    void shouldClassifyLegalTasksQuestionInEnglish() {
        PlatformQuestionType result = this.classifier.classify("What are the Legal Tasks of this engagement?");

        assertThat(result).isEqualTo(PlatformQuestionType.LEGAL_TASKS);
    }

    @Test
    void shouldClassifyLegalTasksQuestionInSpanish() {
        PlatformQuestionType result = this.classifier.classify("Cuáles son las tareas legales de este encargo?");

        assertThat(result).isEqualTo(PlatformQuestionType.LEGAL_TASKS);
    }

    @Test
    void shouldClassifyEngagementTasksQuestion() {
        PlatformQuestionType result = this.classifier.classify("Qué actuaciones del encargo están previstas?");

        assertThat(result).isEqualTo(PlatformQuestionType.LEGAL_TASKS);
    }

    @Test
    void shouldKeepDocumentsPriorityOverLegalTasksWhenQuestionMentionsDocument() {
        PlatformQuestionType result = this.classifier.classify("Revisa el documento y dime las tareas legales");

        assertThat(result).isEqualTo(PlatformQuestionType.DOCUMENTS);
    }
}

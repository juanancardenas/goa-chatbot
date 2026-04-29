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
        assertThat(this.classifier.classify(null)).isEqualTo(PlatformQuestionType.GENERAL_CONTEXT);
    }
}

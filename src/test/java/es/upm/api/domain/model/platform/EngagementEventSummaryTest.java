package es.upm.api.domain.model.platform;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EngagementEventSummaryTest {

    @Test
    void displayTextShouldUseTrimmedTitleTypeAndState() {
        EngagementEventSummary summary = new EngagementEventSummary(
                "  reunion  ",
                "  completado  ",
                "  Revisión final  ",
                "comentario",
                LocalDate.of(2026, 5, 3)
        );

        assertThat(summary.displayText()).isEqualTo("Revisión final [reunion] - completado");
    }

    @Test
    void displayTextShouldUseFallbackTitleAndSkipBlankTypeAndState() {
        EngagementEventSummary summary = new EngagementEventSummary(
                "   ",
                null,
                " ",
                null,
                LocalDate.of(2026, 5, 4)
        );

        assertThat(summary.displayText()).isEqualTo("Evento sin t\u00edtulo");
    }

    @Test
    void displayTextShouldAppendOnlyTypeWhenStateIsBlank() {
        EngagementEventSummary summary = new EngagementEventSummary(
                "  EVENT  ",
                "   ",
                "  Vista inicial  ",
                null,
                LocalDate.of(2026, 5, 5)
        );

        assertThat(summary.displayText()).isEqualTo("Vista inicial [EVENT]");
    }

    @Test
    void displayTextShouldAppendOnlyStateWhenTypeIsBlank() {
        EngagementEventSummary summary = new EngagementEventSummary(
                "   ",
                "  OPEN  ",
                "  Vista inicial  ",
                null,
                LocalDate.of(2026, 5, 6)
        );

        assertThat(summary.displayText()).isEqualTo("Vista inicial - OPEN");
    }

    @Test
    void displayTextShouldUseFallbackTitleWhenTitleIsNull() {
        EngagementEventSummary summary = new EngagementEventSummary(
                "  MILESTONE  ",
                "  DONE  ",
                null,
                null,
                LocalDate.of(2026, 5, 7)
        );

        assertThat(summary.displayText()).isEqualTo("Evento sin t\u00edtulo [MILESTONE] - DONE");
    }
}

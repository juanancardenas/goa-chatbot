package es.upm.api.domain.model.platform;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class EngagementEventSummaryTest {

    @Test
    void displayTextShouldUseTrimmedTitleTypeAndState() {
        EngagementEventSummary summary = new EngagementEventSummary(
                "  reunion  ",
                "  completado  ",
                "  Revisión final  ",
                "comentario",
                LocalDate.of(2026, Month.MAY, 3)
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
                LocalDate.of(2026, Month.MAY, 4)
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
                LocalDate.of(2026, Month.MAY, 5)
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
                LocalDate.of(2026, Month.MAY, 6)
        );

        assertThat(summary.displayText()).isEqualTo("Vista inicial - OPEN");
    }

    @Test
    void displayTextShouldAppendOnlyStateWhenTypeIsNull() {
        EngagementEventSummary summary = new EngagementEventSummary(
                null,
                "  OPEN  ",
                "  Vista inicial  ",
                null,
                LocalDate.of(2026, Month.MAY, 6)
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
                LocalDate.of(2026, Month.MAY, 7)
        );

        assertThat(summary.displayText()).isEqualTo("Evento sin t\u00edtulo [MILESTONE] - DONE");
    }

    @Test
    void displayTextShouldIgnoreCommentAndDate() {
        EngagementEventSummary summary = new EngagementEventSummary(
                "EVENT",
                "DONE",
                "Resolucion recibida",
                "Comentario interno",
                LocalDate.of(2026, Month.MAY, 8)
        );

        assertThat(summary.displayText()).isEqualTo("Resolucion recibida [EVENT] - DONE");
        assertThat(summary.displayText()).doesNotContain("Comentario interno");
        assertThat(summary.displayText()).doesNotContain("2026");
    }

    @Test
    void noArgsConstructorShouldAllowSettingFieldsBeforeDisplayText() {
        EngagementEventSummary summary = new EngagementEventSummary();
        summary.setType(" TASK ");
        summary.setState(" OPEN ");
        summary.setTitle(" Preparar escrito ");
        summary.setComment("comentario");
        summary.setDate(LocalDate.of(2026, Month.MAY, 9));

        assertThat(summary.displayText()).isEqualTo("Preparar escrito [TASK] - OPEN");
    }
}

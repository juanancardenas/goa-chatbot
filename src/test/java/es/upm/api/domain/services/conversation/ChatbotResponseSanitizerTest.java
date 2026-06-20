package es.upm.api.domain.services.conversation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotResponseSanitizerTest {

    private final ChatbotResponseSanitizer chatbotResponseSanitizer = new ChatbotResponseSanitizer();

    @Test
    void normalizeReplyForFrontendShouldReturnNullAndBlankRepliesAsIs() {
        assertThat(this.chatbotResponseSanitizer.normalizeReplyForFrontend(null)).isNull();
        assertThat(this.chatbotResponseSanitizer.normalizeReplyForFrontend("   ")).isEqualTo("   ");
    }

    @Test
    void normalizeReplyForFrontendShouldKeepRepliesWithoutPipeCharacters() {
        String reply = """
                Respuesta normal
                con dos lineas
                """.trim();

        assertThat(this.chatbotResponseSanitizer.normalizeReplyForFrontend(reply)).isEqualTo(reply);
    }

    @Test
    void normalizeReplyForFrontendShouldKeepWhitespaceWhenReplyHasNoPipes() {
        String reply = "  Respuesta normal  " + System.lineSeparator() + "  con espacios  ";

        assertThat(this.chatbotResponseSanitizer.normalizeReplyForFrontend(reply)).isEqualTo(reply);
    }

    @Test
    void normalizeReplyForFrontendShouldConvertMarkdownTableIntoBulletList() {
        String reply = """
                | Documento | Estado |
                | --- | --- |
                | Contrato | Firmado |
                | Poder | Pendiente |
                """;

        assertThat(this.chatbotResponseSanitizer.normalizeReplyForFrontend(reply))
                .isEqualTo(String.join(
                        System.lineSeparator(),
                        "- Documento: Estado",
                        "- Contrato: Firmado",
                        "- Poder: Pendiente"
                ));
    }

    @Test
    void normalizeReplyForFrontendShouldPreservePlainLinesAndHandleSingleCellRows() {
        String reply = """
                Resumen:

                | Hito |
                | Reuni\u00f3n inicial |
                Texto final
                """;

        assertThat(this.chatbotResponseSanitizer.normalizeReplyForFrontend(reply))
                .isEqualTo(String.join(
                        System.lineSeparator(),
                        "Resumen:",
                        "",
                        "- Hito",
                        "- Reuni\u00f3n inicial",
                        "Texto final"
                ));
    }

    @Test
    void normalizeReplyForFrontendShouldSkipLeadingBlankLinesWhenReplyContainsPipes() {
        String reply = System.lineSeparator()
                + "  " + System.lineSeparator()
                + "| Campo | Valor |";

        assertThat(this.chatbotResponseSanitizer.normalizeReplyForFrontend(reply))
                .isEqualTo("- Campo: Valor");
    }

    @Test
    void normalizeReplyForFrontendShouldTrimPlainLinesWhenReplyContainsPipes() {
        String reply = "  Inicio con espacios  " + System.lineSeparator()
                + "  Valor | Detalle  " + System.lineSeparator()
                + "  Cierre con espacios  ";

        assertThat(this.chatbotResponseSanitizer.normalizeReplyForFrontend(reply))
                .isEqualTo(String.join(
                        System.lineSeparator(),
                        "Inicio con espacios",
                        "- Valor: Detalle",
                        "Cierre con espacios"
                ));
    }

    @Test
    void normalizeReplyForFrontendShouldJoinRowsWithMoreThanTwoCellsUsingSemicolons() {
        String reply = """
                | Tarea | Estado | Responsable |
                | Revisar contrato | Pendiente | Maria |
                """;

        assertThat(this.chatbotResponseSanitizer.normalizeReplyForFrontend(reply))
                .isEqualTo(String.join(
                        System.lineSeparator(),
                        "- Tarea: Estado; Responsable",
                        "- Revisar contrato: Pendiente; Maria"
                ));
    }

    @Test
    void normalizeReplyForFrontendShouldSkipPipeRowsWithoutUsefulCells() {
        String reply = "Antes" + System.lineSeparator()
                + "| \t | \t |" + System.lineSeparator()
                + "| Valor |" + System.lineSeparator()
                + "Despues";

        assertThat(this.chatbotResponseSanitizer.normalizeReplyForFrontend(reply))
                .isEqualTo(String.join(
                        System.lineSeparator(),
                        "Antes",
                        "- Valor",
                        "Despues"
                ));
    }

    @Test
    void normalizeReplyForFrontendShouldReturnEmptyStringWhenOnlyPipeNoiseExists() {
        String reply = " | | " + System.lineSeparator()
                + "| --- | :---: |" + System.lineSeparator()
                + " | ";

        assertThat(this.chatbotResponseSanitizer.normalizeReplyForFrontend(reply)).isEmpty();
    }

    @Test
    void normalizeReplyForFrontendShouldSkipSeparatorRowsWithColonsAndDashes() {
        String reply = """
                | Campo | Valor |
                | :--- | ---: |
                | Estado | Activo |
                """;

        assertThat(this.chatbotResponseSanitizer.normalizeReplyForFrontend(reply))
                .isEqualTo(String.join(
                        System.lineSeparator(),
                        "- Campo: Valor",
                        "- Estado: Activo"
                ));
    }

    @Test
    void normalizeReplyForFrontendShouldSkipCompactSeparatorRowsWithoutMarkdownTableShape() {
        String reply = "Antes" + System.lineSeparator()
                + "|:--|" + System.lineSeparator()
                + "---|---" + System.lineSeparator()
                + "A|B";

        assertThat(this.chatbotResponseSanitizer.normalizeReplyForFrontend(reply))
                .isEqualTo(String.join(
                        System.lineSeparator(),
                        "Antes",
                        "- A: B"
                ));
    }

    @Test
    void normalizeReplyForFrontendShouldTrimPipeRowsAndIgnoreEmptyCells() {
        String reply = "  |  Tarea  |   |  Responsable  |  ";

        assertThat(this.chatbotResponseSanitizer.normalizeReplyForFrontend(reply))
                .isEqualTo("- Tarea: Responsable");
    }

    @Test
    void normalizeReplyForFrontendShouldConvertPipeRowsWithoutOuterPipes() {
        String reply = "Tarea | Estado | Responsable";

        assertThat(this.chatbotResponseSanitizer.normalizeReplyForFrontend(reply))
                .isEqualTo("- Tarea: Estado; Responsable");
    }
}

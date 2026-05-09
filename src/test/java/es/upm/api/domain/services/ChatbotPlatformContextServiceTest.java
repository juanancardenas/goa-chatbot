package es.upm.api.domain.services;

import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.model.platform.EngagementEventPage;
import es.upm.api.domain.model.platform.EngagementEventSummary;
import es.upm.api.domain.model.platform.EngagementLetterSummary;
import es.upm.api.domain.model.platform.LegalProcedureSummary;
import es.upm.api.domain.model.platform.UserSummary;
import es.upm.api.infrastructure.webclients.EngagementWebClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotPlatformContextServiceTest {
    @Mock
    private EngagementWebClient engagementWebClient;

    @InjectMocks
    private ChatbotPlatformContextService chatbotPlatformContextService;

    @Test
    void loadContextShouldReturnEmptyWhenEngagementLetterIdIsBlank() {
        ChatbotPlatformContextService service = new ChatbotPlatformContextService(this.engagementWebClient);

        assertThat(service.loadContext(" ")).isEmpty();

        verify(this.engagementWebClient, never()).readById(anyString());
        verify(this.engagementWebClient, never()).readEventsByEngagementLetterId(anyString(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void loadContextShouldReturnContextWithOwnerProceduresEventsAndSources() {
        String engagementLetterId = "eng-001";
        ChatbotPlatformContextService service = new ChatbotPlatformContextService(this.engagementWebClient);

        when(this.engagementWebClient.readById(engagementLetterId))
                .thenReturn(new EngagementLetterSummary(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 4, 1),
                        null,
                        new UserSummary(UUID.randomUUID(), "Ana", "Ocaña", "ana@goa.es", "600000000"),
                        List.of(
                                new LegalProcedureSummary("Reclamación civil", LocalDate.of(2026, 4, 2), null, List.of()),
                                new LegalProcedureSummary("Reclamación civil", LocalDate.of(2026, 4, 3), null, List.of()),
                                new LegalProcedureSummary("Seguimiento penal", LocalDate.of(2026, 4, 4), null, List.of())
                        )
                ));
        when(this.engagementWebClient.readEventsByEngagementLetterId(engagementLetterId, 0, 5))
                .thenReturn(new EngagementEventPage(List.of(
                        new EngagementEventSummary("MILESTONE", "OPEN", "Se registró escrito", "comentario", LocalDate.of(2026, 4, 10)),
                        new EngagementEventSummary("EVENT", "SCHEDULED", "Vista programada", "comentario", LocalDate.of(2026, 4, 11)),
                        new EngagementEventSummary("EVENT", "DONE", "Notificación enviada", "comentario", LocalDate.of(2026, 4, 12)),
                        new EngagementEventSummary("EVENT", "DONE", "Hito extra no esperado", "comentario", LocalDate.of(2026, 4, 13))
                )));

        var result = service.loadContext(engagementLetterId);

        assertThat(result).isPresent();
        assertThat(result.get().getEngagementLetterId()).isEqualTo(engagementLetterId);
        assertThat(result.get().getOwnerDisplayName()).isEqualTo("Ana Ocaña");
        assertThat(result.get().getProcedureTitles()).containsExactly("Reclamación civil", "Seguimiento penal");
        assertThat(result.get().getRecentEventSummaries()).hasSize(3);
        assertThat(result.get().getRecentEventSummaries().getFirst()).contains("Se registró escrito");
        assertThat(result.get().getSourcesSummary()).contains("Hoja de encargo " + engagementLetterId);
        assertThat(result.get().getSourcesSummary()).contains("Procedimiento: Reclamación civil");
        assertThat(result.get().getSourcesSummary()).contains("Procedimiento: Seguimiento penal");
        assertThat(result.get().getSourcesSummary()).contains("Hito/evento: Se registró escrito [MILESTONE] - OPEN");
        assertThat(result.get().getSourcesSummary()).contains("Hito/evento: Vista programada [EVENT] - SCHEDULED");
    }

    @Test
    void loadContextShouldFallbackToDefaultOwnerWhenEngagementLetterIsUnavailableButEventsExist() {
        String engagementLetterId = "eng-002";
        ChatbotPlatformContextService service = new ChatbotPlatformContextService(this.engagementWebClient);

        when(this.engagementWebClient.readById(engagementLetterId))
                .thenThrow(new IllegalStateException("engagement unavailable"));
        when(this.engagementWebClient.readEventsByEngagementLetterId(engagementLetterId, 0, 5))
                .thenReturn(new EngagementEventPage(List.of(
                        new EngagementEventSummary("EVENT", "OPEN", "Vista inicial", null, LocalDate.of(2026, 4, 16))
                )));

        var result = service.loadContext(engagementLetterId);

        assertThat(result).isPresent();
        assertThat(result.get().getOwnerDisplayName()).isEqualTo("usuario del encargo");
        assertThat(result.get().getProcedureTitles()).isEmpty();
        assertThat(result.get().getRecentEventSummaries()).containsExactly("Vista inicial [EVENT] - OPEN");
    }

    @Test
    void loadContextShouldReturnEmptyWhenNoEngagementDataAndNoEventsAreAvailable() {
        String engagementLetterId = "eng-003";
        ChatbotPlatformContextService service = new ChatbotPlatformContextService(this.engagementWebClient);

        when(this.engagementWebClient.readById(engagementLetterId)).thenReturn(null);
        when(this.engagementWebClient.readEventsByEngagementLetterId(engagementLetterId, 0, 5))
                .thenReturn(new EngagementEventPage(List.of()));

        assertThat(service.loadContext(engagementLetterId)).isEmpty();
    }

    @Test
    void loadContextShouldKeepEngagementDataWhenEventsCannotBeLoaded() {
        String engagementLetterId = "eng-004";
        ChatbotPlatformContextService service = new ChatbotPlatformContextService(this.engagementWebClient);

        when(this.engagementWebClient.readById(engagementLetterId))
                .thenReturn(new EngagementLetterSummary(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 4, 5),
                        null,
                        null,
                        List.of(
                                new LegalProcedureSummary("Procedimiento A", LocalDate.of(2026, 4, 6), null, List.of()),
                                new LegalProcedureSummary(" ", LocalDate.of(2026, 4, 7), null, List.of()),
                                new LegalProcedureSummary("Procedimiento B", LocalDate.of(2026, 4, 8), null, List.of())
                        )
                ));
        when(this.engagementWebClient.readEventsByEngagementLetterId(engagementLetterId, 0, 5))
                .thenThrow(new IllegalStateException("events unavailable"));

        var result = service.loadContext(engagementLetterId);

        assertThat(result).isPresent();
        assertThat(result.get().getOwnerDisplayName()).isEqualTo("usuario del encargo");
        assertThat(result.get().getProcedureTitles()).containsExactly("Procedimiento A", "Procedimiento B");
        assertThat(result.get().getRecentEventSummaries()).isEmpty();
        assertThat(result.get().getSourcesSummary()).containsExactly(
                "Hoja de encargo " + engagementLetterId,
                "Procedimiento: Procedimiento A",
                "Procedimiento: Procedimiento B"
        );
    }

    @Test
    void loadContextShouldUseFallbackTitleForBlankEventsAndLimitSourcesSummaryToTwoEvents() {
        String engagementLetterId = "eng-005";
        ChatbotPlatformContextService service = new ChatbotPlatformContextService(this.engagementWebClient);

        when(this.engagementWebClient.readById(engagementLetterId))
                .thenReturn(new EngagementLetterSummary(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 4, 1),
                        null,
                        new UserSummary(UUID.randomUUID(), "Lucia", "Perez", "lucia@goa.es", "600000001"),
                        List.of()
                ));
        when(this.engagementWebClient.readEventsByEngagementLetterId(engagementLetterId, 0, 5))
                .thenReturn(new EngagementEventPage(List.of(
                        new EngagementEventSummary("EVENT", "OPEN", " ", null, LocalDate.of(2026, 4, 10)),
                        new EngagementEventSummary("EVENT", "OPEN", "Evento 1", null, LocalDate.of(2026, 4, 11)),
                        new EngagementEventSummary("MILESTONE", "DONE", "Evento 2", null, LocalDate.of(2026, 4, 12)),
                        new EngagementEventSummary("TASK", "OPEN", "Evento 3", null, LocalDate.of(2026, 4, 13))
                )));

        var result = service.loadContext(engagementLetterId);

        assertThat(result).isPresent();
        assertThat(result.get().getRecentEventSummaries()).hasSize(3);
        assertThat(result.get().getRecentEventSummaries().get(0)).startsWith("Evento sin");
        assertThat(result.get().getRecentEventSummaries().get(0)).contains("[EVENT] - OPEN");
        assertThat(result.get().getRecentEventSummaries().get(1)).isEqualTo("Evento 1 [EVENT] - OPEN");
        assertThat(result.get().getRecentEventSummaries().get(2)).isEqualTo("Evento 2 [MILESTONE] - DONE");
        assertThat(result.get().getSourcesSummary()).hasSize(3);
        assertThat(result.get().getSourcesSummary().get(0)).isEqualTo("Hoja de encargo " + engagementLetterId);
        assertThat(result.get().getSourcesSummary().get(1)).startsWith("Hito/evento: Evento sin");
        assertThat(result.get().getSourcesSummary().get(1)).contains("[EVENT] - OPEN");
        assertThat(result.get().getSourcesSummary().get(2)).isEqualTo("Hito/evento: Evento 1 [EVENT] - OPEN");
    }

    @Test
    void loadContextShouldFallbackToDefaultOwnerAndLimitProcedureSourcesWhenOwnerDisplayNameIsBlank() {
        String engagementLetterId = "eng-006";
        ChatbotPlatformContextService service = new ChatbotPlatformContextService(this.engagementWebClient);
        UserSummary owner = org.mockito.Mockito.mock(UserSummary.class);

        when(owner.displayName()).thenReturn(" ");
        when(this.engagementWebClient.readById(engagementLetterId))
                .thenReturn(new EngagementLetterSummary(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 4, 1),
                        null,
                        owner,
                        List.of(
                                new LegalProcedureSummary("Procedimiento A", LocalDate.of(2026, 4, 2), null, List.of()),
                                new LegalProcedureSummary(null, LocalDate.of(2026, 4, 3), null, List.of()),
                                new LegalProcedureSummary("Procedimiento B", LocalDate.of(2026, 4, 4), null, List.of()),
                                new LegalProcedureSummary("Procedimiento C", LocalDate.of(2026, 4, 5), null, List.of())
                        )
                ));
        when(this.engagementWebClient.readEventsByEngagementLetterId(engagementLetterId, 0, 5))
                .thenReturn(new EngagementEventPage(List.of()));

        var result = service.loadContext(engagementLetterId);

        assertThat(result).isPresent();
        assertThat(result.get().getOwnerDisplayName()).isEqualTo("usuario del encargo");
        assertThat(result.get().getProcedureTitles()).containsExactly("Procedimiento A", "Procedimiento B", "Procedimiento C");
        assertThat(result.get().getSourcesSummary()).containsExactly(
                "Hoja de encargo " + engagementLetterId,
                "Procedimiento: Procedimiento A",
                "Procedimiento: Procedimiento B"
        );
    }

    @Test
    void loadContextShouldIgnoreNullOrBlankEventTexts() {
        String engagementLetterId = "eng-007";
        ChatbotPlatformContextService service = new ChatbotPlatformContextService(this.engagementWebClient);
        EngagementEventSummary nullTextEvent = org.mockito.Mockito.mock(EngagementEventSummary.class);
        EngagementEventSummary blankTextEvent = org.mockito.Mockito.mock(EngagementEventSummary.class);
        EngagementEventSummary validEvent = org.mockito.Mockito.mock(EngagementEventSummary.class);
        EngagementEventSummary secondValidEvent = org.mockito.Mockito.mock(EngagementEventSummary.class);

        when(this.engagementWebClient.readById(engagementLetterId))
                .thenReturn(new EngagementLetterSummary(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 4, 1),
                        null,
                        new UserSummary(UUID.randomUUID(), "Ana", "Diaz", "ana@goa.es", "600000002"),
                        List.of()
                ));
        when(nullTextEvent.displayText()).thenReturn(null);
        when(blankTextEvent.displayText()).thenReturn(" ");
        when(validEvent.displayText()).thenReturn("Evento valido");
        when(secondValidEvent.displayText()).thenReturn("Evento adicional");
        when(this.engagementWebClient.readEventsByEngagementLetterId(engagementLetterId, 0, 5))
                .thenReturn(new EngagementEventPage(List.of(
                        nullTextEvent,
                        blankTextEvent,
                        validEvent,
                        secondValidEvent
                )));

        var result = service.loadContext(engagementLetterId);

        assertThat(result).isPresent();
        assertThat(result.get().getRecentEventSummaries()).containsExactly("Evento valido", "Evento adicional");
        assertThat(result.get().getSourcesSummary()).containsExactly(
                "Hoja de encargo " + engagementLetterId,
                "Hito/evento: Evento valido",
                "Hito/evento: Evento adicional"
        );
    }

    @Test
    void shouldMapLegalTasksFromEngagementContext() {
        LegalProcedureSummary procedure = new LegalProcedureSummary(
                "Procedimiento de herencia",
                LocalDate.of(2026, 4, 28),
                null,
                List.of(
                        "Estudio de antecedentes y documentación.",
                        "Asesoramiento jurídico.",
                        "Localización de personas."
                )
        );

        EngagementLetterSummary engagementLetter = new EngagementLetterSummary(
                UUID.randomUUID(),
                LocalDate.of(2026, 4, 1),
                null,
                null,
                List.of(procedure)
        );

        when(this.engagementWebClient.readById("engagement-001"))
                .thenReturn(engagementLetter);

        Optional<ChatbotPlatformContext> result = this.chatbotPlatformContextService.loadContext("engagement-001");

        assertThat(result).isPresent();
        assertThat(result.get().getLegalTaskSummaries()).containsExactly(
                "Procedimiento de herencia: Estudio de antecedentes y documentación.",
                "Procedimiento de herencia: Asesoramiento jurídico.",
                "Procedimiento de herencia: Localización de personas."
        );
        assertThat(result.get().getSourcesSummary()).anyMatch(source -> source.contains("Legal Task"));
    }

    @Test
    void shouldIgnoreBlankLegalTasks() {
        LegalProcedureSummary procedure = new LegalProcedureSummary(
                "Procedimiento de herencia",
                null,
                null,
                List.of(
                        "Estudio de antecedentes y documentación.",
                        " ",
                        ""
                )
        );

        EngagementLetterSummary engagementLetter = new EngagementLetterSummary(
                UUID.randomUUID(),
                LocalDate.of(2026, 4, 1),
                null,
                null,
                List.of(procedure)
        );

        when(this.engagementWebClient.readById("engagement-001"))
                .thenReturn(engagementLetter);

        Optional<ChatbotPlatformContext> result = this.chatbotPlatformContextService.loadContext("engagement-001");

        assertThat(result).isPresent();
        assertThat(result.get().getLegalTaskSummaries()).containsExactly(
                "Procedimiento de herencia: Estudio de antecedentes y documentación."
        );
    }

    @Test
    void shouldUseFallbackProcedureTitleWhenProcedureTitleIsMissing() {
        LegalProcedureSummary procedure = new LegalProcedureSummary(
                null,
                null,
                null,
                List.of("Asesoramiento jurídico.")
        );

        EngagementLetterSummary engagementLetter = new EngagementLetterSummary(
                UUID.randomUUID(),
                LocalDate.of(2026, 4, 1),
                null,
                null,
                List.of(procedure)
        );

        when(this.engagementWebClient.readById("engagement-001"))
                .thenReturn(engagementLetter);

        Optional<ChatbotPlatformContext> result = this.chatbotPlatformContextService.loadContext("engagement-001");

        assertThat(result).isPresent();
        assertThat(result.get().getLegalTaskSummaries()).containsExactly(
                "Procedimiento sin título: Asesoramiento jurídico."
        );
    }
}

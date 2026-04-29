package es.upm.api.domain.services;

import es.upm.api.domain.model.platform.EngagementEventPage;
import es.upm.api.domain.model.platform.EngagementEventSummary;
import es.upm.api.domain.model.platform.EngagementLetterSummary;
import es.upm.api.domain.model.platform.LegalProcedureSummary;
import es.upm.api.domain.model.platform.UserSummary;
import es.upm.api.domain.webclients.EngagementWebClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
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
}

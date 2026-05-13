package es.upm.api.infrastructure.webclients;

import es.upm.api.domain.model.platform.EngagementEventPage;
import es.upm.api.domain.model.platform.EngagementEventSummary;
import es.upm.api.domain.model.platform.EngagementLetterSummary;
import es.upm.api.domain.model.platform.UserSummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EngagementClientAdapterTest {

    @Test
    void readByIdShouldDelegateToFeignClient() {
        EngagementFeignClient engagementFeignClient = mock(EngagementFeignClient.class);
        EngagementClientAdapter adapter = new EngagementClientAdapter(engagementFeignClient);
        String engagementLetterId = "engagement-123";
        EngagementLetterSummary expected = new EngagementLetterSummary(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 30),
                new UserSummary(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        "Ana",
                        "Ocana",
                        "ana@goa.test",
                        "600000000"
                ),
                List.of()
        );
        when(engagementFeignClient.readById(engagementLetterId)).thenReturn(expected);

        EngagementLetterSummary result = adapter.readById(engagementLetterId);

        assertThat(result).isSameAs(expected);
        verify(engagementFeignClient).readById(engagementLetterId);
    }

    @Test
    void readEventsByEngagementLetterIdShouldDelegateToFeignClient() {
        EngagementFeignClient engagementFeignClient = mock(EngagementFeignClient.class);
        EngagementClientAdapter adapter = new EngagementClientAdapter(engagementFeignClient);
        String engagementLetterId = "engagement-123";
        int page = 2;
        int size = 10;
        EngagementEventPage expected = new EngagementEventPage(List.of(
                new EngagementEventSummary("EVENT", "OPEN", "Vista senalada", null, LocalDate.of(2026, 5, 12))
        ));
        when(engagementFeignClient.readEventsByEngagementLetterId(engagementLetterId, page, size)).thenReturn(expected);

        EngagementEventPage result = adapter.readEventsByEngagementLetterId(engagementLetterId, page, size);

        assertThat(result).isSameAs(expected);
        verify(engagementFeignClient).readEventsByEngagementLetterId(engagementLetterId, page, size);
    }
}

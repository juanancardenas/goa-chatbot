package es.upm.api.adapter.out.webclient.engagement;

import es.upm.api.domain.model.platform.EngagementEventPage;
import es.upm.api.domain.model.platform.EngagementEventSummary;
import es.upm.api.domain.model.platform.EngagementLetterSummary;
import es.upm.api.adapter.out.webclient.engagement.dto.EngagementEventPageResponseDto;
import es.upm.api.adapter.out.webclient.engagement.dto.EngagementEventResponseDto;
import es.upm.api.adapter.out.webclient.engagement.dto.EngagementLetterResponseDto;
import es.upm.api.adapter.out.webclient.user.UserFeignMapper;
import es.upm.api.adapter.out.webclient.user.dto.UserResponseDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EngagementClientAdapterTest {

    @Test
    void readByIdShouldMapFeignDtoToDomain() {
        EngagementFeignClient engagementFeignClient = mock(EngagementFeignClient.class);
        EngagementClientAdapter adapter = this.adapter(engagementFeignClient);
        String engagementLetterId = "engagement-123";
        EngagementLetterResponseDto expected = new EngagementLetterResponseDto(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                LocalDate.of(2026, Month.MAY, 1),
                LocalDate.of(2026, Month.MAY, 30),
                new UserResponseDto(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        "Ana",
                        "Ocana",
                        "ana@goa.test",
                        "600000000"
                ),
                List.of(new EngagementLetterResponseDto.LegalProcedureResponseDto(
                        "Procedimiento laboral",
                        LocalDate.of(2026, Month.APRIL, 1),
                        null,
                        List.of("Preparar demanda")
                ))
        );
        when(engagementFeignClient.readById(engagementLetterId)).thenReturn(expected);

        EngagementLetterSummary result = adapter.readById(engagementLetterId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(expected.getId());
        assertThat(result.getCreationDate()).isEqualTo(LocalDate.of(2026, Month.MAY, 1));
        assertThat(result.getClosingDate()).isEqualTo(LocalDate.of(2026, Month.MAY, 30));
        assertThat(result.getOwner().getId()).isEqualTo(expected.getOwner().getId());
        assertThat(result.getOwner().getFirstName()).isEqualTo("Ana");
        assertThat(result.getLegalProcedures()).hasSize(1);
        assertThat(result.getLegalProcedures().getFirst().getTitle()).isEqualTo("Procedimiento laboral");
        assertThat(result.getLegalProcedures().getFirst().getLegalTasks()).containsExactly("Preparar demanda");
        verify(engagementFeignClient).readById(engagementLetterId);
    }

    @Test
    void readEventsByEngagementLetterIdShouldMapFeignDtoToDomain() {
        EngagementFeignClient engagementFeignClient = mock(EngagementFeignClient.class);
        EngagementClientAdapter adapter = this.adapter(engagementFeignClient);
        String engagementLetterId = "engagement-123";
        int page = 2;
        int size = 10;
        EngagementEventPageResponseDto expected = new EngagementEventPageResponseDto(List.of(
                new EngagementEventResponseDto("EVENT", "OPEN", "Vista senalada", null, LocalDate.of(2026, Month.MAY, 12))
        ));
        when(engagementFeignClient.readEventsByEngagementLetterId(engagementLetterId, page, size)).thenReturn(expected);

        EngagementEventPage result = adapter.readEventsByEngagementLetterId(engagementLetterId, page, size);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        EngagementEventSummary event = result.getContent().getFirst();
        assertThat(event.getType()).isEqualTo("EVENT");
        assertThat(event.getState()).isEqualTo("OPEN");
        assertThat(event.getTitle()).isEqualTo("Vista senalada");
        assertThat(event.getDate()).isEqualTo(LocalDate.of(2026, Month.MAY, 12));
        verify(engagementFeignClient).readEventsByEngagementLetterId(engagementLetterId, page, size);
    }

    @Test
    void readByIdShouldHandleIncompleteFeignDto() {
        EngagementFeignClient engagementFeignClient = mock(EngagementFeignClient.class);
        EngagementClientAdapter adapter = this.adapter(engagementFeignClient);
        EngagementLetterResponseDto responseDto = new EngagementLetterResponseDto(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                null,
                null,
                null,
                null
        );
        when(engagementFeignClient.readById("engagement-incomplete")).thenReturn(responseDto);

        EngagementLetterSummary result = adapter.readById("engagement-incomplete");

        assertThat(result.getId()).isEqualTo(responseDto.getId());
        assertThat(result.getCreationDate()).isNull();
        assertThat(result.getClosingDate()).isNull();
        assertThat(result.getOwner()).isNull();
        assertThat(result.getLegalProcedures()).isEmpty();
    }

    @Test
    void readEventsByEngagementLetterIdShouldMapNullContentToEmptyList() {
        EngagementFeignClient engagementFeignClient = mock(EngagementFeignClient.class);
        EngagementClientAdapter adapter = this.adapter(engagementFeignClient);
        when(engagementFeignClient.readEventsByEngagementLetterId("engagement-empty", 0, 5))
                .thenReturn(new EngagementEventPageResponseDto(null));

        EngagementEventPage result = adapter.readEventsByEngagementLetterId("engagement-empty", 0, 5);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void readByIdShouldReturnNullWhenFeignReturnsNull() {
        EngagementFeignClient engagementFeignClient = mock(EngagementFeignClient.class);
        EngagementClientAdapter adapter = this.adapter(engagementFeignClient);
        when(engagementFeignClient.readById("missing-engagement")).thenReturn(null);

        EngagementLetterSummary result = adapter.readById("missing-engagement");

        assertThat(result).isNull();
    }

    @Test
    void readEventsByEngagementLetterIdShouldPropagateFeignErrors() {
        EngagementFeignClient engagementFeignClient = mock(EngagementFeignClient.class);
        EngagementClientAdapter adapter = this.adapter(engagementFeignClient);
        RuntimeException expected = new RuntimeException("engagement service unavailable");
        when(engagementFeignClient.readEventsByEngagementLetterId("engagement-error", 0, 5))
                .thenThrow(expected);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.readEventsByEngagementLetterId("engagement-error", 0, 5)
        );

        assertThat(exception).isSameAs(expected);
    }

    private EngagementClientAdapter adapter(EngagementFeignClient engagementFeignClient) {
        return new EngagementClientAdapter(
                engagementFeignClient,
                new EngagementFeignMapper(new UserFeignMapper())
        );
    }
}

package es.upm.api.infrastructure.webclients.engagement;

import es.upm.api.domain.model.platform.EngagementEventPage;
import es.upm.api.domain.model.platform.EngagementLetterSummary;
import es.upm.api.infrastructure.webclients.engagement.dto.EngagementEventPageResponseDto;
import es.upm.api.infrastructure.webclients.engagement.dto.EngagementEventResponseDto;
import es.upm.api.infrastructure.webclients.engagement.dto.EngagementLetterResponseDto;
import es.upm.api.infrastructure.webclients.user.UserFeignMapper;
import es.upm.api.infrastructure.webclients.user.dto.UserResponseDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EngagementFeignMapperTest {

    private final EngagementFeignMapper mapper = new EngagementFeignMapper(new UserFeignMapper());

    @Test
    void toDomainShouldReturnNullWhenEngagementLetterDtoIsNull() {
        assertThat(this.mapper.toDomain((EngagementLetterResponseDto) null)).isNull();
    }

    @Test
    void toDomainShouldMapEngagementLetterDtoToDomain() {
        UUID engagementId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID ownerId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        EngagementLetterResponseDto dto = new EngagementLetterResponseDto(
                engagementId,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 30),
                new UserResponseDto(ownerId, "Ana", "Ocana", "ana@goa.test", "600000000"),
                List.of(new EngagementLetterResponseDto.LegalProcedureResponseDto(
                        "Procedimiento laboral",
                        LocalDate.of(2026, 4, 1),
                        null,
                        List.of("Preparar demanda")
                ))
        );

        EngagementLetterSummary result = this.mapper.toDomain(dto);

        assertThat(result.getId()).isEqualTo(engagementId);
        assertThat(result.getCreationDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(result.getClosingDate()).isEqualTo(LocalDate.of(2026, 5, 30));
        assertThat(result.getOwner().getId()).isEqualTo(ownerId);
        assertThat(result.getOwner().getFirstName()).isEqualTo("Ana");
        assertThat(result.getLegalProcedures()).hasSize(1);
        assertThat(result.getLegalProcedures().getFirst().getTitle()).isEqualTo("Procedimiento laboral");
        assertThat(result.getLegalProcedures().getFirst().getStartDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(result.getLegalProcedures().getFirst().getClosingDate()).isNull();
        assertThat(result.getLegalProcedures().getFirst().getLegalTasks()).containsExactly("Preparar demanda");
    }

    @Test
    void toDomainShouldMapNullLegalProceduresToEmptyList() {
        EngagementLetterResponseDto dto = new EngagementLetterResponseDto(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                null,
                null,
                null,
                null
        );

        EngagementLetterSummary result = this.mapper.toDomain(dto);

        assertThat(result.getLegalProcedures()).isEmpty();
    }

    @Test
    void toDomainShouldFilterNullLegalProceduresAndDefaultNullLegalTasksToEmptyList() {
        EngagementLetterResponseDto dto = new EngagementLetterResponseDto(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                null,
                null,
                null,
                Arrays.asList(
                        null,
                        new EngagementLetterResponseDto.LegalProcedureResponseDto(
                                "Procedimiento civil",
                                null,
                                LocalDate.of(2026, 6, 1),
                                null
                        )
                )
        );

        EngagementLetterSummary result = this.mapper.toDomain(dto);

        assertThat(result.getLegalProcedures()).hasSize(1);
        assertThat(result.getLegalProcedures().getFirst().getTitle()).isEqualTo("Procedimiento civil");
        assertThat(result.getLegalProcedures().getFirst().getClosingDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(result.getLegalProcedures().getFirst().getLegalTasks()).isEmpty();
    }

    @Test
    void toDomainShouldReturnNullWhenEventPageDtoIsNull() {
        assertThat(this.mapper.toDomain((EngagementEventPageResponseDto) null)).isNull();
    }

    @Test
    void toDomainShouldMapNullEventContentToEmptyList() {
        EngagementEventPage result = this.mapper.toDomain(new EngagementEventPageResponseDto(null));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void toDomainShouldFilterNullEventsAndMapEventFields() {
        EngagementEventPageResponseDto dto = new EngagementEventPageResponseDto(Arrays.asList(
                null,
                new EngagementEventResponseDto(
                        "EVENT",
                        "OPEN",
                        "Vista senalada",
                        "Comentario",
                        LocalDate.of(2026, 5, 12)
                )
        ));

        EngagementEventPage result = this.mapper.toDomain(dto);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getType()).isEqualTo("EVENT");
        assertThat(result.getContent().getFirst().getState()).isEqualTo("OPEN");
        assertThat(result.getContent().getFirst().getTitle()).isEqualTo("Vista senalada");
        assertThat(result.getContent().getFirst().getComment()).isEqualTo("Comentario");
        assertThat(result.getContent().getFirst().getDate()).isEqualTo(LocalDate.of(2026, 5, 12));
    }
}

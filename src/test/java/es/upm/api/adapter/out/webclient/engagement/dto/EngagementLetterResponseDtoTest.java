package es.upm.api.adapter.out.webclient.engagement.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import es.upm.api.adapter.out.webclient.user.dto.UserResponseDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EngagementLetterResponseDtoTest {

    @Test
    void allArgsConstructorShouldSetFields() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UserResponseDto owner = new UserResponseDto(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Ana",
                "Ocana",
                "ana@goa.test",
                "600000000"
        );
        EngagementLetterResponseDto.LegalProcedureResponseDto procedure =
                new EngagementLetterResponseDto.LegalProcedureResponseDto(
                        "Procedimiento laboral",
                        LocalDate.of(2026, 4, 1),
                        null,
                        List.of("Preparar demanda")
                );

        EngagementLetterResponseDto dto = new EngagementLetterResponseDto(
                id,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 30),
                owner,
                List.of(procedure)
        );

        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getCreationDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(dto.getClosingDate()).isEqualTo(LocalDate.of(2026, 5, 30));
        assertThat(dto.getOwner()).isSameAs(owner);
        assertThat(dto.getLegalProcedures()).containsExactly(procedure);
    }

    @Test
    void settersShouldUpdateFields() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UserResponseDto owner = new UserResponseDto();
        EngagementLetterResponseDto.LegalProcedureResponseDto procedure =
                new EngagementLetterResponseDto.LegalProcedureResponseDto();
        EngagementLetterResponseDto dto = new EngagementLetterResponseDto();

        dto.setId(id);
        dto.setCreationDate(LocalDate.of(2026, 5, 1));
        dto.setClosingDate(LocalDate.of(2026, 5, 30));
        dto.setOwner(owner);
        dto.setLegalProcedures(List.of(procedure));

        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getCreationDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(dto.getClosingDate()).isEqualTo(LocalDate.of(2026, 5, 30));
        assertThat(dto.getOwner()).isSameAs(owner);
        assertThat(dto.getLegalProcedures()).containsExactly(procedure);
    }

    @Test
    void legalProcedureSettersShouldUpdateFields() {
        EngagementLetterResponseDto.LegalProcedureResponseDto procedure =
                new EngagementLetterResponseDto.LegalProcedureResponseDto();

        procedure.setTitle("Procedimiento civil");
        procedure.setStartDate(LocalDate.of(2026, 4, 1));
        procedure.setClosingDate(LocalDate.of(2026, 6, 1));
        procedure.setLegalTasks(List.of("Revisar expediente"));

        assertThat(procedure.getTitle()).isEqualTo("Procedimiento civil");
        assertThat(procedure.getStartDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(procedure.getClosingDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(procedure.getLegalTasks()).containsExactly("Revisar expediente");
    }

    @Test
    void shouldIgnoreUnknownJsonProperties() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        EngagementLetterResponseDto dto = objectMapper.readValue("""
                {
                  "id": "11111111-1111-1111-1111-111111111111",
                  "creationDate": "2026-05-01",
                  "closingDate": "2026-05-30",
                  "owner": {
                    "id": "22222222-2222-2222-2222-222222222222",
                    "firstName": "Ana",
                    "familyName": "Ocana",
                    "email": "ana@goa.test",
                    "mobile": "600000000",
                    "externalOnlyField": "ignored"
                  },
                  "legalProcedures": [
                    {
                      "title": "Procedimiento laboral",
                      "startDate": "2026-04-01",
                      "closingDate": null,
                      "legalTasks": ["Preparar demanda"],
                      "externalOnlyField": "ignored"
                    }
                  ],
                  "externalOnlyField": "ignored"
                }
                """, EngagementLetterResponseDto.class);

        assertThat(dto.getId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(dto.getCreationDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(dto.getClosingDate()).isEqualTo(LocalDate.of(2026, 5, 30));
        assertThat(dto.getOwner().getFirstName()).isEqualTo("Ana");
        assertThat(dto.getLegalProcedures()).hasSize(1);
        assertThat(dto.getLegalProcedures().getFirst().getTitle()).isEqualTo("Procedimiento laboral");
        assertThat(dto.getLegalProcedures().getFirst().getLegalTasks()).containsExactly("Preparar demanda");
    }
}

package es.upm.api.infrastructure.webclients.engagement.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EngagementEventResponseDtoTest {

    @Test
    void allArgsConstructorShouldSetFields() {
        LocalDate date = LocalDate.of(2026, 5, 12);

        EngagementEventResponseDto dto = new EngagementEventResponseDto(
                "EVENT",
                "OPEN",
                "Vista senalada",
                "Comentario",
                date
        );

        assertThat(dto.getType()).isEqualTo("EVENT");
        assertThat(dto.getState()).isEqualTo("OPEN");
        assertThat(dto.getTitle()).isEqualTo("Vista senalada");
        assertThat(dto.getComment()).isEqualTo("Comentario");
        assertThat(dto.getDate()).isEqualTo(date);
    }

    @Test
    void settersShouldUpdateFields() {
        EngagementEventResponseDto dto = new EngagementEventResponseDto();

        dto.setType("TASK");
        dto.setState("DONE");
        dto.setTitle("Tarea completada");
        dto.setComment("Sin incidencias");
        dto.setDate(LocalDate.of(2026, 5, 13));

        assertThat(dto.getType()).isEqualTo("TASK");
        assertThat(dto.getState()).isEqualTo("DONE");
        assertThat(dto.getTitle()).isEqualTo("Tarea completada");
        assertThat(dto.getComment()).isEqualTo("Sin incidencias");
        assertThat(dto.getDate()).isEqualTo(LocalDate.of(2026, 5, 13));
    }

    @Test
    void shouldIgnoreUnknownJsonProperties() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        EngagementEventResponseDto dto = objectMapper.readValue("""
                {
                  "type": "EVENT",
                  "state": "OPEN",
                  "title": "Vista senalada",
                  "comment": "Comentario",
                  "date": "2026-05-12",
                  "externalOnlyField": "ignored"
                }
                """, EngagementEventResponseDto.class);

        assertThat(dto.getType()).isEqualTo("EVENT");
        assertThat(dto.getState()).isEqualTo("OPEN");
        assertThat(dto.getTitle()).isEqualTo("Vista senalada");
        assertThat(dto.getComment()).isEqualTo("Comentario");
        assertThat(dto.getDate()).isEqualTo(LocalDate.of(2026, 5, 12));
    }
}

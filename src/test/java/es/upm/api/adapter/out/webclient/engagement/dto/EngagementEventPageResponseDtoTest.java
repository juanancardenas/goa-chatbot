package es.upm.api.adapter.out.webclient.engagement.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EngagementEventPageResponseDtoTest {

    @Test
    void allArgsConstructorShouldSetContent() {
        EngagementEventResponseDto event = new EngagementEventResponseDto(
                "EVENT",
                "OPEN",
                "Vista senalada",
                null,
                LocalDate.of(2026, Month.MAY, 12)
        );

        EngagementEventPageResponseDto dto = new EngagementEventPageResponseDto(List.of(event));

        assertThat(dto.getContent()).containsExactly(event);
    }

    @Test
    void setterShouldUpdateContent() {
        EngagementEventResponseDto event = new EngagementEventResponseDto();
        EngagementEventPageResponseDto dto = new EngagementEventPageResponseDto();

        dto.setContent(List.of(event));

        assertThat(dto.getContent()).containsExactly(event);
    }

    @Test
    void shouldIgnoreUnknownJsonProperties() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        EngagementEventPageResponseDto dto = objectMapper.readValue("""
                {
                  "content": [
                    {
                      "type": "EVENT",
                      "state": "OPEN",
                      "title": "Vista senalada",
                      "date": "2026-05-12",
                      "externalOnlyField": "ignored"
                    }
                  ],
                  "totalElements": 1
                }
                """, EngagementEventPageResponseDto.class);

        assertThat(dto.getContent()).hasSize(1);
        assertThat(dto.getContent().getFirst().getTitle()).isEqualTo("Vista senalada");
        assertThat(dto.getContent().getFirst().getDate()).isEqualTo(LocalDate.of(2026, Month.MAY, 12));
    }
}

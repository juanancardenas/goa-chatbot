package es.upm.api.adapter.out.webclient.engagement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EngagementEventResponseDto {
    private String type;
    private String state;
    private String title;
    private String comment;
    private LocalDate date;
}

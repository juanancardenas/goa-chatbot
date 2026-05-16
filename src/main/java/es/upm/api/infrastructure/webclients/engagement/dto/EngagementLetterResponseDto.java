package es.upm.api.infrastructure.webclients.engagement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import es.upm.api.infrastructure.webclients.user.dto.UserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EngagementLetterResponseDto {
    private UUID id;
    private LocalDate creationDate;
    private LocalDate closingDate;
    private UserResponseDto owner;
    private List<LegalProcedureResponseDto> legalProcedures;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LegalProcedureResponseDto {
        private String title;
        private LocalDate startDate;
        private LocalDate closingDate;
        private List<String> legalTasks;
    }
}

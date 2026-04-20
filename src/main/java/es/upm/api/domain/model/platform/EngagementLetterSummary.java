package es.upm.api.domain.model.platform;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class EngagementLetterSummary {
    private UUID id;
    private LocalDate creationDate;
    private LocalDate closingDate;
    private UserSummary owner;
    private List<LegalProcedureSummary> legalProcedures;
}

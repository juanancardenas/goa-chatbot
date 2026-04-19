package es.upm.api.domain.model.platform;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegalProcedureSummary {
    private String title;
    private LocalDate startDate;
    private LocalDate closingDate;
    private List<String> legalTasks;
}

package es.upm.api.domain.model.platform;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EngagementEventSummary {
    private String type;
    private String state;
    private String title;
    private String comment;
    private LocalDate date;

    public String displayText() {
        String resolvedTitle = title == null || title.isBlank() ? "Evento sin título" : title.trim();
        String resolvedType = type == null || type.isBlank() ? null : type.trim();
        String resolvedState = state == null || state.isBlank() ? null : state.trim();

        StringBuilder builder = new StringBuilder(resolvedTitle);

        if (resolvedType != null) {
            builder.append(" [").append(resolvedType).append("]");
        }

        if (resolvedState != null) {
            builder.append(" - ").append(resolvedState);
        }

        return builder.toString();
    }
}

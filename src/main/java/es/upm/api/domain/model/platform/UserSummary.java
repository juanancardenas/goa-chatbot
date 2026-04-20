package es.upm.api.domain.model.platform;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSummary {
    private UUID id;
    private String firstName;
    private String familyName;
    private String email;
    private String mobile;

    public String displayName() {
        String first = firstName == null ? "" : firstName.trim();
        String family = familyName == null ? "" : familyName.trim();
        String fullName = (first + " " + family).trim();
        return fullName.isBlank() ? "usuario del encargo" : fullName;
    }
}

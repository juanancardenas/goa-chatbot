package es.upm.api.resources.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatbotContextualConversationRequestDto {

    @NotBlank(message = "engagementLetterId es obligatorio")
    private String engagementLetterId;

    public void setEngagementLetterId(String engagementLetterId) {
        this.engagementLetterId = engagementLetterId != null && engagementLetterId.isBlank()
                ? null
                : engagementLetterId;
    }

}

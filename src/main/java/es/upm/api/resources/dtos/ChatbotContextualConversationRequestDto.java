package es.upm.api.resources.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatbotContextualConversationRequestDto {

    private String engagementLetterId;

    public void setEngagementLetterId(String engagementLetterId) {
        this.engagementLetterId = engagementLetterId != null && engagementLetterId.isBlank()
                ? null
                : engagementLetterId;
    }

}

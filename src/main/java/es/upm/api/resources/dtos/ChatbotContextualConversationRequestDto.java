package es.upm.api.resources.dtos;

import jakarta.validation.constraints.NotBlank;

public class ChatbotContextualConversationRequestDto {

    @NotBlank
    private String engagementLetterId;

    public ChatbotContextualConversationRequestDto() {
    }

    public String getEngagementLetterId() {
        return engagementLetterId;
    }

    public void setEngagementLetterId(String engagementLetterId) {
        this.engagementLetterId = engagementLetterId;
    }

}

package es.upm.api.resources.dtos;

public class ChatbotContextualConversationResponseDto {

    private String conversationId;
    private String engagementLetterId;
    private String createdAt;
    private String error;

    public ChatbotContextualConversationResponseDto() {
    }

    public ChatbotContextualConversationResponseDto(
            String conversationId,
            String engagementLetterId,
            String createdAt,
            String error
    ) {
        this.conversationId = conversationId;
        this.engagementLetterId = engagementLetterId;
        this.createdAt = createdAt;
        this.error = error;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getEngagementLetterId() {
        return engagementLetterId;
    }

    public void setEngagementLetterId(String engagementLetterId) {
        this.engagementLetterId = engagementLetterId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

}

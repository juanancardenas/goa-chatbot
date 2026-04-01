package es.upm.api.resources.dtos;

public class ChatbotMessageResponseDto {

    private String conversationId;
    private String message;
    private String error;
    private String createdAt;

    public ChatbotMessageResponseDto() {
    }

    public ChatbotMessageResponseDto(String conversationId, String message, String error, String createdAt) {
        this.conversationId = conversationId;
        this.message = message;
        this.error = error;
        this.createdAt = createdAt;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
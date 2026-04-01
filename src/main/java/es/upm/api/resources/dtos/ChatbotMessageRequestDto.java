package es.upm.api.resources.dtos;

import jakarta.validation.constraints.NotBlank;

public class ChatbotMessageRequestDto {

    private String conversationId;

    @NotBlank
    private String message;

    public ChatbotMessageRequestDto() {
    }

    public ChatbotMessageRequestDto(String conversationId, String message) {
        this.conversationId = conversationId;
        this.message = message;
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
}
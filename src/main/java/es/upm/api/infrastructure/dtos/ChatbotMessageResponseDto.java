package es.upm.api.infrastructure.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
public class ChatbotMessageResponseDto {

    private String conversationId;
    private String message;
    private String error;
    private String createdAt;
    private String responseMode;
    private Boolean usedPlatformData;
    private List<String> sourcesSummary;

    public ChatbotMessageResponseDto(
            String conversationId,
            String message,
            String error,
            String createdAt,
            String responseMode,
            Boolean usedPlatformData,
            List<String> sourcesSummary
    ) {
        this.conversationId = conversationId;
        this.message = message;
        this.error = error;
        this.createdAt = createdAt;
        this.responseMode = responseMode;
        this.usedPlatformData = usedPlatformData;
        this.sourcesSummary = sourcesSummary;
    }

}
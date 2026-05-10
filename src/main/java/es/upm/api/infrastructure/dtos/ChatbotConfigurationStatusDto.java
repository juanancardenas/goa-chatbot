package es.upm.api.infrastructure.dtos;

import es.upm.api.domain.model.configuration.ChatbotConfigurationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotConfigurationStatusDto {
    private boolean enabled;
    private String provider;
    private String model;
    private int maxInputCharacters;
    private int maxOutputTokens;
    private int maxContextMessages;
    private boolean documentsAvailable;

    // Mapper para convertir de dominio (modelo) a DTO
    public static ChatbotConfigurationStatusDto fromDomain(ChatbotConfigurationStatus status) {
        return ChatbotConfigurationStatusDto.builder()
                .enabled(status.isEnabled())
                .provider(status.getProvider())
                .model(status.getModel())
                .maxInputCharacters(status.getMaxInputCharacters())
                .maxOutputTokens(status.getMaxOutputTokens())
                .maxContextMessages(status.getMaxContextMessages())
                .documentsAvailable(status.isDocumentsAvailable())
                .build();
    }
}

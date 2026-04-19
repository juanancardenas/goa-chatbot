package es.upm.api.domain.model;

import es.upm.api.domain.enums.ConversationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    private String id;
    @NotNull
    private String userId;
    private String engagementLetterId;
    @Builder.Default
    @NotNull
    private ConversationStatus status = ConversationStatus.ACTIVE;
    @NotNull
    private String type;
    @NotNull
    private LocalDateTime createdAt;
}

package es.upm.api.data.entities;

import es.upm.api.data.enums.ConversationStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "conversations")
public class ConversationEntity {

    private static final ConversationStatus DEFAULT_STATUS = ConversationStatus.ACTIVE;

    @Id
    private String id;
    private String userId;
    private String engagementLetterId;
    @NotNull
    private ConversationStatus status = DEFAULT_STATUS;
    @NotNull
    private String type;
    @NotNull
    private LocalDateTime createdAt;

    public ConversationEntity(
            String id,
            String userId,
            String engagementLetterId,
            ConversationStatus status,
            String type,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.engagementLetterId = engagementLetterId;
        this.status = status != null ? status : DEFAULT_STATUS;
        this.type = type;
        this.createdAt = createdAt;
    }

}

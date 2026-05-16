package es.upm.api.infrastructure.mongodb.entities;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.model.Conversation;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@Document(collection = "conversations")
public class ConversationEntity {
    @Id
    private String id;
    private String userId;
    private String engagementLetterId;
    @Builder.Default
    @NotNull
    private ConversationStatus status = ConversationStatus.ACTIVE;
    @NotNull
    private String type;
    @NotNull
    private LocalDateTime createdAt;
    @Builder.Default
    @NotNull
    private Integer lastSequenceNumber = 0;

    public ConversationEntity(Conversation conversation) {
        this.id = conversation.getId();
        this.userId = conversation.getUserId();
        this.engagementLetterId = conversation.getEngagementLetterId();
        this.status = conversation.getStatus();
        this.type = conversation.getType() != null ? conversation.getType().name() : null;
        this.createdAt = conversation.getCreatedAt();
        this.lastSequenceNumber = conversation.getLastSequenceNumber() != null
                ? conversation.getLastSequenceNumber()
                : 0;
    }

    public ConversationEntity(
            String id,
            String userId,
            String engagementLetterId,
            ConversationStatus status,
            String type,
            LocalDateTime createdAt
    ) {
        this(id, userId, engagementLetterId, status, type, createdAt, 0);
    }

    public ConversationEntity(
            String id,
            String userId,
            String engagementLetterId,
            ConversationStatus status,
            String type,
            LocalDateTime createdAt,
            Integer lastSequenceNumber
    ) {
        this.id = id;
        this.userId = userId;
        this.engagementLetterId = engagementLetterId;
        this.status = status != null ? status : ConversationStatus.ACTIVE;
        this.type = type;
        this.createdAt = createdAt;
        this.lastSequenceNumber = lastSequenceNumber != null ? lastSequenceNumber : 0;
    }

    public static ConversationEntity fromConversation(Conversation conversation) {
        return new ConversationEntity(conversation);
    }

    public Conversation toConversation() {
        return Conversation.builder()
                .id(this.id)
                .userId(this.userId)
                .engagementLetterId(this.engagementLetterId)
                .status(this.status)
                .type(this.type != null ? ConversationType.valueOf(this.type) : null)
                .createdAt(this.createdAt)
                .lastSequenceNumber(this.lastSequenceNumber != null ? this.lastSequenceNumber : 0)
                .build();
    }
}

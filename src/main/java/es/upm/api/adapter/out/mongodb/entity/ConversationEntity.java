package es.upm.api.adapter.out.mongodb.entity;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.model.Conversation;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Objects;

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
        Objects.requireNonNull(conversation, "conversation must not be null");
        this.id = conversation.getId();
        this.userId = Objects.requireNonNull(conversation.getUserId(), "conversation.userId must not be null");
        this.engagementLetterId = conversation.getEngagementLetterId();
        this.status = Objects.requireNonNull(conversation.getStatus(), "conversation.status must not be null");
        this.type = Objects.requireNonNull(conversation.getType(), "conversation.type must not be null").name();
        this.createdAt = Objects.requireNonNull(conversation.getCreatedAt(), "conversation.createdAt must not be null");
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
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
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
                .status(Objects.requireNonNull(this.status, "status must not be null"))
                .type(ConversationType.valueOf(Objects.requireNonNull(this.type, "type must not be null")))
                .createdAt(Objects.requireNonNull(this.createdAt, "createdAt must not be null"))
                .lastSequenceNumber(this.lastSequenceNumber != null ? this.lastSequenceNumber : 0)
                .build();
    }
}

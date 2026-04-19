package es.upm.api.infrastructure.mongodb.entities;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.model.Conversation;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.BeanUtils;
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

    public ConversationEntity(Conversation conversation) {
        BeanUtils.copyProperties(conversation, this);
        this.userId = conversation.getUserId();
    }

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
        this.status = status != null ? status : ConversationStatus.ACTIVE;
        this.type = type;
        this.createdAt = createdAt;
    }

    public static ConversationEntity fromConversation(Conversation conversation) {
        ConversationEntity entity = new ConversationEntity();
        BeanUtils.copyProperties(conversation, entity);

        return entity;
    }

    public Conversation toConversation() {
        Conversation conversation = new Conversation();
        BeanUtils.copyProperties(this, conversation);

        return conversation;
    }
}

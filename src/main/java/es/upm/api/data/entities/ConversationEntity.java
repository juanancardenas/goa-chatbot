package es.upm.api.data.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "conversations")
public class ConversationEntity {

    @Id
    private String id;
    private String userId;
    private String engagementLetterId;
    private String type;
    private LocalDateTime createdAt;

    public ConversationEntity() {
    }

    public ConversationEntity(
            String id,
            String userId,
            String engagementLetterId,
            String type,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.engagementLetterId = engagementLetterId;
        this.type = type;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getEngagementLetterId() {
        return engagementLetterId;
    }

    public String getType() {
        return type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setEngagementLetterId(String engagementLetterId) {
        this.engagementLetterId = engagementLetterId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}

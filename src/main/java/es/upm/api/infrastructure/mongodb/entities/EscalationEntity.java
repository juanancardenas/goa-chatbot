package es.upm.api.infrastructure.mongodb.entities;

import es.upm.api.domain.model.Escalation;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@Document(collection = "escalations")
public class EscalationEntity {
    @Id
    private UUID id;
    @NotNull
    private String conversationId;
    @NotNull
    private String userId;
    @NotNull
    private LocalDateTime createdAt;
    private String phone;
    private String email;

    public EscalationEntity(Escalation escalation) {
        BeanUtils.copyProperties(escalation, this);
    }

    public EscalationEntity(
            UUID id,
            String conversationId,
            String userId,
            LocalDateTime createdAt,
            String phone,
            String email
    ) {
        this.id = id;
        this.conversationId = conversationId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.phone = phone;
        this.email = email;
    }

    public static EscalationEntity fromEscalation(Escalation escalation) {
        EscalationEntity entity = new EscalationEntity();
        BeanUtils.copyProperties(escalation, entity);

        return entity;
    }

    public Escalation toEscalation() {
        Escalation escalation = new Escalation();
        BeanUtils.copyProperties(this, escalation);

        return escalation;
    }
}

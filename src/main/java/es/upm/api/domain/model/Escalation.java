package es.upm.api.domain.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Escalation {

    private UUID id;
    @NotNull
    private String conversationId;
    @NotNull
    private String user;
    @NotNull
    private LocalDateTime createdAt;
    private String phone;
    private String email;
}

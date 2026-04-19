package es.upm.api.domain.model;

import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
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
public class Message {

    private String id;
    @NotNull
    private String conversationId;
    @NotNull
    private MessageSenderType senderType;
    @NotNull
    private MessageType messageType;
    @NotNull
    private String content;
    @NotNull
    private LocalDateTime timestamp;
    @NotNull
    private Integer sequenceNumber;
    private String parentMessageId;
}

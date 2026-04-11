package es.upm.api.data.entities;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "messages")
public class MessageEntity {
    @Id
    private String id;
    @Indexed
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

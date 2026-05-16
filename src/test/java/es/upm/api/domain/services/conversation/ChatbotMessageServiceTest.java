package es.upm.api.domain.services.conversation;

import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.domain.model.Message;
import es.upm.api.domain.model.configuration.ChatbotHistoryMessageResult;
import es.upm.api.domain.ports.out.ConversationGateway;
import es.upm.api.domain.ports.out.MessageGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotMessageServiceTest {

    @Mock
    private MessageGateway messageGateway;

    @Mock
    private ConversationGateway conversationGateway;

    @InjectMocks
    private ChatbotMessageService chatbotMessageService;

    @Test
    void saveMessageShouldPersistBuiltMessageAndReturnGeneratedId() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 5, 15, 10, 30);
        when(this.messageGateway.createAndReturnId(any(Message.class))).thenReturn("message-id");

        String createdMessageId = this.chatbotMessageService.saveMessage(
                "conversation-1",
                MessageSenderType.USER,
                MessageType.REQUEST,
                "Hola",
                3,
                "parent-1",
                timestamp
        );

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(this.messageGateway).createAndReturnId(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();

        assertThat(createdMessageId).isEqualTo("message-id");
        assertThat(savedMessage.getId()).isNotBlank();
        assertThat(savedMessage.getConversationId()).isEqualTo("conversation-1");
        assertThat(savedMessage.getSenderType()).isEqualTo(MessageSenderType.USER);
        assertThat(savedMessage.getMessageType()).isEqualTo(MessageType.REQUEST);
        assertThat(savedMessage.getContent()).isEqualTo("Hola");
        assertThat(savedMessage.getSequenceNumber()).isEqualTo(3);
        assertThat(savedMessage.getParentMessageId()).isEqualTo("parent-1");
        assertThat(savedMessage.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void reserveSequenceNumbersShouldDelegateToConversationGateway() {
        when(this.conversationGateway.reserveSequenceNumbers("conversation-2", 2)).thenReturn(8);

        Integer firstReservedSequenceNumber = this.chatbotMessageService.reserveSequenceNumbers("conversation-2", 2);

        assertThat(firstReservedSequenceNumber).isEqualTo(8);
        verify(this.conversationGateway).reserveSequenceNumbers("conversation-2", 2);
    }

    @Test
    void toHistoryMessageResultShouldMapDomainMessageFields() {
        Message message = Message.builder()
                .id("message-1")
                .conversationId("conversation-1")
                .senderType(MessageSenderType.ASSISTANT)
                .messageType(MessageType.RESPONSE)
                .content("Respuesta")
                .timestamp(LocalDateTime.of(2026, 5, 15, 9, 45))
                .sequenceNumber(4)
                .parentMessageId("message-0")
                .build();

        ChatbotHistoryMessageResult result = this.chatbotMessageService.toHistoryMessageResult(message);

        assertThat(result.getId()).isEqualTo("message-1");
        assertThat(result.getConversationId()).isEqualTo("conversation-1");
        assertThat(result.getSenderType()).isEqualTo("ASSISTANT");
        assertThat(result.getMessageType()).isEqualTo("RESPONSE");
        assertThat(result.getContent()).isEqualTo("Respuesta");
        assertThat(result.getTimestamp()).isEqualTo("2026-05-15T09:45");
        assertThat(result.getSequenceNumber()).isEqualTo(4);
        assertThat(result.getParentMessageId()).isEqualTo("message-0");
    }

    @Test
    void readRecentMessagesForPromptShouldReturnLastMessagesWithTrimmedContent() {
        when(this.messageGateway.findByConversationIdOrdered("conversation-3"))
                .thenReturn(List.of(
                        Message.builder()
                                .senderType(MessageSenderType.USER)
                                .content("  Primer mensaje  ")
                                .build(),
                        Message.builder()
                                .senderType(MessageSenderType.ASSISTANT)
                                .content("Respuesta previa")
                                .build(),
                        Message.builder()
                                .senderType(MessageSenderType.USER)
                                .content(" \u00daltima pregunta ")
                                .build()
                ));

        List<String> recentMessages = this.chatbotMessageService.readRecentMessagesForPrompt("conversation-3", 2);

        assertThat(recentMessages).containsExactly(
                "ASSISTANT: Respuesta previa",
                "USER: \u00daltima pregunta"
        );
    }

    @Test
    void readRecentMessagesForPromptShouldUseEmptyContentWhenMessageContentIsNullOrBlank() {
        when(this.messageGateway.findByConversationIdOrdered("conversation-blank-content"))
                .thenReturn(List.of(
                        Message.builder()
                                .senderType(MessageSenderType.USER)
                                .content(null)
                                .build(),
                        Message.builder()
                                .senderType(MessageSenderType.ASSISTANT)
                                .content("   ")
                                .build()
                ));

        List<String> recentMessages = this.chatbotMessageService.readRecentMessagesForPrompt(
                "conversation-blank-content",
                5
        );

        assertThat(recentMessages).containsExactly(
                "USER: ",
                "ASSISTANT: "
        );
    }

    @Test
    void readRecentMessagesForPromptShouldReturnEmptyListWhenGatewayReturnsNullOrEmptyMessages() {
        when(this.messageGateway.findByConversationIdOrdered("conversation-null")).thenReturn(null);
        when(this.messageGateway.findByConversationIdOrdered("conversation-empty")).thenReturn(List.of());

        assertThat(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-null", 5)).isEmpty();
        assertThat(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-empty", 5)).isEmpty();
    }

    @Test
    void readRecentMessagesForPromptShouldReturnEmptyListWhenGatewayFails() {
        when(this.messageGateway.findByConversationIdOrdered("conversation-4"))
                .thenThrow(new RuntimeException("Mongo unavailable"));

        assertThat(this.chatbotMessageService.readRecentMessagesForPrompt("conversation-4", 5)).isEmpty();
    }
}

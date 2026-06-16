package es.upm.api.domain.services.conversation;

import es.upm.api.domain.enums.ConversationType;

import es.upm.api.domain.enums.ConversationStatus;
import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.Message;
import es.upm.api.domain.model.chatbot.result.ChatbotConversationHistoryResult;
import es.upm.api.domain.model.chatbot.result.ChatbotConversationSummaryResult;
import es.upm.api.domain.model.chatbot.result.ChatbotHistoryMessageResult;
import es.upm.api.domain.model.pagination.PageResult;
import es.upm.api.domain.ports.out.ConversationGateway;
import es.upm.api.domain.ports.out.MessageGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotHistoryServiceTest {

    @Mock
    private ConversationGateway conversationGateway;

    @Mock
    private MessageGateway messageGateway;

    @Mock
    private ChatbotConversationService chatbotConversationService;

    @Mock
    private ChatbotMessageService chatbotMessageService;

    @InjectMocks
    private ChatbotHistoryService chatbotHistoryService;

    @Test
    void readConversationHistoryListShouldReturnGeneralSummariesWithLatestMessagePreview() {
        Conversation latestConversation = Conversation.builder()
                .id("conversation-1")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 15, 9, 0))
                .build();
        Conversation olderConversation = Conversation.builder()
                .id("conversation-2")
                .userId("professional-1")
                .status(ConversationStatus.CLOSED)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 14, 9, 0))
                .build();

        when(this.conversationGateway.findByUserIdAndTypeOrderByCreatedAtDesc("professional-1", ConversationType.GENERAL))
                .thenReturn(List.of(latestConversation, olderConversation));
        when(this.messageGateway.findLatestByConversationId("conversation-1"))
                .thenReturn(Optional.of(
                        Message.builder()
                                .id("message-1")
                                .conversationId("conversation-1")
                                .content("Resumen reciente")
                                .timestamp(LocalDateTime.of(2026, Month.MAY, 15, 10, 15))
                                .build()
                ));
        when(this.messageGateway.findLatestByConversationId("conversation-2")).thenReturn(Optional.empty());

        List<ChatbotConversationSummaryResult> response = this.chatbotHistoryService.readConversationHistoryList(
                "professional-1",
                " general ",
                null
        );

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getConversationId()).isEqualTo("conversation-1");
        assertThat(response.get(0).getPreview()).isEqualTo("Resumen reciente");
        assertThat(response.get(0).getLastMessageAt()).isEqualTo("2026-05-15T10:15");
        assertThat(response.get(1).getConversationId()).isEqualTo("conversation-2");
        assertThat(response.get(1).getPreview()).isNull();
    }

    @Test
    void readConversationHistoryListShouldDefaultToGeneralWhenTypeIsBlank() {
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .userId("professional-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 15, 9, 0))
                .build();

        when(this.conversationGateway.findByUserIdAndTypeOrderByCreatedAtDesc("professional-1", ConversationType.GENERAL))
                .thenReturn(List.of(conversation));
        when(this.messageGateway.findLatestByConversationId("conversation-1")).thenReturn(Optional.empty());

        List<ChatbotConversationSummaryResult> response = this.chatbotHistoryService.readConversationHistoryList(
                "professional-1",
                "   ",
                null
        );

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getConversationId()).isEqualTo("conversation-1");
        verify(this.conversationGateway).findByUserIdAndTypeOrderByCreatedAtDesc("professional-1", ConversationType.GENERAL);
    }

    @Test
    void readConversationHistoryListShouldReturnAllContextualConversationsWhenEngagementLetterIdIsBlank() {
        Conversation conversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("customer-1")
                .engagementLetterId("EL-9")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 15, 12, 0))
                .build();

        when(this.conversationGateway.findByUserIdAndTypeOrderByCreatedAtDesc("customer-1", ConversationType.CONTEXTUAL))
                .thenReturn(List.of(conversation));
        when(this.messageGateway.findLatestByConversationId("conversation-ctx")).thenReturn(Optional.empty());

        List<ChatbotConversationSummaryResult> response = this.chatbotHistoryService.readConversationHistoryList(
                "customer-1",
                "CONTEXTUAL",
                " "
        );

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getConversationId()).isEqualTo("conversation-ctx");
        verify(this.conversationGateway).findByUserIdAndTypeOrderByCreatedAtDesc("customer-1", ConversationType.CONTEXTUAL);
        verify(this.conversationGateway, never())
                .findByUserIdAndEngagementLetterIdAndTypeOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    void readConversationHistoryListShouldUseSpecificContextualFinderWhenEngagementLetterIdIsPresent() {
        Conversation conversation = Conversation.builder()
                .id("conversation-ctx")
                .userId("customer-1")
                .engagementLetterId("EL-9")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 15, 12, 0))
                .build();

        when(this.conversationGateway.findByUserIdAndEngagementLetterIdAndTypeOrderByCreatedAtDesc(
                "customer-1",
                "EL-9",
                ConversationType.CONTEXTUAL
        )).thenReturn(List.of(conversation));
        when(this.messageGateway.findLatestByConversationId("conversation-ctx")).thenReturn(Optional.empty());

        List<ChatbotConversationSummaryResult> response = this.chatbotHistoryService.readConversationHistoryList(
                "customer-1",
                "contextual",
                "EL-9"
        );

        assertThat(response).hasSize(1);
        verify(this.conversationGateway)
                .findByUserIdAndEngagementLetterIdAndTypeOrderByCreatedAtDesc("customer-1", "EL-9", ConversationType.CONTEXTUAL);
    }

    @Test
    void readConversationHistoryListShouldRejectUnsupportedConversationType() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> this.chatbotHistoryService.readConversationHistoryList("user-1", "other", null)
        );

        assertThat(exception).hasMessageContaining("Tipo de conversacion no soportado: other");
    }

    @Test
    void readConversationHistoryShouldSortMessagesAscendingAndApplyDefaultPagination() {
        Conversation conversation = Conversation.builder()
                .id("conversation-history")
                .userId("customer-1")
                .engagementLetterId("EL-10")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.CONTEXTUAL)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 15, 8, 0))
                .build();

        Message newestInPage = Message.builder()
                .id("message-2")
                .conversationId("conversation-history")
                .senderType(MessageSenderType.ASSISTANT)
                .messageType(MessageType.RESPONSE)
                .content("Segundo")
                .timestamp(LocalDateTime.of(2026, Month.MAY, 15, 9, 30))
                .sequenceNumber(2)
                .parentMessageId("message-1")
                .build();
        Message oldestInPage = Message.builder()
                .id("message-1")
                .conversationId("conversation-history")
                .senderType(MessageSenderType.USER)
                .messageType(MessageType.REQUEST)
                .content("Primero")
                .timestamp(LocalDateTime.of(2026, Month.MAY, 15, 9, 0))
                .sequenceNumber(1)
                .build();

        when(this.chatbotConversationService.requireOwnedConversation("conversation-history", "customer-1"))
                .thenReturn(conversation);
        when(this.messageGateway.findByConversationIdOrderedDesc("conversation-history", 0, 20))
                .thenReturn(PageResult.<Message>builder()
                        .content(List.of(newestInPage, oldestInPage))
                        .page(0)
                        .size(20)
                        .hasNext(true)
                        .totalElements(12)
                        .build());
        when(this.chatbotMessageService.toHistoryMessageResult(oldestInPage))
                .thenReturn(ChatbotHistoryMessageResult.builder()
                        .id("message-1")
                        .sequenceNumber(1)
                        .build());
        when(this.chatbotMessageService.toHistoryMessageResult(newestInPage))
                .thenReturn(ChatbotHistoryMessageResult.builder()
                        .id("message-2")
                        .sequenceNumber(2)
                        .build());

        ChatbotConversationHistoryResult response = this.chatbotHistoryService.readConversationHistory(
                "customer-1",
                "conversation-history",
                null,
                null
        );

        assertThat(response.getConversationId()).isEqualTo("conversation-history");
        assertThat(response.getEngagementLetterId()).isEqualTo("EL-10");
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getHasMore()).isTrue();
        assertThat(response.getTotalMessages()).isEqualTo(12);
        assertThat(response.getMessages()).hasSize(2);
        assertThat(response.getMessages().get(0).getId()).isEqualTo("message-1");
        assertThat(response.getMessages().get(0).getSequenceNumber()).isEqualTo(1);
        assertThat(response.getMessages().get(1).getId()).isEqualTo("message-2");
        assertThat(response.getMessages().get(1).getSequenceNumber()).isEqualTo(2);
    }

    @Test
    void readConversationHistoryShouldDefaultNegativePageToFirstPage() {
        Conversation conversation = Conversation.builder()
                .id("conversation-history")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 15, 8, 0))
                .build();

        when(this.chatbotConversationService.requireOwnedConversation("conversation-history", "customer-1"))
                .thenReturn(conversation);
        when(this.messageGateway.findByConversationIdOrderedDesc("conversation-history", 0, 10))
                .thenReturn(PageResult.<Message>builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .hasNext(false)
                        .totalElements(0)
                        .build());

        ChatbotConversationHistoryResult response = this.chatbotHistoryService.readConversationHistory(
                "customer-1",
                "conversation-history",
                -1,
                10
        );

        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
        verify(this.messageGateway).findByConversationIdOrderedDesc("conversation-history", 0, 10);
    }

    @Test
    void readConversationHistoryShouldClampSizeToMaximum() {
        Conversation conversation = Conversation.builder()
                .id("conversation-history")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 15, 8, 0))
                .build();

        when(this.chatbotConversationService.requireOwnedConversation("conversation-history", "customer-1"))
                .thenReturn(conversation);
        when(this.messageGateway.findByConversationIdOrderedDesc("conversation-history", 0, 100))
                .thenReturn(PageResult.<Message>builder()
                        .content(List.of())
                        .page(0)
                        .size(100)
                        .hasNext(false)
                        .totalElements(0)
                        .build());

        ChatbotConversationHistoryResult response = this.chatbotHistoryService.readConversationHistory(
                "customer-1",
                "conversation-history",
                0,
                101
        );

        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(100);
        verify(this.messageGateway).findByConversationIdOrderedDesc("conversation-history", 0, 100);
    }

    @Test
    void readConversationHistoryShouldDefaultSizeWhenBelowMinimum() {
        Conversation conversation = Conversation.builder()
                .id("conversation-history")
                .userId("customer-1")
                .status(ConversationStatus.ACTIVE)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 15, 8, 0))
                .build();

        when(this.chatbotConversationService.requireOwnedConversation("conversation-history", "customer-1"))
                .thenReturn(conversation);
        when(this.messageGateway.findByConversationIdOrderedDesc("conversation-history", 0, 20))
                .thenReturn(PageResult.<Message>builder()
                        .content(List.of())
                        .page(0)
                        .size(20)
                        .hasNext(false)
                        .totalElements(0)
                        .build());

        ChatbotConversationHistoryResult response = this.chatbotHistoryService.readConversationHistory(
                "customer-1",
                "conversation-history",
                0,
                0
        );

        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        verify(this.messageGateway).findByConversationIdOrderedDesc("conversation-history", 0, 20);
    }

    @Test
    void readConversationHistoryListShouldExposeNullStatusWhenConversationHasNoStatus() {
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .userId("professional-1")
                .status(null)
                .type(ConversationType.GENERAL)
                .createdAt(LocalDateTime.of(2026, Month.MAY, 15, 9, 0))
                .build();

        when(this.conversationGateway.findByUserIdAndTypeOrderByCreatedAtDesc("professional-1", ConversationType.GENERAL))
                .thenReturn(List.of(conversation));
        when(this.messageGateway.findLatestByConversationId("conversation-1")).thenReturn(Optional.empty());

        List<ChatbotConversationSummaryResult> response = this.chatbotHistoryService.readConversationHistoryList(
                "professional-1",
                null,
                null
        );

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getStatus()).isNull();
    }
}

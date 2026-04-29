package es.upm.api.infrastructure.resources;

import es.upm.api.domain.services.ChatbotService;
import es.upm.api.infrastructure.dtos.ChatbotContextualConversationRequestDto;
import es.upm.api.infrastructure.dtos.ChatbotContextualConversationResponseDto;
import es.upm.api.infrastructure.dtos.ChatbotConversationSummaryDto;
import es.upm.api.infrastructure.dtos.ChatbotConversationResponseDto;
import es.upm.api.infrastructure.dtos.ChatbotConversationMessageResponseDto;
import es.upm.api.infrastructure.dtos.ChatbotMessageRequestDto;
import es.upm.api.infrastructure.dtos.ChatbotMessageResponseDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ChatbotResource.CHATBOT)
public class ChatbotResource {
    public static final String CHATBOT = "/chatbot";
    public static final String CONVERSATIONS = "/conversations";
    public static final String MESSAGES = "/messages";
    public static final String CONTEXTUAL_CONVERSATIONS = "/conversations/contextual";
    public static final String GENERAL_CONVERSATIONS = "/conversations/general";
    public static final String CLOSE_CONVERSATION = "/conversations/{conversationId}/close";
    public static final String REOPEN_CONVERSATION = "/conversations/{conversationId}/reopen";

    private final ChatbotService chatbotService;

    @Autowired
    public ChatbotResource(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @GetMapping(CONVERSATIONS)
    public List<ChatbotConversationSummaryDto> readConversations(
            @RequestParam String type,
            @RequestParam(required = false) String engagementLetterId
    ) {
        return this.chatbotService.readConversationHistoryList(type, engagementLetterId);
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @PostMapping(CONTEXTUAL_CONVERSATIONS)
    public ChatbotContextualConversationResponseDto startContextualConversation(
            @Valid @RequestBody ChatbotContextualConversationRequestDto requestDto
    ) {
        return this.chatbotService.startContextualConversation(requestDto);
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @PostMapping(GENERAL_CONVERSATIONS)
    public ChatbotMessageResponseDto startGeneralConversation(@Valid @RequestBody ChatbotMessageRequestDto requestDto) {
        return this.chatbotService.startGeneralConversation(requestDto);
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @PostMapping(MESSAGES)
    public ChatbotMessageResponseDto sendMessage(@Valid @RequestBody ChatbotMessageRequestDto requestDto) {
        return this.chatbotService.sendMessage(requestDto);
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @PatchMapping(CLOSE_CONVERSATION)
    public ResponseEntity<Void> closeConversation(@PathVariable String conversationId) {
        this.chatbotService.closeConversation(conversationId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @PatchMapping(REOPEN_CONVERSATION)
    public ResponseEntity<Void> reopenConversation(@PathVariable String conversationId) {
        this.chatbotService.reopenConversation(conversationId);
        return ResponseEntity.noContent().build();
    }
}
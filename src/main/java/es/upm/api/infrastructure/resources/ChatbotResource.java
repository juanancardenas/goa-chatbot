package es.upm.api.infrastructure.resources;

import es.upm.api.infrastructure.dtos.ChatbotContextualConversationRequestDto;
import es.upm.api.infrastructure.dtos.ChatbotContextualConversationResponseDto;
import es.upm.api.infrastructure.dtos.ChatbotConversationResponseDto;
import es.upm.api.infrastructure.dtos.ChatbotConversationMessageResponseDto;
import es.upm.api.infrastructure.dtos.ChatbotMessageRequestDto;
import es.upm.api.infrastructure.dtos.ChatbotMessageResponseDto;
import es.upm.api.domain.services.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ChatbotResource.CHATBOT)
public class ChatbotResource {
    public static final String CHATBOT = "/chatbot";
    public static final String MESSAGES = "/messages";
    public static final String CONVERSATIONS = "/conversations";
    public static final String CONVERSATION = "/conversations/{conversationId}";
    public static final String CONVERSATION_MESSAGES = "/conversations/{conversationId}/messages";
    public static final String CONTEXTUAL_CONVERSATIONS = "/conversations/contextual";
    public static final String GENERAL_CONVERSATIONS = "/conversations/general";
    public static final String CLOSE_CONVERSATION = "/conversations/{conversationId}/close";

    private final ChatbotService chatbotService;

    @Autowired
    public ChatbotResource(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
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
    @GetMapping(CONVERSATIONS)
    public List<ChatbotConversationResponseDto> readUserConversations() {
        return this.chatbotService.readUserConversations();
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @GetMapping(CONVERSATION)
    public ChatbotConversationResponseDto readConversation(@PathVariable String conversationId) {
        return this.chatbotService.readConversation(conversationId);
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @GetMapping(CONVERSATION_MESSAGES)
    public List<ChatbotConversationMessageResponseDto> readConversationMessages(@PathVariable String conversationId) {
        return this.chatbotService.readConversationMessages(conversationId);
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @PatchMapping(CLOSE_CONVERSATION)
    public ResponseEntity<Void> closeConversation(@PathVariable String conversationId) {
        this.chatbotService.closeConversation(conversationId);
        return ResponseEntity.noContent().build();
    }
}

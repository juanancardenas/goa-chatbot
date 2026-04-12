package es.upm.api.resources;

import es.upm.api.resources.dtos.ChatbotContextualConversationRequestDto;
import es.upm.api.resources.dtos.ChatbotContextualConversationResponseDto;
import es.upm.api.resources.dtos.ChatbotMessageRequestDto;
import es.upm.api.resources.dtos.ChatbotMessageResponseDto;
import es.upm.api.services.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ChatbotResource.CHATBOT)
public class ChatbotResource {
    public static final String CHATBOT = "/chatbot";
    public static final String MESSAGES = "/messages";
    public static final String CONTEXTUAL_CONVERSATIONS = "/conversations/contextual";
    public static final String GENERAL_CONVERSATIONS = "/conversations/general";

    private final ChatbotService chatbotService;

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
}

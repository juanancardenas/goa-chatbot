package es.upm.api.infrastructure.resources;

import es.upm.api.domain.services.ChatbotService;
import es.upm.api.infrastructure.dtos.ChatbotConfigurationStatusDto;
import es.upm.api.infrastructure.dtos.ChatbotContextualConversationRequestDto;
import es.upm.api.infrastructure.dtos.ChatbotContextualConversationResponseDto;
import es.upm.api.infrastructure.dtos.ChatbotConversationHistoryResponseDto;
import es.upm.api.infrastructure.dtos.ChatbotConversationSummaryDto;
import es.upm.api.infrastructure.dtos.ChatbotMessageRequestDto;
import es.upm.api.infrastructure.dtos.ChatbotMessageResponseDto;
import es.upm.api.infrastructure.security.AuthenticatedUserContextResolver;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping(ChatbotResource.CHATBOT)
public class ChatbotResource {
    public static final String CHATBOT = "/chatbot";
    public static final String CONVERSATIONS = "/conversations";
    public static final String MESSAGES = "/messages";
    public static final String CONTEXTUAL_CONVERSATIONS = "/conversations/contextual";
    public static final String GENERAL_CONVERSATIONS = "/conversations/general";
    public static final String CONVERSATION_MESSAGES = "/conversations/{conversationId}/messages";
    public static final String DELETE_CONVERSATION = "/conversations/{conversationId}";
    public static final String CLOSE_CONVERSATION = "/conversations/{conversationId}/close";
    public static final String REOPEN_CONVERSATION = "/conversations/{conversationId}/reopen";
    public static final String ESCALATE_CONVERSATION = "/conversations/{conversationId}/escalate";
    public static final String CONFIGURATION_STATUS = "/configuration/status";

    private final ChatbotService chatbotService;
    private final AuthenticatedUserContextResolver authenticatedUserContextResolver;

    @Autowired
    public ChatbotResource(
            ChatbotService chatbotService,
            AuthenticatedUserContextResolver authenticatedUserContextResolver
    ) {
        this.chatbotService = chatbotService;
        this.authenticatedUserContextResolver = authenticatedUserContextResolver;
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @GetMapping(CONVERSATIONS)
    public List<ChatbotConversationSummaryDto> readConversations(
            @RequestParam String type,
            @RequestParam(required = false) String engagementLetterId,
            Authentication authentication
    ) {
        return this.chatbotService.readConversationHistoryList(
                        this.authenticatedUserContextResolver.resolve(authentication),
                        type,
                        engagementLetterId
                )
                .stream()
                .map(ChatbotConversationSummaryDto::fromDomain)
                .toList();
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @PostMapping(CONTEXTUAL_CONVERSATIONS)
    public ChatbotContextualConversationResponseDto startContextualConversation(
            @Valid @RequestBody ChatbotContextualConversationRequestDto requestDto,
            Authentication authentication
    ) {
        return ChatbotContextualConversationResponseDto.fromDomain(
                this.chatbotService.startContextualConversation(
                        this.authenticatedUserContextResolver.resolve(authentication),
                        requestDto.toCommand()
                )
        );
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @PostMapping(GENERAL_CONVERSATIONS)
    public ChatbotMessageResponseDto startGeneralConversation(
            @Valid @RequestBody ChatbotMessageRequestDto requestDto,
            Authentication authentication
    ) {
        return ChatbotMessageResponseDto.fromDomain(
                this.chatbotService.startGeneralConversation(
                        this.authenticatedUserContextResolver.resolve(authentication),
                        requestDto.toCommand()
                )
        );
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @PostMapping(MESSAGES)
    public ChatbotMessageResponseDto sendMessage(
            @Valid @RequestBody ChatbotMessageRequestDto requestDto,
            Authentication authentication
    ) {
        return ChatbotMessageResponseDto.fromDomain(
                this.chatbotService.sendMessage(
                        this.authenticatedUserContextResolver.resolve(authentication),
                        requestDto.toCommand()
                )
        );
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @GetMapping(CONFIGURATION_STATUS)
    public ChatbotConfigurationStatusDto readConfigurationStatus() {
        return ChatbotConfigurationStatusDto.fromDomain(this.chatbotService.readConfigurationStatus());
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @GetMapping(CONVERSATION_MESSAGES)
    public ChatbotConversationHistoryResponseDto readConversationHistory(
            @PathVariable String conversationId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication
    ) {
        return ChatbotConversationHistoryResponseDto.fromDomain(
                this.chatbotService.readConversationHistory(
                        this.authenticatedUserContextResolver.resolve(authentication),
                        conversationId,
                        page,
                        size
                )
        );
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @DeleteMapping(DELETE_CONVERSATION)
    public ResponseEntity<Void> deleteConversation(
            @PathVariable String conversationId,
            Authentication authentication
    ) {
        this.chatbotService.deleteConversation(
                this.authenticatedUserContextResolver.resolve(authentication),
                conversationId
        );

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @PatchMapping(CLOSE_CONVERSATION)
    public ResponseEntity<Void> closeConversation(
            @PathVariable String conversationId,
            Authentication authentication
    ) {
        this.chatbotService.closeConversation(
                this.authenticatedUserContextResolver.resolve(authentication),
                conversationId
        );

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @PatchMapping(REOPEN_CONVERSATION)
    public ResponseEntity<Void> reopenConversation(
            @PathVariable String conversationId,
            Authentication authentication
    ) {
        this.chatbotService.reopenConversation(
                this.authenticatedUserContextResolver.resolve(authentication),
                conversationId
        );

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @PatchMapping(ESCALATE_CONVERSATION)
    public ResponseEntity<Void> escalateConversation(
            @PathVariable String conversationId,
            Authentication authentication
    ) {
        this.chatbotService.escalateConversation(
                this.authenticatedUserContextResolver.resolve(authentication),
                conversationId
        );

        return ResponseEntity.noContent().build();
    }
}

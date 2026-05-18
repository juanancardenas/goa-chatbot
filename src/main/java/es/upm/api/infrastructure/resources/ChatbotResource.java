package es.upm.api.infrastructure.resources;

import es.upm.api.domain.ports.in.CloseConversationUseCase;
import es.upm.api.domain.ports.in.DeleteConversationUseCase;
import es.upm.api.domain.ports.in.EscalateConversationUseCase;
import es.upm.api.domain.ports.in.ReadChatbotConfigurationUseCase;
import es.upm.api.domain.ports.in.ReadConversationHistoryListUseCase;
import es.upm.api.domain.ports.in.ReadConversationHistoryUseCase;
import es.upm.api.domain.ports.in.ReopenConversationUseCase;
import es.upm.api.domain.ports.in.SendChatbotMessageUseCase;
import es.upm.api.domain.ports.in.StartContextualConversationUseCase;
import es.upm.api.domain.ports.in.StartGeneralConversationUseCase;
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

    // Constants defining the endpoints
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

    // Attributes to implement the in ports
    private final ReadConversationHistoryListUseCase readConversationHistoryListUseCase;
    private final StartContextualConversationUseCase startContextualConversationUseCase;
    private final StartGeneralConversationUseCase startGeneralConversationUseCase;
    private final SendChatbotMessageUseCase sendChatbotMessageUseCase;
    private final ReadChatbotConfigurationUseCase readChatbotConfigurationUseCase;
    private final ReadConversationHistoryUseCase readConversationHistoryUseCase;
    private final DeleteConversationUseCase deleteConversationUseCase;
    private final CloseConversationUseCase closeConversationUseCase;
    private final ReopenConversationUseCase reopenConversationUseCase;
    private final EscalateConversationUseCase escalateConversationUseCase;

    // Attribute for authentication
    private final AuthenticatedUserContextResolver authenticatedUserContextResolver;

    @Autowired
    public ChatbotResource(
            ReadConversationHistoryListUseCase readConversationHistoryListUseCase,
            StartContextualConversationUseCase startContextualConversationUseCase,
            StartGeneralConversationUseCase startGeneralConversationUseCase,
            SendChatbotMessageUseCase sendChatbotMessageUseCase,
            ReadChatbotConfigurationUseCase readChatbotConfigurationUseCase,
            ReadConversationHistoryUseCase readConversationHistoryUseCase,
            DeleteConversationUseCase deleteConversationUseCase,
            CloseConversationUseCase closeConversationUseCase,
            ReopenConversationUseCase reopenConversationUseCase,
            EscalateConversationUseCase escalateConversationUseCase,
            AuthenticatedUserContextResolver authenticatedUserContextResolver
    ) {
        this.readConversationHistoryListUseCase = readConversationHistoryListUseCase;
        this.startContextualConversationUseCase = startContextualConversationUseCase;
        this.startGeneralConversationUseCase = startGeneralConversationUseCase;
        this.sendChatbotMessageUseCase = sendChatbotMessageUseCase;
        this.readChatbotConfigurationUseCase = readChatbotConfigurationUseCase;
        this.readConversationHistoryUseCase = readConversationHistoryUseCase;
        this.deleteConversationUseCase = deleteConversationUseCase;
        this.closeConversationUseCase = closeConversationUseCase;
        this.reopenConversationUseCase = reopenConversationUseCase;
        this.escalateConversationUseCase = escalateConversationUseCase;
        this.authenticatedUserContextResolver = authenticatedUserContextResolver;
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @GetMapping(CONVERSATIONS)
    public List<ChatbotConversationSummaryDto> readConversations(
            @RequestParam String type,
            @RequestParam(required = false) String engagementLetterId,
            Authentication authentication
    ) {
        return this.readConversationHistoryListUseCase.readConversationHistoryList(
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
                this.startContextualConversationUseCase.startContextualConversation(
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
                this.startGeneralConversationUseCase.startGeneralConversation(
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
                this.sendChatbotMessageUseCase.sendMessage(
                        this.authenticatedUserContextResolver.resolve(authentication),
                        requestDto.toCommand()
                )
        );
    }

    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR_CUSTOMER)
    @GetMapping(CONFIGURATION_STATUS)
    public ChatbotConfigurationStatusDto readConfigurationStatus() {
        return ChatbotConfigurationStatusDto.fromDomain(this.readChatbotConfigurationUseCase.readConfigurationStatus());
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
                this.readConversationHistoryUseCase.readConversationHistory(
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
        this.deleteConversationUseCase.deleteConversation(
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
        this.closeConversationUseCase.closeConversation(
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
        this.reopenConversationUseCase.reopenConversation(
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
        this.escalateConversationUseCase.escalateConversation(
                this.authenticatedUserContextResolver.resolve(authentication),
                conversationId
        );

        return ResponseEntity.noContent().build();
    }
}

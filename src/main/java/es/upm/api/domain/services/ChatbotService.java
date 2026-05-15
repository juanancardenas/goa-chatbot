package es.upm.api.domain.services;

import es.upm.api.configurations.ChatbotAiProperties;
import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.MessageSenderType;
import es.upm.api.domain.enums.MessageType;
import es.upm.api.domain.enums.PlatformQuestionType;
import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.model.ai.ChatbotAiRequest;
import es.upm.api.domain.model.ai.ChatbotAiResponse;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.ports.out.ChatbotAiClient;
import es.upm.api.domain.services.classification.ChatbotQuestionClassifier;
import es.upm.api.domain.services.conversation.*;
import es.upm.api.domain.services.policies.ChatbotScopeDecision;
import es.upm.api.domain.services.policies.ChatbotScopePolicy;
import es.upm.api.domain.common.ChatbotResponseMessages;
import es.upm.api.domain.model.configuration.ChatbotConfigurationStatus;
import es.upm.api.domain.model.configuration.ChatbotMessageCommand;
import es.upm.api.domain.model.configuration.ChatbotMessageResult;
import es.upm.api.domain.model.configuration.ChatbotContextualConversationCommand;
import es.upm.api.domain.model.configuration.ChatbotContextualConversationResult;
import es.upm.api.domain.model.configuration.ChatbotConversationHistoryResult;
import es.upm.api.domain.model.configuration.ChatbotConversationSummaryResult;
import es.upm.api.domain.model.configuration.AuthenticatedUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatbotService {

    // Constants
    private static final String TYPE_CONTEXTUAL = "CONTEXTUAL";

    private static final String RESPONSE_MODE_GENERAL = "GENERAL";
    private static final String RESPONSE_MODE_CONTEXTUAL_PLATFORM_DATA = "CONTEXTUAL_PLATFORM_DATA";
    private static final String RESPONSE_MODE_CONTEXTUAL_RESTRICTED = "CONTEXTUAL_RESTRICTED";
    private static final Pattern ENGAGEMENT_ID_PATTERN = Pattern.compile("\\bEL-\\d+\\b", Pattern.CASE_INSENSITIVE);

    // Attributes
    private final ChatbotMessageService chatbotMessageService;
    private final ChatbotResponseSanitizer chatbotResponseSanitizer;
    private final ChatbotConversationService chatbotConversationService;
    private final ChatbotHistoryService chatbotHistoryService;
    private final ChatbotEscalationService chatbotEscalationService;
    private final ChatbotDocumentContextService chatbotDocumentContextService;
    private final ChatbotPlatformContextService chatbotPlatformContextService;
    private final ChatbotQuestionClassifier chatbotQuestionClassifier;
    private final ChatbotScopePolicy chatbotScopePolicy;
    private final ChatbotAiProperties chatbotAiProperties;

    private final ChatbotAiClient chatbotAiClient;


    // Constructors
    @Autowired
    public ChatbotService(ChatbotMessageService chatbotMessageService,
                          ChatbotResponseSanitizer chatbotResponseSanitizer,
                          ChatbotConversationService chatbotConversationService,
                          ChatbotHistoryService chatbotHistoryService,
                          ChatbotEscalationService chatbotEscalationService,
                          ChatbotDocumentContextService chatbotDocumentContextService,
                          ChatbotPlatformContextService chatbotPlatformContextService,
                          ChatbotQuestionClassifier chatbotQuestionClassifier,
                          ChatbotScopePolicy chatbotScopePolicy,
                          ChatbotAiProperties chatbotAiProperties,
                          ChatbotAiClient chatbotAiClient
    ) {
        this.chatbotMessageService = chatbotMessageService;
        this.chatbotResponseSanitizer = chatbotResponseSanitizer;
        this.chatbotConversationService = chatbotConversationService;
        this.chatbotHistoryService = chatbotHistoryService;
        this.chatbotEscalationService = chatbotEscalationService;
        this.chatbotDocumentContextService = chatbotDocumentContextService;
        this.chatbotPlatformContextService = chatbotPlatformContextService;
        this.chatbotQuestionClassifier = chatbotQuestionClassifier;
        this.chatbotScopePolicy = chatbotScopePolicy;
        this.chatbotAiProperties = chatbotAiProperties;
        this.chatbotAiClient = chatbotAiClient;
    }

    /**
     * Starts Contextual Conversation, this type of conversation is receiving an EngagementLetter ID
     */
    public ChatbotContextualConversationResult startContextualConversation(
            AuthenticatedUserContext authenticatedUser,
            ChatbotContextualConversationCommand command
    ) {
        Conversation conversation = this.chatbotConversationService.findOrCreateContextualConversation(
                authenticatedUser.getUserId(),
                command.getEngagementLetterId()
        );

        return ChatbotContextualConversationResult.builder()
                .conversationId(conversation.getId())
                .engagementLetterId(conversation.getEngagementLetterId())
                .createdAt(conversation.getCreatedAt().toString())
                .error(null)
                .build();
    }

    /**
     * Reading Conversation History List to display historic of conversations
     */
    public List<ChatbotConversationSummaryResult> readConversationHistoryList(
            AuthenticatedUserContext authenticatedUser,
            String type,
            String engagementLetterId
    ) {
        return this.chatbotHistoryService.readConversationHistoryList(
                authenticatedUser.getUserId(),
                type,
                engagementLetterId
        );
    }

    /**
     * Reading the messages of a conversation
     */
    public ChatbotConversationHistoryResult readConversationHistory(
            AuthenticatedUserContext authenticatedUser,
            String conversationId,
            Integer page,
            Integer size
    ) {
        return this.chatbotHistoryService.readConversationHistory(
                authenticatedUser.getUserId(),
                conversationId,
                page,
                size
        );
    }

    /**
     * Starts General Conversation, this type of conversation is not linked to other process or entity
     */
    public ChatbotMessageResult startGeneralConversation(
            AuthenticatedUserContext authenticatedUser,
            ChatbotMessageCommand command
    ) {
        String userMessage = command.getMessage();

        this.validateUserMessageLength(userMessage);

        LocalDateTime date = LocalDateTime.now();

        Conversation conversation = this.chatbotConversationService.createGeneralConversation(
                authenticatedUser.getUserId(),
                date
        );

        String messageId = this.chatbotMessageService.saveMessage(
                conversation.getId(),
                MessageSenderType.USER,
                MessageType.REQUEST,
                userMessage,
                1,
                null,
                date
        );

        ConversationProfileType profile = authenticatedUser.getProfile();
        String baseReply = this.generalStartReply(profile);

        String assistantReply = this.generateConfiguredAssistantReply(
                conversation,
                profile,
                userMessage,
                baseReply,
                Optional.empty()
        );

        this.chatbotMessageService.saveMessage(
                conversation.getId(),
                MessageSenderType.ASSISTANT,
                MessageType.RESPONSE,
                assistantReply,
                2,
                messageId,
                date
        );

        return ChatbotMessageResult.builder()
                .conversationId(conversation.getId())
                .message(assistantReply)
                .error(null)
                .createdAt(date.toString())
                .responseMode(RESPONSE_MODE_GENERAL)
                .usedPlatformData(false)
                .sourcesSummary(List.of())
                .build();
    }

    private ChatbotMessageResult buildMessageResult(
            String conversationId,
            String message,
            String error,
            LocalDateTime createdAt,
            String responseMode,
            boolean usedPlatformData,
            List<String> sourcesSummary
    ) {
        return ChatbotMessageResult.builder()
                .conversationId(conversationId)
                .message(message)
                .error(error)
                .createdAt(createdAt.toString())
                .responseMode(responseMode)
                .usedPlatformData(usedPlatformData)
                .sourcesSummary(sourcesSummary)
                .build();
    }

    // Send Message: method called each time that user clicks on Sent button in the front-end
    public ChatbotMessageResult sendMessage(
            AuthenticatedUserContext authenticatedUser,
            ChatbotMessageCommand command
    ) {
        String userId = authenticatedUser.getUserId();
        LocalDateTime date = LocalDateTime.now();
        String userMessage = command.getMessage();

        if (command.getConversationId() == null || command.getConversationId().isBlank()) {
            throw new BadRequestException("conversationId es obligatorio para enviar mensajes");
        }

        this.validateUserMessageLength(userMessage);

        Conversation conversation = this.chatbotConversationService.requireActiveOwnedConversation(
                command.getConversationId(),
                userId
        );

        Integer nextSequence = this.chatbotMessageService.nextSequenceNumber(conversation.getId());

        String messageId = this.chatbotMessageService.saveMessage(
                conversation.getId(),
                MessageSenderType.USER,
                MessageType.REQUEST,
                userMessage,
                nextSequence,
                null,
                date
        );

        ChatbotScopeDecision scopeDecision = this.chatbotScopePolicy.evaluate(
                conversation,
                userMessage
        );

        String assistantReply;
        String responseMode;
        boolean usedPlatformData;
        List<String> sourcesSummary;
        ConversationProfileType profile = authenticatedUser.getProfile();

        if (this.isCourtesyMessage(userMessage)) {
            assistantReply = this.courtesyReply(profile);
            responseMode = RESPONSE_MODE_GENERAL;
            usedPlatformData = false;
            sourcesSummary = List.of();

            this.chatbotMessageService.saveMessage(
                    conversation.getId(),
                    MessageSenderType.ASSISTANT,
                    MessageType.RESPONSE,
                    assistantReply,
                    nextSequence + 1,
                    messageId,
                    date
            );

            return this.buildMessageResult(
                    conversation.getId(),
                    assistantReply,
                    null,
                    date,
                    responseMode,
                    usedPlatformData,
                    sourcesSummary
            );
        }

        if (this.referencesAnotherEngagement(conversation, userMessage)) {
            assistantReply = ChatbotResponseMessages.OUT_OF_CASE_SCOPE_REPLY;
            responseMode = RESPONSE_MODE_CONTEXTUAL_RESTRICTED;
            usedPlatformData = false;
            sourcesSummary = List.of();

            this.chatbotMessageService.saveMessage(
                    conversation.getId(),
                    MessageSenderType.ASSISTANT,
                    MessageType.RESPONSE,
                    assistantReply,
                    nextSequence + 1,
                    messageId,
                    date
            );

            return this.buildMessageResult(
                    conversation.getId(),
                    assistantReply,
                    null,
                    date,
                    responseMode,
                    usedPlatformData,
                    sourcesSummary
            );
        }

        if (scopeDecision.isAllowed()) {
            if (TYPE_CONTEXTUAL.equals(conversation.getType()) && conversation.getEngagementLetterId() != null) {
                PlatformQuestionType questionType = this.chatbotQuestionClassifier.classify(userMessage);

                if (this.requiresPlatformContext(questionType)) {
                    Optional<ChatbotPlatformContext> platformContext = this.chatbotPlatformContextService
                            .loadContext(conversation.getEngagementLetterId());

                    if (platformContext.isPresent()) {
                        String baseReply = this.contextualPlatformReply(
                                profile,
                                userMessage,
                                conversation,
                                platformContext.get()
                        );

                        assistantReply = this.generateConfiguredAssistantReply(
                                conversation,
                                profile,
                                userMessage,
                                baseReply,
                                platformContext
                        );

                        responseMode = RESPONSE_MODE_CONTEXTUAL_PLATFORM_DATA;
                        usedPlatformData = true;
                        sourcesSummary = platformContext.get().getSourcesSummary();
                    } else {
                        String baseReply = this.contextualFallbackReply(profile, userMessage);

                        assistantReply = this.generateConfiguredAssistantReply(
                                conversation,
                                profile,
                                userMessage,
                                baseReply,
                                Optional.empty()
                        );

                        responseMode = RESPONSE_MODE_CONTEXTUAL_RESTRICTED;
                        usedPlatformData = false;
                        sourcesSummary = List.of();
                    }
                } else {
                    String baseReply = this.generalFaqReply(profile, userMessage);

                    assistantReply = this.generateConfiguredAssistantReply(
                            conversation,
                            profile,
                            userMessage,
                            baseReply,
                            Optional.empty()
                    );

                    responseMode = RESPONSE_MODE_GENERAL;
                    usedPlatformData = false;
                    sourcesSummary = List.of();
                }
            } else {
                String baseReply = this.generalFaqReply(profile, userMessage);

                assistantReply = this.generateConfiguredAssistantReply(
                        conversation,
                        profile,
                        userMessage,
                        baseReply,
                        Optional.empty()
                );

                responseMode = RESPONSE_MODE_GENERAL;
                usedPlatformData = false;
                sourcesSummary = List.of();
            }
        } else {
            assistantReply = scopeDecision.getSafeMessage();
            responseMode = TYPE_CONTEXTUAL.equals(conversation.getType())
                    ? RESPONSE_MODE_CONTEXTUAL_RESTRICTED
                    : RESPONSE_MODE_GENERAL;
            usedPlatformData = false;
            sourcesSummary = List.of();
        }

        assistantReply = this.chatbotResponseSanitizer.normalizeReplyForFrontend(assistantReply);

        this.chatbotMessageService.saveMessage(
                conversation.getId(),
                MessageSenderType.ASSISTANT,
                MessageType.RESPONSE,
                assistantReply,
                nextSequence + 1,
                messageId,
                date
        );

        return this.buildMessageResult(
                conversation.getId(),
                assistantReply,
                null,
                date,
                responseMode,
                usedPlatformData,
                sourcesSummary
        );
    }

    public ChatbotConfigurationStatus readConfigurationStatus() {
        return ChatbotConfigurationStatus.builder()
                .enabled(this.chatbotAiProperties.isEnabled())
                .provider(this.chatbotAiProperties.normalizedProvider())
                .model(this.chatbotAiProperties.getModel())
                .maxInputCharacters(this.chatbotAiProperties.getMaxInputCharacters())
                .maxOutputTokens(this.chatbotAiProperties.getMaxOutputTokens())
                .maxContextMessages(this.chatbotAiProperties.getMaxContextMessages())
                .documentsAvailable(this.chatbotAiProperties.isDocumentsAvailable())
                .build();
    }

    /**
     * Close a conversation
     * It will be triggered once the user leaves the chatbot or selects a
     * different conversation to reopen (previous would be closed)
     */
    public void closeConversation(
            AuthenticatedUserContext authenticatedUser,
            String conversationId
    ) {
        this.chatbotConversationService.closeConversation(
                conversationId,
                authenticatedUser.getUserId()
        );
    }

    /**
     * Escalate a conversation
     * It will be triggered once the user selects this option, creating a new
     * entry in the table escalations and locking the conversation
     */
    public void escalateConversation(
            AuthenticatedUserContext authenticatedUser,
            String conversationId
    ) {
        this.chatbotEscalationService.escalateConversation(
                conversationId,
                authenticatedUser.getUserId()
        );
    }

    /**
     * Delete a conversation
     * It will be triggered from list of conversations, if user runs delete action
     */
    public void deleteConversation(
            AuthenticatedUserContext authenticatedUser,
            String conversationId
    ) {
        this.chatbotConversationService.deleteConversation(
                conversationId,
                authenticatedUser.getUserId()
        );
    }

    /**
     * Reopen a conversation
     * It will be triggered from list of conversations, once user selects one
     */
    public void reopenConversation(
            AuthenticatedUserContext authenticatedUser,
            String conversationId
    ) {
        this.chatbotConversationService.reopenConversation(
                conversationId,
                authenticatedUser.getUserId()
        );
    }

    private boolean requiresPlatformContext(PlatformQuestionType questionType) {
        if (questionType == null) {
            return true;
        }

        return switch (questionType) {
            case ENGAGEMENT_STATUS, LEGAL_TASKS, TIMELINE_EVENTS, DOCUMENTS, GENERAL_CONTEXT -> true;
        };
    }

    private String generateConfiguredAssistantReply(
            Conversation conversation,
            ConversationProfileType profile,
            String userMessage,
            String baseReply,
            Optional<ChatbotPlatformContext> platformContext
    ) {
        if (!this.chatbotAiProperties.isEnabled()) {
            return baseReply;
        }

        try {
            ChatbotAiRequest aiRequest = ChatbotAiRequest.builder()
                    .conversationId(conversation.getId())
                    .userId(conversation.getUserId())
                    .userMessage(this.buildAiUserMessage(conversation, userMessage, baseReply, platformContext))
                    .basePrompt(this.chatbotAiProperties.getBasePrompt())
                    .roleProfile(profile.name())
                    .conversationType(conversation.getType())
                    .platformContext(this.buildPlatformContextForPrompt(platformContext))
                    .recentMessages(
                            this.chatbotMessageService.readRecentMessagesForPrompt(
                                    conversation.getId(),
                                    this.chatbotAiProperties.getMaxContextMessages()
                            )
                    )
                    .model(this.chatbotAiProperties.getModel())
                    .maxOutputTokens(this.chatbotAiProperties.getMaxOutputTokens())
                    .temperature(this.chatbotAiProperties.getTemperature())
                    .documentsAvailable(this.chatbotAiProperties.isDocumentsAvailable())
                    .build();

            ChatbotAiResponse aiResponse = this.chatbotAiClient.generate(aiRequest);

            if (aiResponse == null || aiResponse.getError() != null) {
                return baseReply;
            }

            if (aiResponse.getContent() == null || aiResponse.getContent().isBlank()) {
                return baseReply;
            }

            return aiResponse.getContent().trim();
        } catch (RuntimeException ignored) {
            return baseReply;
        }
    }

    private String buildAiUserMessage(
            Conversation conversation,
            String userMessage,
            String baseReply,
            Optional<ChatbotPlatformContext> platformContext
    ) {
        String contextualRules = "";

        if (TYPE_CONTEXTUAL.equals(conversation.getType())) {
            String activeEngagementId = platformContext
                    .map(ChatbotPlatformContext::getEngagementLetterId)
                    .orElse(this.safeText(conversation.getEngagementLetterId(), "No disponible"));

            contextualRules = """
                Reglas adicionales para chat contextual:
                - Este chat está asociado al encargo activo: %s.
                - No respondas con datos de otros encargos, expedientes o casos.
                - Si el usuario pide comparar con otro encargo o salir de este ámbito, indícalo con claridad y mantén el foco en el encargo activo.
                - Evita copiar la respuesta base como plantilla literal; úsala solo como guardrail y redacta una respuesta natural.
                - Responde con tono de abogado cercano, amable y servicial.
                - Cierra la respuesta con una sugerencia útil o una pregunta breve para continuar ayudando.
                """.formatted(activeEngagementId);
        }

        return """
            Pregunta actual del usuario:
            %s

            Respuesta base segura generada por GOA:
            %s

            Usa la respuesta base como guía de seguridad, no como texto obligatorio.
            Si la pregunta es general, hipotética, explicativa o pide ejemplos, puedes desarrollar una respuesta útil.
            Mantén un tono amable, claro y profesional.
            Puedes sonar cercano, pero no uses bromas excesivas ni lenguaje demasiado informal.
            Si el usuario pide datos reales de un encargo, expediente, documento, hito, estado o tarea concreta, responde solo si esos datos están disponibles en el contexto.
            No inventes datos reales de plataforma.
            No inventes documentos, estados, hitos, fechas ni tareas de un encargo concreto.
            No proporciones asesoramiento legal vinculante.
            Si el usuario pide una tabla, gráfico, diagrama o formato que dependa de Markdown/renderizado especial, indica brevemente que en esta versión de la interfaz aún no está disponible.
            Después, ofrece la alternativa en forma de lista clara y útil.
            Responde únicamente a la pregunta actual del usuario.
            No repitas respuestas anteriores salvo que el usuario lo pida explícitamente.
            No arrastres contexto anterior si no es relevante para la pregunta actual.
            Si generas listas, usa saltos de línea y viñetas simples.
            No generes tablas en texto con separadores " | ".
            No generes tablas Markdown.
            No generes bloques pseudo-gráficos.
            No uses sintaxis Markdown de negrita como **texto**.
            Devuelve únicamente la respuesta final para el usuario.
            No escribas títulos como "Respuesta mejorada", "Respuesta final" o similares.
            %s
            """.formatted(
                this.safeText(userMessage, "No disponible"),
                this.safeText(baseReply, "No disponible"),
                contextualRules
        );
    }

    private boolean asksForSpecificEngagementData(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String normalized = message.toLowerCase();

        return normalized.contains("este encargo")
                || normalized.contains("mi encargo")
                || normalized.contains("del encargo")
                || normalized.contains("de un encargo")
                || normalized.contains("este caso")
                || normalized.contains("mi caso")
                || normalized.contains("del caso")
                || normalized.contains("esta hoja de encargo")
                || normalized.contains("mi hoja de encargo")
                || normalized.contains("del expediente")
                || normalized.contains("mi expediente")
                || normalized.contains("este expediente");
    }

    private String buildPlatformContextForPrompt(Optional<ChatbotPlatformContext> platformContext) {
        if (platformContext.isEmpty()) {
            return "No hay contexto de plataforma disponible.";
        }

        ChatbotPlatformContext context = platformContext.get();

        String procedures = context.getProcedureTitles() == null || context.getProcedureTitles().isEmpty()
                ? "No disponible"
                : String.join(", ", context.getProcedureTitles());

        String legalTasks = context.getLegalTaskSummaries() == null || context.getLegalTaskSummaries().isEmpty()
                ? "No disponible"
                : String.join(System.lineSeparator(), context.getLegalTaskSummaries());

        String events = context.getRecentEventSummaries() == null || context.getRecentEventSummaries().isEmpty()
                ? "No disponible"
                : String.join(System.lineSeparator(), context.getRecentEventSummaries());

        String sources = context.getSourcesSummary() == null || context.getSourcesSummary().isEmpty()
                ? "No disponible"
                : String.join(System.lineSeparator(), context.getSourcesSummary());

        return """
            EngagementLetterId: %s
            Cliente/propietario visible: %s
            Procedimientos: %s
        
            Tareas legales:
            %s
        
            Eventos recientes:
            %s
        
            Fuentes internas disponibles:
            %s
            """.formatted(
                this.safeText(context.getEngagementLetterId(), "No disponible"),
                this.safeText(context.getOwnerDisplayName(), "No disponible"),
                procedures,
                legalTasks,
                events,
                sources
        );
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private String generalStartReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_START_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_START_REPLY;
        };
    }

    private String contextualPlatformReply(
            ConversationProfileType profile,
            String userMessage,
            Conversation conversation,
            ChatbotPlatformContext platformContext
    ) {
        PlatformQuestionType questionType = this.classifyQuestion(userMessage);

        return switch (questionType) {
            case ENGAGEMENT_STATUS -> this.buildEngagementStatusReply(profile, platformContext);
            case LEGAL_TASKS -> this.buildLegalTasksReply(profile, platformContext);
            case TIMELINE_EVENTS -> this.buildTimelineReply(profile, platformContext);
            case DOCUMENTS -> this.buildDocumentsReply(profile, conversation, platformContext);
            case GENERAL_CONTEXT -> this.buildGeneralContextReply(profile, platformContext);
        };
    }

    private String buildEngagementStatusReply(
            ConversationProfileType profile,
            ChatbotPlatformContext platformContext
    ) {
        String base = switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_STATUS_REPLY_TEMPLATE.formatted(
                    platformContext.getEngagementLetterId(),
                    platformContext.getOwnerDisplayName()
            );
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_STATUS_REPLY_TEMPLATE.formatted(
                    platformContext.getEngagementLetterId(),
                    platformContext.getOwnerDisplayName()
            );
        };

        StringBuilder reply = new StringBuilder(base);

        if (platformContext.getProcedureTitles() != null && !platformContext.getProcedureTitles().isEmpty()) {
            String procedures = String.join(", ", platformContext.getProcedureTitles());
            String proceduresReply = switch (profile) {
                case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_PROCEDURES_REPLY_TEMPLATE.formatted(procedures);
                case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_PROCEDURES_REPLY_TEMPLATE.formatted(procedures);
            };
            reply.append(" ").append(proceduresReply);
        }

        return reply.toString();
    }

    private String buildTimelineReply(
            ConversationProfileType profile,
            ChatbotPlatformContext platformContext
    ) {
        boolean hasRecentEvents = platformContext.getRecentEventSummaries() != null
                && !platformContext.getRecentEventSummaries().isEmpty();

        boolean hasProcedures = platformContext.getProcedureTitles() != null
                && !platformContext.getProcedureTitles().isEmpty();

        StringBuilder reply = new StringBuilder();

        if (hasRecentEvents) {
            String recentEvents = String.join(", ", platformContext.getRecentEventSummaries());
            reply.append(
                    switch (profile) {
                        case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_EVENTS_REPLY_TEMPLATE.formatted(recentEvents);
                        case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_EVENTS_REPLY_TEMPLATE.formatted(recentEvents);
                    }
            );
        } else {
            reply.append(
                    switch (profile) {
                        case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_NO_EVENTS_REPLY;
                        case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_NO_EVENTS_REPLY;
                    }
            );
        }

        if (hasProcedures) {
            String procedures = String.join(", ", platformContext.getProcedureTitles());
            reply.append(" ").append(
                    switch (profile) {
                        case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_PROCEDURES_REPLY_TEMPLATE.formatted(procedures);
                        case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_PROCEDURES_REPLY_TEMPLATE.formatted(procedures);
                    }
            );
        }

        return reply.toString();
    }

    private String buildDocumentsReply(
            ConversationProfileType profile,
            Conversation conversation,
            ChatbotPlatformContext platformContext
    ) {
        var documentContext = this.chatbotDocumentContextService.loadDocumentContext(conversation);

        StringBuilder reply = new StringBuilder(
                switch (profile) {
                    case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_DOCUMENTS_STUB_REPLY;
                    case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_DOCUMENTS_STUB_REPLY;
                }
        );

        if (platformContext.getProcedureTitles() != null && !platformContext.getProcedureTitles().isEmpty()) {
            String procedures = String.join(", ", platformContext.getProcedureTitles());
            reply.append(" ").append(
                    switch (profile) {
                        case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_PROCEDURES_REPLY_TEMPLATE.formatted(procedures);
                        case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_PROCEDURES_REPLY_TEMPLATE.formatted(procedures);
                    }
            );
        }

        if (documentContext != null
                && documentContext.getVisibleDocumentTitles() != null
                && !documentContext.getVisibleDocumentTitles().isEmpty()) {
            reply.append(" Documentos visibles preparados para futura integración: ")
                    .append(String.join(", ", documentContext.getVisibleDocumentTitles()))
                    .append(".");
        }

        return reply.toString();
    }

    private String buildLegalTasksReply(
            ConversationProfileType profile,
            ChatbotPlatformContext platformContext
    ) {
        boolean hasLegalTasks = platformContext.getLegalTaskSummaries() != null
                && !platformContext.getLegalTaskSummaries().isEmpty();

        if (!hasLegalTasks) {
            return switch (profile) {
                case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_NO_LEGAL_TASKS_REPLY;
                case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_NO_LEGAL_TASKS_REPLY;
            };
        }

        String legalTasks = platformContext.getLegalTaskSummaries().stream()
                .map(task -> "- " + task)
                .collect(Collectors.joining(System.lineSeparator()));

        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_LEGAL_TASKS_REPLY_TEMPLATE.formatted(legalTasks);
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_LEGAL_TASKS_REPLY_TEMPLATE.formatted(legalTasks);
        };
    }

    private String buildGeneralContextReply(
            ConversationProfileType profile,
            ChatbotPlatformContext platformContext
    ) {
        StringBuilder reply = new StringBuilder(
                switch (profile) {
                    case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_GENERAL_SUMMARY_REPLY;
                    case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_GENERAL_SUMMARY_REPLY;
                }
        );

        reply.append(" ").append(this.buildEngagementStatusReply(profile, platformContext));

        if (platformContext.getRecentEventSummaries() != null && !platformContext.getRecentEventSummaries().isEmpty()) {
            String recentEvents = String.join(", ", platformContext.getRecentEventSummaries());
            String eventsReply = switch (profile) {
                case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_EVENTS_REPLY_TEMPLATE.formatted(recentEvents);
                case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_EVENTS_REPLY_TEMPLATE.formatted(recentEvents);
            };
            reply.append(" ").append(eventsReply);
        } else {
            String noEventsReply = switch (profile) {
                case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXTUAL_NO_EVENTS_REPLY;
                case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXTUAL_NO_EVENTS_REPLY;
            };
            reply.append(" ").append(noEventsReply);
        }

        if (platformContext.getLegalTaskSummaries() != null && !platformContext.getLegalTaskSummaries().isEmpty()) {
            reply.append(" ").append(this.buildLegalTasksReply(profile, platformContext));
        }

        return reply.toString();
    }

    private String contextualFallbackReply(
            ConversationProfileType profile,
            String userMessage
    ) {
        PlatformQuestionType questionType = this.chatbotQuestionClassifier.classify(userMessage);

        if (questionType == null) {
            return ChatbotResponseMessages.CONTEXTUAL_PLATFORM_DATA_UNAVAILABLE_REPLY;
        }

        return switch (questionType) {
            case ENGAGEMENT_STATUS -> this.buildContextUnavailableStatusReply(profile);
            case TIMELINE_EVENTS -> this.buildContextUnavailableEventsReply(profile);
            case DOCUMENTS -> this.buildContextUnavailableDocumentsReply(profile);
            case LEGAL_TASKS -> this.buildContextUnavailableLegalTasksReply(profile);
            case GENERAL_CONTEXT -> this.buildContextUnavailableGeneralReply(profile);
        };
    }

    private String buildContextUnavailableStatusReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_STATUS_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_STATUS_REPLY;
        };
    }

    private String buildContextUnavailableEventsReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_EVENTS_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_EVENTS_REPLY;
        };
    }

    private String buildContextUnavailableDocumentsReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_DOCUMENTS_STUB_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_DOCUMENTS_STUB_REPLY;
        };
    }

    private String buildContextUnavailableLegalTasksReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_LEGAL_TASKS_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_LEGAL_TASKS_REPLY;
        };
    }

    private String buildContextUnavailableGeneralReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_CONTEXT_UNAVAILABLE_GENERAL_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_CONTEXT_UNAVAILABLE_GENERAL_REPLY;
        };
    }

    private PlatformQuestionType classifyQuestion(String userMessage) {
        return Optional.ofNullable(this.chatbotQuestionClassifier.classify(userMessage))
                .orElse(PlatformQuestionType.GENERAL_CONTEXT);
    }

    private String generalFaqReply(
            ConversationProfileType profile,
            String userMessage
    ) {
        PlatformQuestionType questionType = this.classifyQuestion(userMessage);

        return switch (questionType) {
            case ENGAGEMENT_STATUS -> this.buildGeneralStatusReply(profile, userMessage);
            case LEGAL_TASKS -> this.buildGeneralLegalTasksReply(profile, userMessage);
            case TIMELINE_EVENTS -> this.buildGeneralTimelineReply(profile, userMessage);
            case DOCUMENTS -> this.buildGeneralDocumentsReply(profile);
            case GENERAL_CONTEXT -> this.buildGeneralContextReply(profile);
        };
    }

    private String buildGeneralStatusReply(ConversationProfileType profile, String message) {
        if (this.asksForSpecificEngagementData(message)) {
            return switch (profile) {
                case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_STATUS_REPLY;
                case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_STATUS_REPLY;
            };
        }

        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_STATUS_EXAMPLE_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_STATUS_EXAMPLE_REPLY;
        };
    }

    private String buildGeneralTimelineReply(ConversationProfileType profile, String message) {
        if (this.asksForSpecificEngagementData(message)) {
            return switch (profile) {
                case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_TIMELINE_REPLY;
                case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_TIMELINE_REPLY;
            };
        }

        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_TIMELINE_EXAMPLE_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_TIMELINE_EXAMPLE_REPLY;
        };
    }

    private String buildGeneralDocumentsReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_DOCUMENTS_STUB_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_DOCUMENTS_STUB_REPLY;
        };
    }

    private String buildGeneralLegalTasksReply(ConversationProfileType profile, String message) {
        if (this.asksForSpecificEngagementData(message)) {
            return switch (profile) {
                case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_LEGAL_TASKS_REPLY;
                case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_LEGAL_TASKS_REPLY;
            };
        }

        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_LEGAL_TASKS_EXAMPLE_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_LEGAL_TASKS_EXAMPLE_REPLY;
        };
    }

    private String buildGeneralContextReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_GENERAL_CONTEXT_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_GENERAL_CONTEXT_REPLY;
        };
    }

    private void validateUserMessageLength(String message) {
        int maxInputCharacters = this.chatbotAiProperties.getMaxInputCharacters();

        if (maxInputCharacters > 0 && message != null && message.length() > maxInputCharacters) {
            throw new BadRequestException("message supera el limite maximo de caracteres configurado");
        }
    }

    private boolean isCourtesyMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String normalized = message.toLowerCase(Locale.ROOT);

        return normalized.contains("gracias")
                || normalized.contains("muchas gracias")
                || normalized.contains("por favor")
                || normalized.contains("te quiero")
                || normalized.contains("te amo")
                || normalized.contains("buen dia")
                || normalized.contains("buen día")
                || normalized.contains("buenas")
                || normalized.contains("hasta luego")
                || normalized.contains("nos vemos");
    }

    private String courtesyReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_COURTESY_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_COURTESY_REPLY;
        };
    }

    private boolean referencesAnotherEngagement(Conversation conversation, String message) {
        if (!TYPE_CONTEXTUAL.equals(conversation.getType()) || message == null || message.isBlank()) {
            return false;
        }

        String activeEngagementId = this.safeText(conversation.getEngagementLetterId(), "");
        Matcher matcher = ENGAGEMENT_ID_PATTERN.matcher(message);

        while (matcher.find()) {
            String candidate = matcher.group();
            if (!candidate.equalsIgnoreCase(activeEngagementId)) {
                return true;
            }
        }

        return false;
    }
}

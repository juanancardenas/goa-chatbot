package es.upm.api.domain.services.aireply;

import es.upm.api.configurations.ChatbotAiProperties;
import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.enums.ConversationType;
import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.ai.ChatbotAiRequest;
import es.upm.api.domain.model.ai.ChatbotAiResponse;
import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.ports.out.ChatbotAiClient;
import es.upm.api.domain.services.conversation.ChatbotMessageService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ChatbotAiReplyService {

    private static final String TEXT_NOT_AVAILABLE = "No disponible";

    private final ChatbotAiClient chatbotAiClient;
    private final ChatbotAiProperties chatbotAiProperties;
    private final ChatbotMessageService chatbotMessageService;

    public ChatbotAiReplyService(
            ChatbotAiClient chatbotAiClient,
            ChatbotAiProperties chatbotAiProperties,
            ChatbotMessageService chatbotMessageService
    ) {
        this.chatbotAiClient = chatbotAiClient;
        this.chatbotAiProperties = chatbotAiProperties;
        this.chatbotMessageService = chatbotMessageService;
    }

    public String generateConfiguredAssistantReply(
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
                    .conversationType(conversation.getType().name())
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

        if (ConversationType.CONTEXTUAL == conversation.getType()) {
            String activeEngagementId = platformContext
                    .map(ChatbotPlatformContext::getEngagementLetterId)
                    .orElse(this.safeText(conversation.getEngagementLetterId(), TEXT_NOT_AVAILABLE));

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
                this.safeText(userMessage, TEXT_NOT_AVAILABLE),
                this.safeText(baseReply, TEXT_NOT_AVAILABLE),
                contextualRules
        );
    }

    private String buildPlatformContextForPrompt(Optional<ChatbotPlatformContext> platformContext) {
        if (platformContext.isEmpty()) {
            return "No hay contexto de plataforma disponible.";
        }

        ChatbotPlatformContext context = platformContext.get();

        String procedures = context.getProcedureTitles() == null || context.getProcedureTitles().isEmpty()
                ? TEXT_NOT_AVAILABLE
                : String.join(", ", context.getProcedureTitles());

        String legalTasks = context.getLegalTaskSummaries() == null || context.getLegalTaskSummaries().isEmpty()
                ? TEXT_NOT_AVAILABLE
                : String.join(System.lineSeparator(), context.getLegalTaskSummaries());

        String events = context.getRecentEventSummaries() == null || context.getRecentEventSummaries().isEmpty()
                ? TEXT_NOT_AVAILABLE
                : String.join(System.lineSeparator(), context.getRecentEventSummaries());

        String sources = context.getSourcesSummary() == null || context.getSourcesSummary().isEmpty()
                ? TEXT_NOT_AVAILABLE
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
                this.safeText(context.getEngagementLetterId(), TEXT_NOT_AVAILABLE),
                this.safeText(context.getOwnerDisplayName(), TEXT_NOT_AVAILABLE),
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
}

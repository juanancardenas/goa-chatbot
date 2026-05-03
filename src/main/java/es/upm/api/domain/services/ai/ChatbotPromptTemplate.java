package es.upm.api.domain.services.ai;

import es.upm.api.configurations.ChatbotAiProperties;
import es.upm.api.domain.model.ai.ChatbotAiRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

@Service
public class ChatbotPromptTemplate {
    private static final String NOT_AVAILABLE = "No disponible";
    private static final String TYPE_CONTEXTUAL = "CONTEXTUAL";

    private final ChatbotAiProperties chatbotAiProperties;

    public ChatbotPromptTemplate(ChatbotAiProperties chatbotAiProperties) {
        this.chatbotAiProperties = chatbotAiProperties;
    }

    public String buildSystemPrompt(ChatbotAiRequest request) {
        StringJoiner prompt = new StringJoiner(System.lineSeparator() + System.lineSeparator());

        prompt.add(this.safeText(request.getBasePrompt(), this.chatbotAiProperties.getBasePrompt()));
        prompt.add(this.buildConversationTypeSection(request));
        prompt.add(this.buildRoleSection(request));
        prompt.add(this.buildScopeSection());
        prompt.add(this.buildDocumentsSection(request));
        prompt.add(this.buildPlatformContextSection(request));
        prompt.add(this.buildHistorySection(request));

        return prompt.toString();
    }

    private String buildConversationTypeSection(ChatbotAiRequest request) {
        String conversationType = this.safeText(request.getConversationType(), "GENERAL");

        if (TYPE_CONTEXTUAL.equalsIgnoreCase(conversationType)) {
            return """
                    [TIPO DE CONVERSACIÓN]
                    Conversación contextual.
                    Usa el contexto del encargo asociado cuando esté disponible.
                    Si el contexto del encargo no está disponible, indícalo claramente.
                    No inventes tareas legales, estados, hitos, documentos, eventos ni fechas.
                    """;
        }

        return """
                [TIPO DE CONVERSACIÓN]
                Conversación general.
                Responde como apoyo conversacional jurídico seguro dentro de GOA.
                Mantén un tono técnico, claro, cercano y profesional.
                Debes sonar como un abogado que explica bien y cae bien, sin perder seriedad.
                Puedes explicar conceptos, generar ejemplos, listas o guías generales.
                Si el usuario pide tabla, gráfico o formato Markdown, indica que esa visualización aún no está disponible en esta interfaz y ofrece una lista clara como alternativa.
                No uses sintaxis Markdown de negrita como **texto**.
                No asumas que existe un encargo concreto si el usuario no lo menciona explícitamente.
                Si el usuario pide datos reales de un encargo específico, indícale que entre en Hojas de Encargo y abra el Asistente de ese encargo.
                No sustituyes a un abogado real ni emites asesoramiento legal vinculante.
                """;
    }

    private String buildRoleSection(ChatbotAiRequest request) {
        return """
                [PERFIL DEL USUARIO]
                Rol conversacional: %s
                Si el rol es CLIENT, usa lenguaje sencillo y guiado.
                Si el rol es PROFESSIONAL, usa lenguaje técnico y operativo.
                """.formatted(this.safeText(request.getRoleProfile(), NOT_AVAILABLE));
    }

    private String buildScopeSection() {
        return """
                [RESTRICCIÓN DE ÁMBITO]
                Responde únicamente dentro del ámbito de GOA, del encargo y de la información autorizada.
                Si el usuario pide algo fuera de ámbito, responde de forma prudente e indica la limitación.
                No proporciones asesoramiento legal vinculante.
                No inventes información.
                """;
    }

    private String buildDocumentsSection(ChatbotAiRequest request) {
        boolean documentsAvailable = Boolean.TRUE.equals(request.getDocumentsAvailable());

        if (documentsAvailable) {
            return """
                    [DISPONIBILIDAD DOCUMENTAL]
                    El servicio documental está disponible.
                    Usa únicamente documentos autorizados y fuentes proporcionadas por la plataforma.
                    """;
        }

        return """
                [DISPONIBILIDAD DOCUMENTAL]
                El servicio documental no está disponible actualmente.
                No afirmes haber leído documentos reales.
                No inventes contenido documental.
                Si el usuario pregunta por documentos, indica que no tienes acceso al servicio documental.
                Puedes apoyarte únicamente en datos disponibles del encargo, hitos o eventos.
                """;
    }

    private String buildPlatformContextSection(ChatbotAiRequest request) {
        String platformContext = this.safeText(request.getPlatformContext(), NOT_AVAILABLE);

        return """
            [CONTEXTO DE PLATAFORMA]
            Usa los siguientes datos solo si están disponibles y son suficientes.
            Si el contexto aparece como "No disponible", no inventes datos de plataforma.
            Si el usuario pregunta por legal tasks, legal task, tareas legales, tarea legal, estados, hitos, eventos o fechas, responde solo si esa información está presente en el contexto.
            Si existe una sección "Legal Tasks", "Tareas Legales", úsala como fuente principal para responder preguntas sobre tareas legales del encargo.

            %s
            """.formatted(platformContext);
    }

    private String buildHistorySection(ChatbotAiRequest request) {
        List<String> recentMessages = request.getRecentMessages();

        if (recentMessages == null || recentMessages.isEmpty()) {
            return """
                    [HISTORIAL RECIENTE]
                    No hay mensajes recientes disponibles.
                    """;
        }

        int maxMessages = Math.min(
                recentMessages.size(),
                this.chatbotAiProperties.getMaxContextMessages()
        );

        String history = String.join(
                System.lineSeparator(),
                recentMessages.subList(Math.max(0, recentMessages.size() - maxMessages), recentMessages.size())
        );

        return """
                [HISTORIAL RECIENTE]
                El historial sirve solo para continuidad conversacional.
                No repitas información anterior si la pregunta actual no la solicita.
                No uses el historial para responder sobre datos del encargo si el contexto de plataforma no está disponible.
        
                %s
                """.formatted(history);
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }
}

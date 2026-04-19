package es.upm.api.domain.services.policies;

import es.upm.api.domain.model.Conversation;
import es.upm.api.infrastructure.mongodb.entities.ConversationEntity;
import es.upm.api.domain.enums.ChatbotScopeViolationReason;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ChatbotScopePolicy {
    private static final String TYPE_CONTEXTUAL = "CONTEXTUAL";
    private static final String TYPE_GENERAL = "GENERAL";

    private static final List<String> LEGAL_BINDING_PATTERNS = List.of(
            "legal vinculante",
            "vinculante",
            "qué debo alegar",
            "que debo alegar",
            "qué tengo que alegar",
            "que tengo que alegar",
            "redáctame una demanda",
            "redactame una demanda",
            "redáctame una respuesta definitiva",
            "redactame una respuesta definitiva",
            "estrategia legal",
            "qué pasará exactamente",
            "que pasara exactamente",
            "garantízame",
            "garantizame",
            "confirma que esta cláusula es legal",
            "confirma que esta clausula es legal",
            "dime exactamente qué hacer",
            "dime exactamente que hacer"
    );

    private static final List<String> CONTEXT_REQUIRED_PATTERNS = List.of(
            "estado de mi encargo",
            "estado del encargo",
            "mi abogado ya presentó",
            "mi abogado ya presento",
            "se presentó el escrito",
            "se presento el escrito",
            "qué documentos hay",
            "que documentos hay",
            "qué hitos hay",
            "que hitos hay",
            "qué pasó en mi caso",
            "que paso en mi caso",
            "próximos pasos de mi caso",
            "proximos pasos de mi caso"
    );

    private static final List<String> OTHER_CASE_PATTERNS = List.of(
            "otro caso",
            "otro encargo",
            "mi otro caso",
            "mi otro encargo",
            "otro expediente",
            "además de este caso",
            "ademas de este caso"
    );

    public ChatbotScopeDecision evaluate(Conversation conversation, String message) {
        String normalizedMessage = this.normalize(message);

        if (normalizedMessage.isBlank()) {
            return ChatbotScopeDecision.allow();
        }

        if (this.containsAny(normalizedMessage, LEGAL_BINDING_PATTERNS)) {
            return ChatbotScopeDecision.reject(
                    ChatbotScopeViolationReason.LEGAL_BINDING_ADVICE_REQUESTED,
                    "No puedo emitir asesoramiento legal vinculante ni indicar una estrategia jurídica definitiva. Puedo ofrecer orientación general y ayudarte a revisar la información disponible en la plataforma.",
                    true
            );
        }

        if (TYPE_GENERAL.equals(conversation.getType())
                && this.containsAny(normalizedMessage, CONTEXT_REQUIRED_PATTERNS)) {
            return ChatbotScopeDecision.reject(
                    ChatbotScopeViolationReason.MISSING_CASE_CONTEXT,
                    "Esta conversación es general y no está asociada a un encargo concreto. Para responder sobre el estado, documentos o pasos de un caso, abre el asistente desde la hoja de encargo correspondiente.",
                    false
            );
        }

        if (TYPE_CONTEXTUAL.equals(conversation.getType())
                && this.containsAny(normalizedMessage, OTHER_CASE_PATTERNS)) {
            return ChatbotScopeDecision.reject(
                    ChatbotScopeViolationReason.OUT_OF_CASE_SCOPE,
                    "Solo puedo responder dentro del ámbito del encargo activo. Si necesitas consultar otro caso, abre una conversación desde la hoja de encargo correspondiente.",
                    false
            );
        }

        if (TYPE_CONTEXTUAL.equals(conversation.getType())
                && this.looksLikeUnsupportedFactualAssertion(normalizedMessage)) {
            return ChatbotScopeDecision.reject(
                    ChatbotScopeViolationReason.UNSUPPORTED_FACTUAL_ASSERTION,
                    "No debo afirmar hechos que no estén disponibles en el contexto actual. Puedo ayudarte con orientación general o con la información visible del encargo activo.",
                    false
            );
        }

        if (TYPE_GENERAL.equals(conversation.getType())
                && this.looksAmbiguous(normalizedMessage)) {
            return ChatbotScopeDecision.reject(
                    ChatbotScopeViolationReason.AMBIGUOUS_CONTEXT,
                    "Tu consulta necesita más contexto para responder con seguridad. Si se refiere a un encargo concreto, abre el asistente desde esa hoja de encargo.",
                    false
            );
        }

        return ChatbotScopeDecision.allow();
    }

    private boolean looksLikeUnsupportedFactualAssertion(String normalizedMessage) {
        return normalizedMessage.contains("confirma que")
                || normalizedMessage.contains("asegura que")
                || normalizedMessage.contains("asegúrame que")
                || normalizedMessage.contains("asegurame que")
                || normalizedMessage.contains("puedes garantizar")
                || normalizedMessage.contains("garantiza que");
    }

    private boolean looksAmbiguous(String normalizedMessage) {
        return normalizedMessage.contains("mi caso")
                || normalizedMessage.contains("mi encargo")
                || normalizedMessage.contains("mi expediente");
    }

    private boolean containsAny(String normalizedMessage, List<String> patterns) {
        return patterns.stream().anyMatch(normalizedMessage::contains);
    }

    private String normalize(String message) {
        if (message == null) {
            return "";
        }
        return message.trim().toLowerCase(Locale.ROOT);
    }
}

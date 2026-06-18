package es.upm.api.domain.services.policies;

import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.common.ChatbotResponseMessages;
import es.upm.api.domain.enums.ChatbotScopeViolationReason;
import es.upm.api.domain.enums.ConversationType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ChatbotScopePolicy {

    private static final Pattern ENGAGEMENT_REFERENCE_PATTERN = Pattern.compile(
            "\\b(?:EL-\\d+|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\\b",
            Pattern.CASE_INSENSITIVE
    );

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
            "proximos pasos de mi caso",
            "información del encargo",
            "informacion del encargo",
            "datos del encargo",
            "detalle del encargo",
            "detalles del encargo",
            "mis encargos",
            "mis casos",
            "mis expedientes",
            "qué encargos tengo",
            "que encargos tengo",
            "encargos tengo",
            "encargos abiertos",
            "encargos relacionados",
            "encargos pendientes",
            "encargos cerrados",
            "lista mis encargos",
            "listar mis encargos",
            "ver mis encargos",
            "compara mis encargos",
            "comparar mis encargos"
    );

    private static final List<String> EMOTIONAL_DISTRESS_PATTERNS = List.of(
            "me quiero morir",
            "quiero morirme",
            "no quiero vivir",
            "quiero desaparecer",
            "estoy deprimido",
            "estoy deprimida",
            "estoy hundido",
            "estoy hundida",
            "no puedo mas",
            "no puedo más",
            "no le encuentro sentido",
            "me siento fatal",
            "tengo ansiedad",
            "estoy en crisis",
            "quiero hacerme dano",
            "quiero hacerme daño",
            "hacerme dano",
            "hacerme daño",
            "suicid"
    );

    private static final List<String> OTHER_CASE_PATTERNS = List.of(
            "otro caso",
            "otro encargo",
            "mi otro caso",
            "mi otro encargo",
            "otro expediente",
            "además de este caso",
            "ademas de este caso",
            "comparar con otro encargo",
            "compara con otro encargo",
            "comparar este encargo con otro",
            "compara este encargo con otro",
            "comparar este encargo con el encargo",
            "compara este encargo con el encargo",
            "compararlo con otro encargo",
            "compararlo con otro caso",
            "consultar otro encargo",
            "consulta otro encargo",
            "ver otro encargo",
            "revisar otro encargo"
    );

    private static final List<String> OUT_OF_DOMAIN_PATTERNS = List.of(
            "te quiero",
            "nos vemos",
            "te amo",
            "me importas",
            "no te importo",
            "pum pum",
            "a tu cabeza",
            "idiota",
            "imbecil",
            "estupido",
            "gilipollas"
    );

    private static final List<String> DOMAIN_KEYWORDS = List.of(
            "goa",
            "encargo",
            "caso",
            "expediente",
            "estado",
            "hito",
            "evento",
            "timeline",
            "documento",
            "procedimiento",
            "tarea",
            "plataforma",
            "abogado",
            "legal"
    );

    public ChatbotScopeDecision evaluate(Conversation conversation, String message) {
        String normalizedMessage = this.normalize(message);

        if (normalizedMessage.isBlank()) {
            return ChatbotScopeDecision.allow();
        }

        if (this.containsAny(normalizedMessage, LEGAL_BINDING_PATTERNS)) {
            return ChatbotScopeDecision.reject(
                    ChatbotScopeViolationReason.LEGAL_BINDING_ADVICE_REQUESTED,
                    ChatbotResponseMessages.LEGAL_BINDING_ADVICE_REPLY,
                    true
            );
        }

        if (this.containsAny(normalizedMessage, EMOTIONAL_DISTRESS_PATTERNS)) {
            return ChatbotScopeDecision.reject(
                    ChatbotScopeViolationReason.EMOTIONAL_DISTRESS,
                    ChatbotResponseMessages.EMOTIONAL_DISTRESS_REPLY,
                    false
            );
        }

        if (ConversationType.GENERAL == conversation.getType()
                && (this.containsAny(normalizedMessage, CONTEXT_REQUIRED_PATTERNS)
                || this.referencesSpecificEngagement(normalizedMessage))) {
            return ChatbotScopeDecision.reject(
                    ChatbotScopeViolationReason.MISSING_CASE_CONTEXT,
                    ChatbotResponseMessages.MISSING_CASE_CONTEXT_REPLY,
                    false
            );
        }

        if (ConversationType.CONTEXTUAL == conversation.getType()
                && this.containsAny(normalizedMessage, OTHER_CASE_PATTERNS)) {
            return ChatbotScopeDecision.reject(
                    ChatbotScopeViolationReason.OUT_OF_CASE_SCOPE,
                    ChatbotResponseMessages.OUT_OF_CASE_SCOPE_REPLY,
                    false
            );
        }

        if (ConversationType.CONTEXTUAL == conversation.getType()
                && this.looksLikeUnsupportedFactualAssertion(normalizedMessage)) {
            return ChatbotScopeDecision.reject(
                    ChatbotScopeViolationReason.UNSUPPORTED_FACTUAL_ASSERTION,
                    ChatbotResponseMessages.UNSUPPORTED_FACTUAL_ASSERTION_REPLY,
                    false
            );
        }

        if (ConversationType.GENERAL == conversation.getType()
                && this.looksAmbiguous(normalizedMessage)) {
            return ChatbotScopeDecision.reject(
                    ChatbotScopeViolationReason.AMBIGUOUS_CONTEXT,
                    ChatbotResponseMessages.AMBIGUOUS_CONTEXT_REPLY,
                    false
            );
        }

        if (this.looksOutOfDomain(normalizedMessage)) {
            return ChatbotScopeDecision.reject(
                    ChatbotScopeViolationReason.OUT_OF_DOMAIN,
                    ChatbotResponseMessages.OUT_OF_DOMAIN_REPLY,
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

    private boolean looksOutOfDomain(String normalizedMessage) {
        return this.containsAny(normalizedMessage, OUT_OF_DOMAIN_PATTERNS)
                && !this.containsAny(normalizedMessage, DOMAIN_KEYWORDS);
    }

    private boolean containsAny(String normalizedMessage, List<String> patterns) {
        return patterns.stream().anyMatch(normalizedMessage::contains);
    }

    private boolean referencesSpecificEngagement(String normalizedMessage) {
        return ENGAGEMENT_REFERENCE_PATTERN.matcher(normalizedMessage).find();
    }

    private String normalize(String message) {
        if (message == null) {
            return "";
        }
        return message.trim().toLowerCase(Locale.ROOT);
    }
}

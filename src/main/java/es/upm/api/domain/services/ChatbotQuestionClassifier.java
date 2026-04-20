package es.upm.api.domain.services;

import es.upm.api.domain.enums.PlatformQuestionType;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class ChatbotQuestionClassifier {
    public PlatformQuestionType classify(String message) {
        String normalized = normalize(message);

        if (containsAny(normalized,
                "documento", "documentos", "archivo", "archivos", "adjunto", "adjuntos",
                "escrito", "escritos", "pdf", "demanda", "contrato")) {
            return PlatformQuestionType.DOCUMENTS;
        }

        if (containsAny(normalized,
                "hito", "hitos", "evento", "eventos", "timeline", "linea temporal",
                "línea temporal", "proximo paso", "próximo paso", "proximos pasos", "próximos pasos",
                "siguiente paso", "siguientes pasos", "fecha", "fechas", "plazo", "plazos")) {
            return PlatformQuestionType.TIMELINE_EVENTS;
        }

        if (containsAny(normalized,
                "estado", "encargo", "caso", "procedimiento", "procedimientos",
                "resumen", "contexto", "situacion", "situación")) {
            return PlatformQuestionType.ENGAGEMENT_STATUS;
        }

        return PlatformQuestionType.GENERAL_CONTEXT;
    }

    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}

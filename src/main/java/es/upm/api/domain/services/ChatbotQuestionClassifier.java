package es.upm.api.domain.services;

import es.upm.api.domain.enums.PlatformQuestionType;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class ChatbotQuestionClassifier {
    public PlatformQuestionType classify(String message) {
        String normalized = normalize(message);

        if (containsAny(normalized,
                "documento", "documentos", "archivo", "archivos", "adjunto", "adjuntos",
                "escrito", "escritos", "pdf", "demanda", "contrato", "contratos",
                "evidencia", "evidencias")) {
            return PlatformQuestionType.DOCUMENTS;
        }

        if (containsAny(normalized,
                "hito", "hitos", "evento", "eventos", "timeline", "linea temporal",
                "proximo paso", "proximos pasos", "siguiente paso", "siguientes pasos",
                "fecha", "fechas", "plazo", "plazos", "cuando es", "cuando vence",
                "que sigue", "como sigue", "que toca", "que viene")) {
            return PlatformQuestionType.TIMELINE_EVENTS;
        }

        if (containsAny(normalized,
                "estado", "encargo", "caso", "procedimiento", "procedimientos",
                "resumen", "contexto", "situacion", "como va", "en que esta",
                "en que estado", "avance")) {
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
        if (value == null) {
            return "";
        }
        String lowered = value.toLowerCase(Locale.ROOT).trim();
        return Normalizer.normalize(lowered, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}

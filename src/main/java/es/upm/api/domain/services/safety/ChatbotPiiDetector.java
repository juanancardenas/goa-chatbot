package es.upm.api.domain.services.safety;

import es.upm.api.domain.model.safety.ChatbotPiiDetectionResult;

import java.util.regex.Pattern;

public class ChatbotPiiDetector {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
    );

    private static final Pattern SPANISH_PHONE_PATTERN = Pattern.compile(
            "(?<!\\d)(?:\\+34[\\s-]?)?[679](?:[\\s-]?\\d){8}(?!\\d)"
    );

    private static final Pattern DNI_NIE_PATTERN = Pattern.compile(
            "\\b(?:[XYZ][\\s-]?\\d{7}[\\s-]?[A-Z]|\\d{8}[\\s-]?[A-Z])\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CARD_PATTERN = Pattern.compile(
            "(?<!\\d)(?:\\d[\\s-]*?){13,19}(?!\\d)"
    );

    private static final Pattern SPANISH_IBAN_PATTERN = Pattern.compile(
            "\\bES\\d{2}(?:[\\s-]?\\d{4}){5}\\b",
            Pattern.CASE_INSENSITIVE
    );

    public ChatbotPiiDetectionResult detect(String message) {
        if (message == null || message.isBlank()) {
            return ChatbotPiiDetectionResult.empty();
        }

        return ChatbotPiiDetectionResult.of(
                this.containsEmail(message),
                this.containsPhone(message),
                this.containsDniNie(message),
                this.containsCard(message),
                this.containsIban(message)
        );
    }

    private boolean containsEmail(String message) {
        return EMAIL_PATTERN.matcher(message).find();
    }

    private boolean containsPhone(String message) {
        return SPANISH_PHONE_PATTERN.matcher(message).find();
    }

    private boolean containsDniNie(String message) {
        return DNI_NIE_PATTERN.matcher(message).find();
    }

    private boolean containsCard(String message) {
        return CARD_PATTERN.matcher(message).find();
    }

    private boolean containsIban(String message) {
        return SPANISH_IBAN_PATTERN.matcher(message).find();
    }
}

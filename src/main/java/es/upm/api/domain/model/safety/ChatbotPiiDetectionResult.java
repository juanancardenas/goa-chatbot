package es.upm.api.domain.model.safety;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
public class ChatbotPiiDetectionResult {
    private final boolean containsEmail;
    private final boolean containsPhone;
    private final boolean containsDniNie;
    private final boolean containsPassport;
    private final boolean containsCard;
    private final boolean containsIban;
    private final Set<ChatbotModerationReason> reasons;

    private ChatbotPiiDetectionResult(
            boolean containsEmail,
            boolean containsPhone,
            boolean containsDniNie,
            boolean containsPassport,
            boolean containsCard,
            boolean containsIban
    ) {
        this.containsEmail = containsEmail;
        this.containsPhone = containsPhone;
        this.containsDniNie = containsDniNie;
        this.containsPassport = containsPassport;
        this.containsCard = containsCard;
        this.containsIban = containsIban;
        this.reasons = this.buildReasons();
    }

    public static ChatbotPiiDetectionResult empty() {
        return new ChatbotPiiDetectionResult(
                false,
                false,
                false,
                false,
                false,
                false
        );
    }

    public static ChatbotPiiDetectionResult of(
            boolean containsEmail,
            boolean containsPhone,
            boolean containsDniNie,
            boolean containsPassport,
            boolean containsCard,
            boolean containsIban
    ) {
        return new ChatbotPiiDetectionResult(
                containsEmail,
                containsPhone,
                containsDniNie,
                containsPassport,
                containsCard,
                containsIban
        );
    }

    public boolean containsPii() {
        return this.containsEmail
                || this.containsPhone
                || this.containsDniNie
                || this.containsPassport
                || this.containsCard
                || this.containsIban;
    }

    public Set<ChatbotModerationReason> getReasons() {
        return Collections.unmodifiableSet(this.reasons);
    }

    private Set<ChatbotModerationReason> buildReasons() {
        Set<ChatbotModerationReason> detectedReasons = new LinkedHashSet<>();

        if (this.containsEmail) {
            detectedReasons.add(ChatbotModerationReason.PII_EMAIL);
        }
        if (this.containsPhone) {
            detectedReasons.add(ChatbotModerationReason.PII_PHONE);
        }
        if (this.containsDniNie) {
            detectedReasons.add(ChatbotModerationReason.PII_DNI_NIE);
        }
        if (this.containsPassport) {
            detectedReasons.add(ChatbotModerationReason.PII_PASSPORT);
        }
        if (this.containsCard) {
            detectedReasons.add(ChatbotModerationReason.PII_CARD);
        }
        if (this.containsIban) {
            detectedReasons.add(ChatbotModerationReason.PII_IBAN);
        }

        return detectedReasons;
    }
}

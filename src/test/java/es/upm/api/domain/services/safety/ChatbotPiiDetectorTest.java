package es.upm.api.domain.services.safety;

import es.upm.api.domain.model.safety.ChatbotModerationReason;
import es.upm.api.domain.model.safety.ChatbotPiiDetectionResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotPiiDetectorTest {
    private final ChatbotPiiDetector detector = new ChatbotPiiDetector();

    @Test
    void detectShouldReturnEmptyResultWhenMessageIsNull() {
        ChatbotPiiDetectionResult result = this.detector.detect(null);

        assertThat(result.containsPii()).isFalse();
        assertThat(result.getReasons()).isEmpty();
    }

    @Test
    void detectShouldReturnEmptyResultWhenMessageIsEmpty() {
        ChatbotPiiDetectionResult result = this.detector.detect("");

        assertThat(result.containsPii()).isFalse();
        assertThat(result.getReasons()).isEmpty();
    }

    @Test
    void detectShouldReturnEmptyResultWhenMessageIsBlank() {
        ChatbotPiiDetectionResult result = this.detector.detect("   ");

        assertThat(result.containsPii()).isFalse();
        assertThat(result.getReasons()).isEmpty();
    }

    @Test
    void detectShouldFindSimpleEmail() {
        ChatbotPiiDetectionResult result = this.detector.detect("Mi email es usuario@example.com");

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsEmail()).isTrue();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_EMAIL);
    }

    @Test
    void detectShouldFindEmailWithAlias() {
        ChatbotPiiDetectionResult result = this.detector.detect("Contacta con test+alias@gmail.com");

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsEmail()).isTrue();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_EMAIL);
    }

    @Test
    void detectShouldNotFindIncompleteEmail() {
        ChatbotPiiDetectionResult result = this.detector.detect("Mi usuario es usuario@");

        assertThat(result.containsPii()).isFalse();
        assertThat(result.isContainsEmail()).isFalse();
    }

    @Test
    void detectShouldFindSpanishMobilePhone() {
        ChatbotPiiDetectionResult result = this.detector.detect("Mi móvil es 612345678");

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsPhone()).isTrue();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_PHONE);
    }

    @Test
    void detectShouldFindSpanishLandlinePhone() {
        ChatbotPiiDetectionResult result = this.detector.detect("Mi fijo es 912345678");

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsPhone()).isTrue();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_PHONE);
    }

    @Test
    void detectShouldFindSpanishPhoneWithPrefixAndSpaces() {
        ChatbotPiiDetectionResult result = this.detector.detect("Mi teléfono es +34 612 345 678");

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsPhone()).isTrue();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_PHONE);
    }

    @Test
    void detectShouldFindSpanishPhoneWithPrefixAndHyphens() {
        ChatbotPiiDetectionResult result = this.detector.detect("Mi teléfono es +34-912-345-678");

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsPhone()).isTrue();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_PHONE);
    }

    @Test
    void detectShouldNotFindShortPhone() {
        ChatbotPiiDetectionResult result = this.detector.detect("El código es 123");

        assertThat(result.containsPii()).isFalse();
        assertThat(result.isContainsPhone()).isFalse();
    }

    @Test
    void detectShouldFindDni() {
        ChatbotPiiDetectionResult result = this.detector.detect("Mi DNI es 12345678Z");

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsDniNie()).isTrue();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_DNI_NIE);
    }

    @Test
    void detectShouldFindDniWithHyphen() {
        ChatbotPiiDetectionResult result = this.detector.detect("Mi DNI es 12345678-Z");

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsDniNie()).isTrue();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_DNI_NIE);
    }

    @Test
    void detectShouldFindNieStartingWithX() {
        ChatbotPiiDetectionResult result = this.detector.detect("Mi NIE es X1234567L");

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsDniNie()).isTrue();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_DNI_NIE);
    }

    @Test
    void detectShouldFindNieStartingWithY() {
        ChatbotPiiDetectionResult result = this.detector.detect("Mi NIE es Y1234567X");

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsDniNie()).isTrue();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_DNI_NIE);
    }

    @Test
    void detectShouldFindNieStartingWithZ() {
        ChatbotPiiDetectionResult result = this.detector.detect("Mi NIE es Z1234567R");

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsDniNie()).isTrue();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_DNI_NIE);
    }

    @Test
    void detectShouldNotFindIncompleteDni() {
        ChatbotPiiDetectionResult result = this.detector.detect("Mi documento es 1234567");

        assertThat(result.containsPii()).isFalse();
        assertThat(result.isContainsDniNie()).isFalse();
    }

    @Test
    void detectShouldFindCardWithoutSeparators() {
        ChatbotPiiDetectionResult result = this.detector.detect("La tarjeta es 4111111111111111");

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsCard()).isTrue();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_CARD);
    }

    @Test
    void detectShouldFindCardWithSpaces() {
        ChatbotPiiDetectionResult result = this.detector.detect("La tarjeta es 4111 1111 1111 1111");

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsCard()).isTrue();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_CARD);
    }

    @Test
    void detectShouldFindCardWithHyphens() {
        ChatbotPiiDetectionResult result = this.detector.detect("La tarjeta es 5500-0000-0000-0004");

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsCard()).isTrue();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_CARD);
    }

    @Test
    void detectShouldNotFindShortCardNumber() {
        ChatbotPiiDetectionResult result = this.detector.detect("El número es 1234");

        assertThat(result.containsPii()).isFalse();
        assertThat(result.isContainsCard()).isFalse();
    }

    @Test
    void detectShouldFindSpanishIbanWithoutSpaces() {
        ChatbotPiiDetectionResult result = this.detector.detect("Mi IBAN es ES9121000418450200051332");

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsIban()).isTrue();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_IBAN);
    }

    @Test
    void detectShouldFindSpanishIbanWithSpaces() {
        ChatbotPiiDetectionResult result = this.detector.detect("Mi IBAN es ES91 2100 0418 4502 0005 1332");

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsIban()).isTrue();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_IBAN);
    }

    @Test
    void detectShouldNotFindIncompleteIban() {
        ChatbotPiiDetectionResult result = this.detector.detect("Mi IBAN es ES91");

        assertThat(result.containsPii()).isFalse();
        assertThat(result.isContainsIban()).isFalse();
    }

    @Test
    void detectShouldNotFindForeignIbanWhenOnlySpanishIbanIsSupported() {
        ChatbotPiiDetectionResult result = this.detector.detect("Mi IBAN francés es FR7630006000011234567890189");

        assertThat(result.containsPii()).isFalse();
        assertThat(result.isContainsIban()).isFalse();
    }

    @Test
    void detectShouldFindSeveralPiiTypesInSameMessage() {
        ChatbotPiiDetectionResult result = this.detector.detect(
                "Mi email es usuario@example.com, mi teléfono es +34 612 345 678 "
                        + "y mi DNI es 12345678Z"
        );

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsEmail()).isTrue();
        assertThat(result.isContainsPhone()).isTrue();
        assertThat(result.isContainsDniNie()).isTrue();
        assertThat(result.getReasons()).contains(
                ChatbotModerationReason.PII_EMAIL,
                ChatbotModerationReason.PII_PHONE,
                ChatbotModerationReason.PII_DNI_NIE
        );
    }

    @Test
    void detectShouldNotStoreOriginalMessageOrDetectedValues() {
        String message = "Mi tarjeta es 4111 1111 1111 1111";

        ChatbotPiiDetectionResult result = this.detector.detect(message);

        assertThat(result.toString()).doesNotContain(message);
        assertThat(result.toString()).doesNotContain("4111 1111 1111 1111");
    }

    @Test
    void detectShouldNotClassifySpanishIbanAsCard() {
        ChatbotPiiDetectionResult result = this.detector.detect(
                "Mi IBAN es ES91 2100 0418 4502 0005 1332"
        );

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsIban()).isTrue();
        assertThat(result.isContainsCard()).isFalse();
        assertThat(result.getReasons()).contains(ChatbotModerationReason.PII_IBAN);
        assertThat(result.getReasons()).doesNotContain(ChatbotModerationReason.PII_CARD);
    }

    @Test
    void detectShouldStillFindCardWhenMessageContainsIbanAndCard() {
        ChatbotPiiDetectionResult result = this.detector.detect(
                "Mi IBAN es ES91 2100 0418 4502 0005 1332 y mi tarjeta es 4111 1111 1111 1111"
        );

        assertThat(result.containsPii()).isTrue();
        assertThat(result.isContainsIban()).isTrue();
        assertThat(result.isContainsCard()).isTrue();
        assertThat(result.getReasons()).contains(
                ChatbotModerationReason.PII_IBAN,
                ChatbotModerationReason.PII_CARD
        );
    }
}

package es.upm.api.domain.model.safety;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotPiiDetectionResultTest {
    @Test
    void emptyShouldCreateResultWithoutPii() {
        ChatbotPiiDetectionResult result = ChatbotPiiDetectionResult.empty();

        assertThat(result.containsPii()).isFalse();
        assertThat(result.isContainsEmail()).isFalse();
        assertThat(result.isContainsPhone()).isFalse();
        assertThat(result.isContainsDniNie()).isFalse();
        assertThat(result.isContainsPassport()).isFalse();
        assertThat(result.isContainsCard()).isFalse();
        assertThat(result.isContainsIban()).isFalse();
        assertThat(result.getReasons()).isEmpty();
    }

    @Test
    void ofShouldCreateResultWithEmailReason() {
        ChatbotPiiDetectionResult result = ChatbotPiiDetectionResult.of(
                true,
                false,
                false,
                false,
                false,
                false
        );

        assertThat(result.containsPii()).isTrue();
        assertThat(result.getReasons()).containsExactly(ChatbotModerationReason.PII_EMAIL);
    }

    @Test
    void ofShouldCreateResultWithPhoneReason() {
        ChatbotPiiDetectionResult result = ChatbotPiiDetectionResult.of(
                false,
                true,
                false,
                false,
                false,
                false
        );

        assertThat(result.containsPii()).isTrue();
        assertThat(result.getReasons()).containsExactly(ChatbotModerationReason.PII_PHONE);
    }

    @Test
    void ofShouldCreateResultWithDniNieReason() {
        ChatbotPiiDetectionResult result = ChatbotPiiDetectionResult.of(
                false,
                false,
                true,
                false,
                false,
                false
        );

        assertThat(result.containsPii()).isTrue();
        assertThat(result.getReasons()).containsExactly(ChatbotModerationReason.PII_DNI_NIE);
    }

    @Test
    void ofShouldCreateResultWithPassportReason() {
        ChatbotPiiDetectionResult result = ChatbotPiiDetectionResult.of(
                false,
                false,
                false,
                true,
                false,
                false
        );

        assertThat(result.containsPii()).isTrue();
        assertThat(result.getReasons()).containsExactly(ChatbotModerationReason.PII_PASSPORT);
    }

    @Test
    void ofShouldCreateResultWithCardReason() {
        ChatbotPiiDetectionResult result = ChatbotPiiDetectionResult.of(
                false,
                false,
                false,
                false,
                true,
                false
        );

        assertThat(result.containsPii()).isTrue();
        assertThat(result.getReasons()).containsExactly(ChatbotModerationReason.PII_CARD);
    }

    @Test
    void ofShouldCreateResultWithIbanReason() {
        ChatbotPiiDetectionResult result = ChatbotPiiDetectionResult.of(
                false,
                false,
                false,
                false,
                false,
                true
        );

        assertThat(result.containsPii()).isTrue();
        assertThat(result.getReasons()).containsExactly(ChatbotModerationReason.PII_IBAN);
    }

    @Test
    void ofShouldCreateResultWithSeveralReasons() {
        ChatbotPiiDetectionResult result = ChatbotPiiDetectionResult.of(
                true,
                true,
                true,
                true,
                true,
                true
        );

        assertThat(result.containsPii()).isTrue();
        assertThat(result.getReasons()).containsExactly(
                ChatbotModerationReason.PII_EMAIL,
                ChatbotModerationReason.PII_PHONE,
                ChatbotModerationReason.PII_DNI_NIE,
                ChatbotModerationReason.PII_PASSPORT,
                ChatbotModerationReason.PII_CARD,
                ChatbotModerationReason.PII_IBAN
        );
    }

    @Test
    void reasonsShouldBeImmutable() {
        ChatbotPiiDetectionResult result = ChatbotPiiDetectionResult.of(
                true,
                false,
                false,
                false,
                false,
                false
        );

        assertThat(result.getReasons())
                .containsExactly(ChatbotModerationReason.PII_EMAIL);
    }
}

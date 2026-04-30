package es.upm.api.domain.model.validations;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PositiveBigDecimalValidatorTest {

    private final PositiveBigDecimalValidator validator = new PositiveBigDecimalValidator();

    @Test
    void initializeShouldAcceptConstraintWithoutSideEffects() {
        this.validator.initialize(null);

        assertThat(this.validator.isValid(BigDecimal.ONE, null)).isTrue();
    }

    @Test
    void isValidShouldReturnFalseForNullValue() {
        assertThat(this.validator.isValid(null, null)).isFalse();
    }

    @Test
    void isValidShouldReturnFalseForNegativeValue() {
        assertThat(this.validator.isValid(new BigDecimal("-0.01"), null)).isFalse();
    }

    @Test
    void isValidShouldReturnTrueForZeroValue() {
        assertThat(this.validator.isValid(BigDecimal.ZERO, null)).isTrue();
    }

    @Test
    void isValidShouldReturnTrueForPositiveValue() {
        assertThat(this.validator.isValid(new BigDecimal("12.50"), null)).isTrue();
    }
}

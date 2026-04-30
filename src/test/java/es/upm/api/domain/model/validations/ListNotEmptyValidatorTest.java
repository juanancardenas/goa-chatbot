package es.upm.api.domain.model.validations;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListNotEmptyValidatorTest {

    private final ListNotEmptyValidator validator = new ListNotEmptyValidator();

    @Test
    void initializeShouldAcceptConstraintWithoutSideEffects() {
        this.validator.initialize(null);

        assertThat(this.validator.isValid(List.of("value"), null)).isTrue();
    }

    @Test
    void isValidShouldReturnFalseForNullList() {
        assertThat(this.validator.isValid(null, null)).isFalse();
    }

    @Test
    void isValidShouldReturnFalseForEmptyList() {
        assertThat(this.validator.isValid(List.of(), null)).isFalse();
    }

    @Test
    void isValidShouldReturnTrueForNonEmptyList() {
        assertThat(this.validator.isValid(List.of("value"), null)).isTrue();
    }
}

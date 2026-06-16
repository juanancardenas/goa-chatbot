package es.upm.api.adapter.in.rest.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorMessageTest {

    @Test
    void constructorShouldExtractExceptionTypeMessageAndCode() {
        IllegalArgumentException exception = new IllegalArgumentException("invalid value");

        ErrorMessage errorMessage = new ErrorMessage(exception, 400);

        assertThat(errorMessage)
                .satisfies(message -> {
                    assertThat(message.getError()).isEqualTo("IllegalArgumentException");
                    assertThat(message.getMessage()).isEqualTo("invalid value");
                    assertThat(message.getCode()).isEqualTo(400);
                });
    }

    @Test
    void toStringShouldIncludeAllFields() {
        ErrorMessage errorMessage = new ErrorMessage(new IllegalStateException("boom"), 500);

        assertThat(errorMessage).hasToString(
                "ErrorMessage{error='IllegalStateException', message='boom', code=500}"
        );
    }
}

package es.upm.api.infrastructure.resources.httperrors;

import es.upm.api.adapter.in.rest.error.ErrorMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorMessageTest {

    @Test
    void constructorShouldExtractExceptionTypeMessageAndCode() {
        IllegalArgumentException exception = new IllegalArgumentException("invalid value");

        ErrorMessage errorMessage = new ErrorMessage(exception, 400);

        assertThat(errorMessage.getError()).isEqualTo("IllegalArgumentException");
        assertThat(errorMessage.getMessage()).isEqualTo("invalid value");
        assertThat(errorMessage.getCode()).isEqualTo(400);
    }

    @Test
    void toStringShouldIncludeAllFields() {
        ErrorMessage errorMessage = new ErrorMessage(new IllegalStateException("boom"), 500);

        assertThat(errorMessage.toString()).isEqualTo(
                "ErrorMessage{error='IllegalStateException', message='boom', code=500}"
        );
    }
}

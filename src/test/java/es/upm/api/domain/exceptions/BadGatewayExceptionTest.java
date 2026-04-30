package es.upm.api.domain.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BadGatewayExceptionTest {

    @Test
    void constructorShouldPrefixDescriptionToDetail() {
        BadGatewayException exception = new BadGatewayException("remote service unavailable");

        assertThat(exception.getMessage()).isEqualTo("Bad Gateway Exception. remote service unavailable");
    }
}

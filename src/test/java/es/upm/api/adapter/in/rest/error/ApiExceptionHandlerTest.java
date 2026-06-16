package es.upm.api.adapter.in.rest.error;

import es.upm.api.domain.exceptions.BadGatewayException;
import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.exceptions.ConflictException;
import es.upm.api.domain.exceptions.ForbiddenException;
import es.upm.api.domain.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {

    @Test
    void unauthorizedRequestShouldAcceptExceptionWithoutThrowing() {
        ApiExceptionHandler handler = new ApiExceptionHandler(Mockito.mock(Environment.class));

        assertThatCode(() -> handler.unauthorizedRequest(
                new org.springframework.security.access.AccessDeniedException("denied")
        )).doesNotThrowAnyException();
    }

    @Test
    void noResourceFoundRequestShouldReturnStandardNotFoundMessage() {
        ApiExceptionHandler handler = new ApiExceptionHandler(Mockito.mock(Environment.class));

        ErrorMessage errorMessage = handler.noResourceFoundRequest();

        assertThat(errorMessage.getError()).isEqualTo("NotFoundException");
        assertThat(errorMessage.getCode()).isEqualTo(404);
        assertThat(errorMessage.getMessage()).contains("Path no encontrado");
    }

    @Test
    void notFoundRequestShouldWrapOriginalException() {
        ApiExceptionHandler handler = new ApiExceptionHandler(Mockito.mock(Environment.class));

        ErrorMessage errorMessage = handler.notFoundRequest(new NotFoundException("missing resource"));

        assertThat(errorMessage.getError()).isEqualTo("NotFoundException");
        assertThat(errorMessage.getCode()).isEqualTo(404);
        assertThat(errorMessage.getMessage()).contains("missing resource");
    }

    @Test
    void badRequestShouldWrapOriginalException() {
        ApiExceptionHandler handler = new ApiExceptionHandler(Mockito.mock(Environment.class));

        ErrorMessage errorMessage = handler.badRequest(new BadRequestException("bad input"));

        assertThat(errorMessage.getError()).isEqualTo("BadRequestException");
        assertThat(errorMessage.getCode()).isEqualTo(400);
        assertThat(errorMessage.getMessage()).contains("bad input");
    }

    @Test
    void invalidArgumentsShouldAggregateDistinctMessagesAndFallbackFieldName() {
        Environment environment = Mockito.mock(Environment.class);
        ApiExceptionHandler handler = new ApiExceptionHandler(environment);
        MethodArgumentNotValidException exception = Mockito.mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "amount", null, false, null, null, "debe ser positivo"),
                new FieldError("request", "amount", null, false, null, null, "debe ser positivo"),
                new FieldError("request", "name", null, false, null, null, null)
        ));

        ErrorMessage errorMessage = handler.invalidArguments(exception);

        assertThat(errorMessage.getError()).isEqualTo("BadRequestException");
        assertThat(errorMessage.getCode()).isEqualTo(400);
        assertThat(errorMessage.getMessage()).contains("debe ser positivo");
        assertThat(errorMessage.getMessage()).contains("name no es valido");
        assertThat(errorMessage.getMessage()).doesNotContain("debe ser positivo; debe ser positivo");
    }

    @Test
    void conflictShouldWrapOriginalException() {
        ApiExceptionHandler handler = new ApiExceptionHandler(Mockito.mock(Environment.class));

        ErrorMessage errorMessage = handler.conflict(new ConflictException("already exists"));

        assertThat(errorMessage.getError()).isEqualTo("ConflictException");
        assertThat(errorMessage.getCode()).isEqualTo(409);
    }

    @Test
    void forbiddenShouldWrapOriginalException() {
        ApiExceptionHandler handler = new ApiExceptionHandler(Mockito.mock(Environment.class));

        ErrorMessage errorMessage = handler.forbidden(new ForbiddenException("not allowed"));

        assertThat(errorMessage.getError()).isEqualTo("ForbiddenException");
        assertThat(errorMessage.getCode()).isEqualTo(403);
    }

    @Test
    void badGatewayShouldWrapOriginalException() {
        ApiExceptionHandler handler = new ApiExceptionHandler(Mockito.mock(Environment.class));

        ErrorMessage errorMessage = handler.badGateway(new BadGatewayException("upstream failed"));

        assertThat(errorMessage.getError()).isEqualTo("BadGatewayException");
        assertThat(errorMessage.getCode()).isEqualTo(502);
        assertThat(errorMessage.getMessage()).contains("upstream failed");
    }

    @Test
    void exceptionShouldCheckDebugProfilesAndReturnInternalServerError() {
        Environment environment = Mockito.mock(Environment.class);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
        ApiExceptionHandler handler = new ApiExceptionHandler(environment);
        Exception exception = new Exception("boom");

        ErrorMessage errorMessage = handler.exception(exception);

        verify(environment).acceptsProfiles(any(Profiles.class));
        assertThat(errorMessage.getError()).isEqualTo(exception.getClass().getSimpleName());
        assertThat(errorMessage.getCode()).isEqualTo(500);
        assertThat(errorMessage.getMessage()).isEqualTo("boom");
    }

    @Test
    void exceptionShouldReturnInternalServerErrorOutsideDevOrTest() {
        Environment environment = Mockito.mock(Environment.class);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        ApiExceptionHandler handler = new ApiExceptionHandler(environment);
        Exception exception = new Exception("boom");

        ErrorMessage errorMessage = handler.exception(exception);

        assertThat(errorMessage.getCode()).isEqualTo(500);
        assertThat(errorMessage.getMessage()).isEqualTo("boom");
    }
}

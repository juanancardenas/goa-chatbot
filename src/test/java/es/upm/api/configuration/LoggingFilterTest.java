package es.upm.api.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoggingFilterTest {

    private final LoggingFilter loggingFilter = new LoggingFilter();

    @Test
    void doFilterInternalShouldCopyWrappedResponseBodyToOriginalResponse() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/chatbot/messages");
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContentType("application/json");
        request.addHeader("X-Trace-Id", "trace-1");
        request.setParameter("type", "GENERAL");
        request.setContent("{\"message\":\"hola\"}".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        AtomicBoolean chainExecuted = new AtomicBoolean(false);
        FilterChain filterChain = (wrappedRequest, wrappedResponse) -> {
            chainExecuted.set(true);
            wrappedRequest.getInputStream().readAllBytes();
            wrappedResponse.setContentType("application/json");
            wrappedResponse.getWriter().write("{\"status\":\"ok\"}");
        };

        this.loggingFilter.doFilterInternal(request, response, filterChain);

        assertThat(chainExecuted).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEqualTo("{\"status\":\"ok\"}");
        assertThat(response.getContentType()).startsWith("application/json");
    }

    @Test
    void doFilterInternalShouldPropagateFilterChainExceptions() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/chatbot/conversations");
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ServletException exception = assertThrows(
                ServletException.class,
                () -> this.loggingFilter.doFilterInternal(
                        request,
                        response,
                        (wrappedRequest, wrappedResponse) -> {
                            throw new ServletException("chain failed");
                        }
                )
        );

        assertThat(exception).hasMessageContaining("chain failed");
        assertThat(response.getContentAsByteArray()).isEmpty();
    }
}

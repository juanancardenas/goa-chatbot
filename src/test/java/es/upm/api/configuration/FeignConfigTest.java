package es.upm.api.configuration;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeignConfigTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T10:00:00Z");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestInterceptorShouldForwardJwtTokenFromSecurityContext() {
        TokenManager tokenManager = Mockito.mock(TokenManager.class);
        FeignConfig feignConfig = new FeignConfig(tokenManager);
        RequestInterceptor requestInterceptor = feignConfig.requestInterceptor();
        RequestTemplate requestTemplate = new RequestTemplate();

        Jwt jwt = new Jwt(
                "jwt-token",
                FIXED_INSTANT,
                FIXED_INSTANT.plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("sub", "user-1")
        );
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        requestInterceptor.apply(requestTemplate);

        assertThat(requestTemplate.headers().get("Authorization")).containsExactly("Bearer jwt-token");
        verify(tokenManager, never()).getToken();
    }

    @Test
    void requestInterceptorShouldUseServiceTokenWhenAuthenticationIsNotJwt() {
        TokenManager tokenManager = Mockito.mock(TokenManager.class);
        when(tokenManager.getToken()).thenReturn("service-token");

        FeignConfig feignConfig = new FeignConfig(tokenManager);
        RequestInterceptor requestInterceptor = feignConfig.requestInterceptor();
        RequestTemplate requestTemplate = new RequestTemplate();

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user-1", "password", "ROLE_CUSTOMER")
        );

        requestInterceptor.apply(requestTemplate);

        assertThat(requestTemplate.headers().get("Authorization")).containsExactly("Bearer service-token");
        verify(tokenManager).getToken();
    }
}

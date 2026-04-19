package es.upm.api.configurations;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
public class FeignConfig {
    private final TokenManager tokenManager;

    public FeignConfig(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                template.header("Authorization", "Bearer " + jwtAuthenticationToken.getToken().getTokenValue());
            } else {
                template.header("Authorization", "Bearer " + tokenManager.getToken());
            }
        };
    }
}

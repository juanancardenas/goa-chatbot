package es.upm.api.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceServerConfigTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void jwtAuthenticationConverterShouldUseRolesClaimWithRolePrefix() {
        ResourceServerConfig config = new ResourceServerConfig();
        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim(ResourceServerConfig.CLAIM_NAME, List.of("admin", "customer"))
                .subject("user-1")
                .issuedAt(FIXED_INSTANT)
                .expiresAt(FIXED_INSTANT.plusSeconds(300))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt).getAuthorities();

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_admin", "ROLE_customer");
    }

    @Test
    void jwtAuthenticationConverterShouldFallbackToAwsGroupsWhenRolesClaimIsMissing() {
        ResourceServerConfig config = new ResourceServerConfig();
        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim(ResourceServerConfig.AWS_CLAIM_NAME, List.of("manager", "operator"))
                .subject("user-2")
                .issuedAt(FIXED_INSTANT)
                .expiresAt(FIXED_INSTANT.plusSeconds(300))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt).getAuthorities();

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_manager", "ROLE_operator");
    }

    @Test
    void jwtAuthenticationConverterShouldReturnNoAuthoritiesWhenRolesAndAwsGroupsAreMissing() {
        ResourceServerConfig config = new ResourceServerConfig();
        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-3")
                .issuedAt(FIXED_INSTANT)
                .expiresAt(FIXED_INSTANT.plusSeconds(300))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt).getAuthorities();

        assertThat(authorities).isEmpty();
    }

    @Test
    void corsConfigurationSourceShouldTrimAndRegisterAllowedOrigins() {
        ResourceServerConfig config = new ResourceServerConfig();
        ReflectionTestUtils.setField(
                config,
                "allowedOrigins",
                " http://localhost:3000 ,https://goa.test,   , http://127.0.0.1:5173 "
        );

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration corsConfiguration = source.getCorsConfiguration(
                new MockHttpServletRequest("GET", "/chatbot/messages")
        );

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.getAllowedOrigins())
                .containsExactly("http://localhost:3000", "https://goa.test", "http://127.0.0.1:5173");
        assertThat(corsConfiguration.getAllowedMethods())
                .containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(corsConfiguration.getAllowedHeaders()).containsExactly("*");
        assertThat(corsConfiguration.getAllowCredentials()).isTrue();
    }

    @Test
    void jwtDecoderShouldCreateNimbusDecoderForConfiguredJwkUri() {
        ResourceServerConfig config = new ResourceServerConfig();

        JwtDecoder jwtDecoder = config.jwtDecoder("https://auth.goa.test/.well-known/jwks.json");

        assertThat(jwtDecoder).isNotNull();
        assertThat(jwtDecoder.getClass().getName()).contains("NimbusJwtDecoder");
    }
}

package es.upm.api.configuration;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class TokenManagerTest {

    @Test
    void getTokenShouldObtainAccessTokenWhenCacheIsEmpty() {
        TokenManager tokenManager = new TokenManager("client-id", "client-secret", "https://issuer/token");

        try (MockedConstruction<RestTemplate> ignored = mockConstruction(RestTemplate.class, (mock, context) ->
                when(mock.postForEntity(eq("https://issuer/token"), any(HttpEntity.class), eq(Map.class)))
                        .thenReturn(ResponseEntity.ok(Map.of(
                                "access_token", "token-1",
                                "expires_in", 3600
                        )))
        )) {
            String token = tokenManager.getToken();

            assertThat(token).isEqualTo("token-1");
            assertThat(this.readField(tokenManager, "token")).isEqualTo("token-1");
            assertThat((Instant) this.readField(tokenManager, "expiry")).isAfter(Instant.now().plusSeconds(3000));
        }
    }

    @Test
    void getTokenShouldReuseCachedTokenWhenItIsNotNearExpiry() {
        TokenManager tokenManager = new TokenManager("client-id", "client-secret", "https://issuer/token");
        this.writeField(tokenManager, "token", "cached-token");
        this.writeField(tokenManager, "expiry", Instant.now().plusSeconds(600));

        try (MockedConstruction<RestTemplate> ignored = mockConstruction(RestTemplate.class)) {
            String token = tokenManager.getToken();

            assertThat(token).isEqualTo("cached-token");
            assertThat(ignored.constructed()).isEmpty();
        }
    }

    @Test
    void getTokenShouldRefreshTokenWhenExpiryIsWithinOneMinute() {
        TokenManager tokenManager = new TokenManager("client-id", "client-secret", "https://issuer/token");
        this.writeField(tokenManager, "token", "stale-token");
        this.writeField(tokenManager, "expiry", Instant.now().plusSeconds(30));

        try (MockedConstruction<RestTemplate> ignored = mockConstruction(RestTemplate.class, (mock, context) ->
                when(mock.postForEntity(eq("https://issuer/token"), any(HttpEntity.class), eq(Map.class)))
                        .thenReturn(new ResponseEntity<>(Map.of(
                                "access_token", "fresh-token",
                                "expires_in", 1800
                        ), HttpStatus.OK))
        )) {
            String token = tokenManager.getToken();

            assertThat(token).isEqualTo("fresh-token");
            assertThat(ignored.constructed()).hasSize(1);
        }
    }

    @Test
    void invalidateTokenShouldClearCachedTokenAndSetExpiryToNowOrEarlier() {
        TokenManager tokenManager = new TokenManager("client-id", "client-secret", "https://issuer/token");
        this.writeField(tokenManager, "token", "cached-token");
        this.writeField(tokenManager, "expiry", Instant.now().plusSeconds(600));

        Instant beforeInvalidate = Instant.now();
        tokenManager.invalidateToken();

        assertThat(this.readField(tokenManager, "token")).isNull();
        assertThat((Instant) this.readField(tokenManager, "expiry")).isBeforeOrEqualTo(Instant.now());
        assertThat((Instant) this.readField(tokenManager, "expiry")).isAfterOrEqualTo(beforeInvalidate);
    }

    @Test
    void getTokenShouldSendExpectedAuthorizationHeaderAndFormBody() {
        TokenManager tokenManager = new TokenManager("client-id", "client-secret", "https://issuer/token");

        try (MockedConstruction<RestTemplate> ignored = mockConstruction(RestTemplate.class, (mock, context) ->
                when(mock.postForEntity(eq("https://issuer/token"), any(HttpEntity.class), eq(Map.class)))
                        .thenReturn(ResponseEntity.ok(Map.of(
                                "access_token", "token-2",
                                "expires_in", 900
                        )))
        )) {
            tokenManager.getToken();

            RestTemplate constructed = ignored.constructed().getFirst();
            @SuppressWarnings("unchecked")
            HttpEntity<MultiValueMap<String, String>> request =
                    (HttpEntity<MultiValueMap<String, String>>) org.mockito.Mockito.mockingDetails(constructed)
                            .getInvocations()
                            .stream()
                            .filter(invocation -> invocation.getMethod().getName().equals("postForEntity"))
                            .findFirst()
                            .orElseThrow()
                            .getArgument(1);

            String expectedAuthorization = "Basic " + Base64.getEncoder()
                    .encodeToString("client-id:client-secret".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            HttpHeaders headers = request.getHeaders();

            assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo(expectedAuthorization);
            assertThat(headers.getContentType()).isNotNull();
            assertThat(request.getBody()).isNotNull();
            assertThat(request.getBody().getFirst("grant_type")).isEqualTo("client_credentials");
            assertThat(request.getBody().getFirst("scope")).isEqualTo(TokenManager.SCOPE_PROFILE);
            assertThat(request.getBody().getFirst("role")).isEqualTo(TokenManager.ROLE_URL_TOKEN);
        }
    }

    private Object readField(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void writeField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

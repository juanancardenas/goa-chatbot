package es.upm.api.infrastructure.security;

import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.exceptions.ForbiddenException;
import es.upm.api.domain.model.configuration.AuthenticatedUserContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatedUserContextResolverTest {

    private final AuthenticatedUserContextResolver resolver = new AuthenticatedUserContextResolver();

    @Test
    void resolveShouldReturnClientProfileForCustomerRole() {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("customer-1", "password", "customer");

        AuthenticatedUserContext context = this.resolver.resolve(authentication);

        assertThat(context.getUserId()).isEqualTo("customer-1");
        assertThat(context.getProfile()).isEqualTo(ConversationProfileType.CLIENT);
    }

    @Test
    void resolveShouldReturnProfessionalProfileForNonCustomerRole() {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("manager-1", "password", "ROLE_MANAGER");

        AuthenticatedUserContext context = this.resolver.resolve(authentication);

        assertThat(context.getUserId()).isEqualTo("manager-1");
        assertThat(context.getProfile()).isEqualTo(ConversationProfileType.PROFESSIONAL);
    }

    @Test
    void resolveShouldRejectMissingAuthentication() {
        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> this.resolver.resolve(null)
        );

        assertThat(exception).hasMessageContaining("Usuario autenticado no disponible");
    }
}

package es.upm.api.infrastructure.security;

import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.exceptions.ForbiddenException;
import es.upm.api.domain.model.configuration.AuthenticatedUserContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void resolveShouldReturnClientProfileForRolePrefixedCustomerAuthority() {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("customer-2", "password", "ROLE_customer");

        AuthenticatedUserContext context = this.resolver.resolve(authentication);

        assertThat(context.getUserId()).isEqualTo("customer-2");
        assertThat(context.getProfile()).isEqualTo(ConversationProfileType.CLIENT);
    }

    @Test
    void resolveShouldRejectAuthenticationWithNullName() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(null);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> this.resolver.resolve(authentication)
        );

        assertThat(exception).hasMessageContaining("Usuario autenticado no disponible");
    }

    @Test
    void resolveShouldRejectAuthenticationWithBlankName() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("   ");

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> this.resolver.resolve(authentication)
        );

        assertThat(exception).hasMessageContaining("Usuario autenticado no disponible");
    }

    @Test
    void resolveShouldIgnoreNullAuthoritiesAndKeepProfessionalProfile() {
        Authentication authentication = mock(Authentication.class);
        GrantedAuthority nullAuthority = () -> null;
        GrantedAuthority managerAuthority = () -> "ROLE_MANAGER";
        Collection<GrantedAuthority> authorities = List.of(nullAuthority, managerAuthority);
        when(authentication.getName()).thenReturn("manager-2");
        doReturn(authorities).when(authentication).getAuthorities();

        AuthenticatedUserContext context = this.resolver.resolve(authentication);

        assertThat(context.getUserId()).isEqualTo("manager-2");
        assertThat(context.getProfile()).isEqualTo(ConversationProfileType.PROFESSIONAL);
    }
}

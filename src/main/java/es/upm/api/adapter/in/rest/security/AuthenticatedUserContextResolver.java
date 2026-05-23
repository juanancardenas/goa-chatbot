package es.upm.api.adapter.in.rest.security;

import es.upm.api.domain.enums.ConversationProfileType;
import es.upm.api.domain.exceptions.ForbiddenException;
import es.upm.api.domain.model.security.AuthenticatedUserContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AuthenticatedUserContextResolver {

    public AuthenticatedUserContext resolve(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ForbiddenException("Usuario autenticado no disponible");
        }

        boolean isCustomer = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::normalizeAuthority)
                .anyMatch("CUSTOMER"::equals);

        return AuthenticatedUserContext.builder()
                .userId(authentication.getName())
                .profile(isCustomer ? ConversationProfileType.CLIENT : ConversationProfileType.PROFESSIONAL)
                .build();
    }

    private String normalizeAuthority(String authority) {
        if (authority == null) {
            return "";
        }

        return authority.replace("ROLE_", "").toUpperCase(Locale.ROOT);
    }
}

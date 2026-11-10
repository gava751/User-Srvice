package com.innoboard.userservice.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

@Component("currentUser")
public class CurrentUser {

    private static final String USER_ID_CLAIM = "user_id";
    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    public long id(Jwt jwt) {
        if (jwt.getClaim(USER_ID_CLAIM) instanceof Number number) {
            return number.longValue();
        }
        throw new InvalidBearerTokenException("Missing user_id claim");
    }

    public boolean isSelf(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof Jwt jwt
                && jwt.getClaim(USER_ID_CLAIM) instanceof Number number
                && number.longValue() == userId;
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
    }

    public long resolveTarget(Jwt jwt, Long pathUserId) {
        if (isAdmin()) {
            return pathUserId;
        }
        long currentId = id(jwt);
        if (currentId != pathUserId) {
            throw new AccessDeniedException("Cannot access resources of another user");
        }
        return currentId;
    }
}

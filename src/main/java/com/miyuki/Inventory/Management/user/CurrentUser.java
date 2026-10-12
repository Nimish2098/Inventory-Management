package com.miyuki.Inventory.Management.user;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return parseId(jwt.getSubject());
        }
        if (principal instanceof UserAccount user && user.getId() != null) {
            return user.getId();
        }
        throw new AccessDeniedException("Authenticated user identity is unavailable");
    }

    private Long parseId(String subject) {
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException("Authenticated user identity is invalid");
        }
    }
}

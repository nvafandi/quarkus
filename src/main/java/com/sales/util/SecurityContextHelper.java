package com.sales.util;

import com.sales.entity.UserEntity;
import com.sales.exception.BadRequestException;
import com.sales.repository.UserRepository;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

@ApplicationScoped
public class SecurityContextHelper {

    @Inject
    UserRepository userRepository;

    /**
     * Extract the authenticated user's UUID from the security context.
     * For JWT tokens: reads the subject (user UUID) from the token.
     * For test security contexts: looks up the user by username in the database.
     *
     * @param securityIdentity the injected security context
     * @return the authenticated user's UUID
     * @throws ForbiddenException if not authenticated
     * @throws BadRequestException if user ID format is invalid or user not found
     */
    public UUID extractUserId(SecurityIdentity securityIdentity) {
        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            throw new ForbiddenException("Authentication required");
        }
        if (securityIdentity.getPrincipal() instanceof JsonWebToken jwt) {
            try {
                return UUID.fromString(jwt.getSubject());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid user ID format in token");
            }
        }
        // Fallback for @TestSecurity: look up user by username in database
        String username = securityIdentity.getPrincipal().getName();
        return userRepository.findByUsername(username)
                .map(UserEntity::getId)
                .orElseThrow(() -> new BadRequestException("User not found: " + username));
    }
}

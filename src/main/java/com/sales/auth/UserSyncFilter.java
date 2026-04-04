package com.sales.auth;

import com.sales.service.UserSyncService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Provider
@Priority(Priorities.AUTHENTICATION + 100)
public class UserSyncFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(UserSyncFilter.class);

    @Inject
    UserSyncService userSyncService;

    @Context
    SecurityIdentity securityIdentity;

    @ConfigProperty(name = "keycloak.admin.sync-users-on-login", defaultValue = "true")
    boolean syncUsersOnLogin;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!syncUsersOnLogin) {
            return;
        }

        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            return;
        }

        try {
            String username = securityIdentity.getPrincipal().getName();

            String keycloakId = extractKeycloakId();
            if (keycloakId == null) {
                LOG.debugf("Could not extract Keycloak ID for user %s, skipping sync", username);
                return;
            }

            List<String> roles = securityIdentity.getRoles().stream().toList();
            String role = roles.isEmpty() ? "USER" : roles.get(0);

            LOG.infof("Auto-syncing authenticated user %s (Keycloak ID: %s) to local database",
                    username, keycloakId);

            userSyncService.syncUserFromKeycloak(Map.of(
                    "id", keycloakId,
                    "username", username,
                    "role", role
            ));

        } catch (Exception e) {
            LOG.warnf("Failed to sync user on login: %s", e.getMessage());
        }
    }

    private String extractKeycloakId() {
        try {
            if (securityIdentity.getPrincipal() instanceof JsonWebToken) {
                JsonWebToken jwt = (JsonWebToken) securityIdentity.getPrincipal();
                return jwt.getSubject();
            }
        } catch (Exception e) {
            LOG.debugf("Could not extract Keycloak ID: %s", e.getMessage());
        }
        return null;
    }
}


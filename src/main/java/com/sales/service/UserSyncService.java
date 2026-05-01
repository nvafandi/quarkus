package com.sales.service;

import com.sales.dto.UserDTO;
import com.sales.exception.ConflictException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class UserSyncService {

    private static final Logger LOG = Logger.getLogger(UserSyncService.class);

    @Inject
    UserService userService;

    @Inject
    KeycloakAdminClient keycloakAdminClient;

    @ConfigProperty(name = "keycloak.admin.target-realm", defaultValue = "sales-realm")
    String targetRealm;

    @Transactional
    public Uni<UserDTO> syncUserFromKeycloak(Map<String, Object> keycloakUser) {
        return Uni.createFrom().item(() -> {
            String keycloakId = (String) keycloakUser.get("id");
            String username = (String) keycloakUser.get("username");
            String email = (String) keycloakUser.get("email");

            if (keycloakId == null || username == null) {
                throw new IllegalArgumentException("Keycloak user must have id and username");
            }

            LOG.infof("Syncing Keycloak user %s (ID: %s) to local database", username, keycloakId);

            String role = extractRoleFromKeycloak(keycloakId);

            // Note: userService.createOrUpdateFromKeycloak now returns Uni, but we handle it synchronously here
            UserDTO syncedUser = userService.createOrUpdateFromKeycloak(keycloakId, username, role).await().indefinitely();

            LOG.infof("Successfully synced user %s (Local ID: %s)", username, syncedUser.getId());

            return syncedUser;
        });
    }

    @Transactional
    public Uni<Void> deleteUserByKeycloakId(String keycloakId) {
        return Uni.createFrom().item(() -> {
                    UserDTO user = userService.findByKeycloakId(keycloakId).await().indefinitely();
                    if (user != null) {
                        LOG.infof("Deleting local user record for Keycloak ID: %s (Local ID: %s)",
                                keycloakId, user.getId());
                        userService.delete(user.getId()).await().indefinitely();
                    }
                    return null;
                })
                .replaceWithVoid();
    }

    private String extractRoleFromKeycloak(String keycloakId) {
        try {
            List<String> roles = keycloakAdminClient.getUserRoles(keycloakId);
            if (roles != null && !roles.isEmpty()) {
                return roles.get(0);
            }
        } catch (Exception e) {
            LOG.warnf("Failed to extract roles for Keycloak user %s: %s",
                    keycloakId, e.getMessage());
        }
        return "USER";
    }
}

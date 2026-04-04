package com.sales;

import com.sales.dto.UserDTO;
import com.sales.repository.UserRepository;
import com.sales.service.UserSyncService;
import com.sales.service.UserService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserSyncServiceTest {

    @Inject
    UserSyncService userSyncService;

    @Inject
    UserService userService;

    @Inject
    UserRepository userRepository;

    @Test
    @Order(1)
    @DisplayName("Sync User - Should Create Local User Record")
    public void testSyncUser_CreatesLocalRecord() {
        String keycloakId = "test-keycloak-id-" + System.currentTimeMillis();
        String username = "testuser_sync_" + System.currentTimeMillis();

        Map<String, Object> keycloakUser = Map.of(
                "id", keycloakId,
                "username", username
        );

        UserDTO syncedUser = userSyncService.syncUserFromKeycloak(keycloakUser);

        assertNotNull(syncedUser);
        assertNotNull(syncedUser.getId());
        assertEquals(username, syncedUser.getUsername());
        assertEquals(keycloakId, syncedUser.getKeycloakId());
        assertEquals("USER", syncedUser.getRole());

        // Verify user exists in database
        UserDTO found = userService.findByKeycloakId(keycloakId);
        assertNotNull(found);
        assertEquals(syncedUser.getId(), found.getId());

        // Cleanup
        try {
            userService.delete(syncedUser.getId());
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    @Order(2)
    @DisplayName("Sync User - Should Update Existing User")
    public void testSyncUser_UpdatesExistingRecord() {
        String keycloakId = "test-keycloak-id-update-" + System.currentTimeMillis();
        String username = "testuser_update_" + System.currentTimeMillis();

        Map<String, Object> keycloakUser = Map.of(
                "id", keycloakId,
                "username", username
        );

        // First sync - creates user
        UserDTO firstSync = userSyncService.syncUserFromKeycloak(keycloakUser);
        assertNotNull(firstSync);

        // Second sync - should update existing user
        String updatedUsername = username + "_updated";
        Map<String, Object> updatedUser = Map.of(
                "id", keycloakId,
                "username", updatedUsername
        );

        UserDTO secondSync = userSyncService.syncUserFromKeycloak(updatedUser);
        assertNotNull(secondSync);
        assertEquals(firstSync.getId(), secondSync.getId());
        assertEquals(updatedUsername, secondSync.getUsername());

        // Cleanup
        try {
            userService.delete(firstSync.getId());
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    @Order(3)
    @DisplayName("Delete User by Keycloak ID - Should Remove Local Record")
    public void testDeleteUser_ByKeycloakId() {
        String keycloakId = "test-keycloak-id-delete-" + System.currentTimeMillis();
        String username = "testuser_delete_" + System.currentTimeMillis();

        // Create user
        Map<String, Object> keycloakUser = Map.of(
                "id", keycloakId,
                "username", username
        );

        UserDTO createdUser = userSyncService.syncUserFromKeycloak(keycloakUser);
        assertNotNull(createdUser);

        // Verify user exists
        UserDTO found = userService.findByKeycloakId(keycloakId);
        assertNotNull(found);

        // Delete user
        userSyncService.deleteUserByKeycloakId(keycloakId);

        // Verify user is deleted
        UserDTO deleted = userService.findByKeycloakId(keycloakId);
        assertNull(deleted);
    }

    @Test
    @Order(4)
    @DisplayName("Sync User - Should Handle Missing Keycloak ID")
    public void testSyncUser_MissingKeycloakId() {
        Map<String, Object> keycloakUser = Map.of(
                "username", "testuser_no_id"
        );

        assertThrows(IllegalArgumentException.class, () -> {
            userSyncService.syncUserFromKeycloak(keycloakUser);
        });
    }

    @Test
    @Order(5)
    @DisplayName("Find User by Keycloak ID - Should Return Null for Non-Existent")
    public void testFindUser_ByKeycloakId_NotFound() {
        UserDTO found = userService.findByKeycloakId("non-existent-keycloak-id");
        assertNull(found);
    }

    @Test
    @DisplayName("User Entity - Should Have Keycloak ID Field")
    public void testUserEntity_HasKeycloakIdField() {
        String keycloakId = "test-keycloak-id-field";
        String username = "testuser_field_" + System.currentTimeMillis();

        Map<String, Object> keycloakUser = Map.of(
                "id", keycloakId,
                "username", username
        );

        UserDTO syncedUser = userSyncService.syncUserFromKeycloak(keycloakUser);

        assertNotNull(syncedUser.getKeycloakId());
        assertEquals(keycloakId, syncedUser.getKeycloakId());

        // Cleanup
        try {
            userService.delete(syncedUser.getId());
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}

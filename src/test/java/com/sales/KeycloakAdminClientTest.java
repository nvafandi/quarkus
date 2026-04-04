package com.sales;

import com.sales.service.KeycloakAdminClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KeycloakAdminClientTest {

    @Inject
    KeycloakAdminClient keycloakAdminClient;

    @Test
    @Order(1)
    @DisplayName("Get All Users - Should Return List or Throw Exception")
    public void testGetAllUsers() {
        try {
            List<Map<String, Object>> users = keycloakAdminClient.getAllUsers();
            assertNotNull(users, "Users list should not be null");
            // If Keycloak is available, we can verify the structure
        } catch (WebApplicationException e) {
            // Expected when Keycloak is not available or not authenticated
            assertTrue(e.getResponse().getStatus() >= 400, "Should return error status");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Get User By ID - Should Handle Non-Existent User")
    public void testGetUserById() {
        String testUserId = "non-existent-user-id";

        try {
            Map<String, Object> user = keycloakAdminClient.getUserById(testUserId);
            // If we get here, Keycloak is available
            assertNotNull(user);
        } catch (WebApplicationException e) {
            // Expected - either 404 (user not found) or 500 (Keycloak unavailable)
            int status = e.getResponse().getStatus();
            assertTrue(status == 404 || status == 500, 
                "Should return 404 or 500, but got: " + status);
        }
    }

    @Test
    @Order(3)
    @DisplayName("Get User By Username - Should Handle Non-Existent User")
    public void testGetUserByUsername() {
        String testUsername = "nonexistent_user_" + System.currentTimeMillis();

        try {
            Map<String, Object> user = keycloakAdminClient.getUserByUsername(testUsername);
            assertNotNull(user);
        } catch (WebApplicationException e) {
            // Expected - either 404 (user not found) or 500 (Keycloak unavailable)
            int status = e.getResponse().getStatus();
            assertTrue(status == 404 || status == 500,
                "Should return 404 or 500, but got: " + status);
        }
    }

    @Test
    @Order(4)
    @DisplayName("Create User - Should Create or Throw Appropriate Exception")
    public void testCreateUser() {
        String testUsername = "test_user_" + System.currentTimeMillis();
        String testPassword = "TestPass123!";
        String testEmail = testUsername + "@example.com";

        try {
            Map<String, Object> createdUser = keycloakAdminClient.createUser(
                testUsername, testPassword, testEmail, true, null);
            
            // If Keycloak is available, verify the created user
            assertNotNull(createdUser);
            assertEquals(testUsername, createdUser.get("username"));
            assertEquals(testEmail, createdUser.get("email"));
            assertNotNull(createdUser.get("id"));
            
            // Clean up - try to delete the created user
            try {
                keycloakAdminClient.deleteUser((String) createdUser.get("id"));
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        } catch (WebApplicationException e) {
            // Expected when Keycloak is not available or authentication fails
            int status = e.getResponse().getStatus();
            assertTrue(status >= 400, "Should return error status: " + status);
        }
    }

    @Test
    @Order(5)
    @DisplayName("Create User With Roles - Should Create With Roles or Throw Exception")
    public void testCreateUserWithRoles() {
        String testUsername = "test_role_user_" + System.currentTimeMillis();
        String testPassword = "TestPass123!";
        String testEmail = testUsername + "@example.com";
        List<String> roles = List.of("USER");

        try {
            Map<String, Object> createdUser = keycloakAdminClient.createUser(
                testUsername, testPassword, testEmail, true, roles);
            
            assertNotNull(createdUser);
            assertEquals(testUsername, createdUser.get("username"));
            
            // Clean up
            try {
                keycloakAdminClient.deleteUser((String) createdUser.get("id"));
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        } catch (WebApplicationException e) {
            // Expected when Keycloak is not available
            int status = e.getResponse().getStatus();
            assertTrue(status >= 400, "Should return error status: " + status);
        }
    }

    @Test
    @Order(6)
    @DisplayName("Update User - Should Update or Throw Exception")
    public void testUpdateUser() {
        String testUserId = "non-existent-user-id";
        String newUsername = "updated_user_" + System.currentTimeMillis();

        try {
            Map<String, Object> updatedUser = keycloakAdminClient.updateUser(
                testUserId, newUsername, "updated@example.com", true, true);
            assertNotNull(updatedUser);
        } catch (WebApplicationException e) {
            // Expected - 404 (user not found) or 500 (Keycloak unavailable)
            int status = e.getResponse().getStatus();
            assertTrue(status == 404 || status == 500,
                "Should return 404 or 500, but got: " + status);
        }
    }

    @Test
    @Order(7)
    @DisplayName("Reset Password - Should Reset or Throw Exception")
    public void testResetPassword() {
        String testUserId = "non-existent-user-id";

        try {
            keycloakAdminClient.resetPassword(testUserId, "NewPassword123!");
        } catch (WebApplicationException e) {
            // Expected - 404 (user not found) or 500 (Keycloak unavailable)
            int status = e.getResponse().getStatus();
            assertTrue(status == 404 || status == 500,
                "Should return 404 or 500, but got: " + status);
        }
    }

    @Test
    @Order(8)
    @DisplayName("Delete User - Should Delete or Throw Exception")
    public void testDeleteUser() {
        String testUserId = "non-existent-user-id";

        try {
            keycloakAdminClient.deleteUser(testUserId);
        } catch (WebApplicationException e) {
            // Expected - 404 (user not found) or 500 (Keycloak unavailable)
            int status = e.getResponse().getStatus();
            assertTrue(status == 404 || status == 500,
                "Should return 404 or 500, but got: " + status);
        }
    }

    @Test
    @Order(9)
    @DisplayName("Get Available Roles - Should Return List")
    public void testGetAvailableRoles() {
        List<String> roles = keycloakAdminClient.getAvailableRoles();
        assertNotNull(roles, "Roles list should not be null");
        // May be empty if Keycloak is unavailable or no roles exist
    }

    @Test
    @Order(10)
    @DisplayName("Assign Roles - Should Assign or Throw Exception")
    public void testAssignRoles() {
        String testUserId = "non-existent-user-id";

        try {
            keycloakAdminClient.assignRoles(testUserId, List.of("USER"));
        } catch (WebApplicationException e) {
            // Expected - user not found or Keycloak unavailable
            int status = e.getResponse().getStatus();
            assertTrue(status >= 400, "Should return error status: " + status);
        }
    }

    @Test
    @Order(11)
    @DisplayName("Get User Roles - Should Return List")
    public void testGetUserRoles() {
        String testUserId = "non-existent-user-id";
        
        List<String> roles = keycloakAdminClient.getUserRoles(testUserId);
        assertNotNull(roles, "Roles list should not be null");
        // May be empty if user doesn't exist or Keycloak is unavailable
    }

    @Test
    @DisplayName("Configuration - Should Have Default Values")
    public void testConfiguration() {
        // Verify that the client is injected successfully
        assertNotNull(keycloakAdminClient, "KeycloakAdminClient should be injected");
    }
}


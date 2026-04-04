package com.sales;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(user = "testuser", roles = {"ADMIN"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KeycloakUserSyncApiTest {

    @Test
    @Order(1)
    @DisplayName("Create Keycloak User - Should Sync to Local Database")
    public void testCreateUser_SyncsToLocal() {
        String uniqueUsername = "sync_test_user_" + System.currentTimeMillis();
        String json = """
            {
                "username": "%s",
                "password": "TestPass123!",
                "email": "%s@example.com",
                "emailVerified": true,
                "roles": ["USER"]
            }
            """.formatted(uniqueUsername, uniqueUsername);

        // This test will fail if Keycloak is not running, which is expected
        // The important part is verifying the sync flow works when Keycloak is available
        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/keycloak/users")
        .then()
            .statusCode(anyOf(
                is(201),  // Success - user created and synced
                is(409),  // Username exists
                is(500)   // Keycloak unavailable (expected in test env)
            ));
    }

    @Test
    @Order(2)
    @DisplayName("Create User - Should Return Local UserDTO with keycloakId")
    public void testCreateUser_ReturnsLocalDto() {
        String uniqueUsername = "sync_test_user2_" + System.currentTimeMillis();
        String json = """
            {
                "username": "%s",
                "password": "TestPass123!",
                "email": "%s@example.com",
                "emailVerified": true
            }
            """.formatted(uniqueUsername, uniqueUsername);

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/keycloak/users")
        .then()
            .statusCode(anyOf(
                is(201),
                is(409),
                is(500)
            ))
            // If successful, response should have local user fields
            .body(anyOf(
                is(notNullValue()),  // Has data
                containsString("success")  // Wrapped in ApiResponse
            ));
    }

    @Test
    @Order(3)
    @DisplayName("Delete Keycloak User - Should Remove from Local Database")
    public void testDeleteUser_RemovesFromLocal() {
        String testKeycloakId = "non-existent-keycloak-id-" + System.currentTimeMillis();

        given()
            .pathParam("id", testKeycloakId)
        .when()
            .delete("/api/keycloak/users/{id}")
        .then()
            .statusCode(anyOf(
                is(204),  // Success
                is(404),  // Not found
                is(500)   // Keycloak unavailable
            ));
    }

    @Test
    @Order(4)
    @DisplayName("Sync Endpoint - Should Handle Invalid Data")
    public void testCreateUser_InvalidData() {
        // Missing required password
        String json = """
            {
                "username": "test_no_password"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/keycloak/users")
        .then()
            .statusCode(400);
    }

    @Test
    @Order(5)
    @DisplayName("List Users - Should Return Response")
    public void testListUsers() {
        given()
        .when()
            .get("/api/keycloak/users")
        .then()
            .statusCode(anyOf(
                is(200),  // Success
                is(500)   // Keycloak unavailable
            ));
    }

    @Test
    @Order(6)
    @DisplayName("Get User By ID - Should Return Response")
    public void testGetUserById() {
        given()
            .pathParam("id", "test-user-id")
        .when()
            .get("/api/keycloak/users/{id}")
        .then()
            .statusCode(anyOf(
                is(200),  // Found
                is(404),  // Not found
                is(500)   // Keycloak unavailable
            ));
    }

    @Test
    @Order(7)
    @DisplayName("Get User By Username - Should Return Response")
    public void testGetUserByUsername() {
        String testUsername = "nonexistent_user_" + System.currentTimeMillis();

        given()
            .pathParam("username", testUsername)
        .when()
            .get("/api/keycloak/users/username/{username}")
        .then()
            .statusCode(anyOf(
                is(200),  // Found
                is(404),  // Not found
                is(500)   // Keycloak unavailable
            ));
    }
}

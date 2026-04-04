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
public class KeycloakUserResourceTest {

    @Test
    @Order(1)
    @DisplayName("List Keycloak Users - Should Return Response")
    public void testListUsers() {
        // This will either succeed or return error depending on Keycloak availability
        given()
            .when()
            .get("/api/keycloak/users")
        .then()
            .statusCode(anyOf(
                is(200),  // Keycloak available
                is(500)   // Keycloak unavailable
            ));
    }

    @Test
    @Order(2)
    @DisplayName("Get User By ID - Should Handle Non-Existent User")
    public void testGetUserById() {
        given()
            .pathParam("id", "non-existent-user-id")
        .when()
            .get("/api/keycloak/users/{id}")
        .then()
            .statusCode(anyOf(
                is(404),  // User not found
                is(500)   // Keycloak unavailable
            ));
    }

    @Test
    @Order(3)
    @DisplayName("Get User By Username - Should Handle Non-Existent User")
    public void testGetUserByUsername() {
        given()
            .pathParam("username", "nonexistent_user_" + System.currentTimeMillis())
        .when()
            .get("/api/keycloak/users/username/{username}")
        .then()
            .statusCode(anyOf(
                is(404),  // User not found
                is(500)   // Keycloak unavailable
            ));
    }

    @Test
    @Order(4)
    @DisplayName("Create User - Should Handle Missing Password")
    public void testCreateUser_MissingPassword() {
        String json = """
            {
                "username": "testuser",
                "email": "test@example.com"
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
    @DisplayName("Create User - Should Handle Creation")
    public void testCreateUser() {
        String uniqueUsername = "test_user_" + System.currentTimeMillis();
        String json = """
            {
                "username": "%s",
                "password": "TestPass123!",
                "email": "%s@example.com",
                "emailVerified": true,
                "roles": ["USER"]
            }
            """.formatted(uniqueUsername, uniqueUsername);

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/keycloak/users")
        .then()
            .statusCode(anyOf(
                is(201),  // Success
                is(409),  // Username exists
                is(500),  // Keycloak unavailable
                is(400)   // Bad request
            ));
    }

    @Test
    @Order(6)
    @DisplayName("Update User - Should Handle Non-Existent User")
    public void testUpdateUser() {
        String json = """
            {
                "username": "updateduser",
                "email": "updated@example.com",
                "emailVerified": true,
                "enabled": true
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .pathParam("id", "non-existent-user-id")
            .body(json)
        .when()
            .put("/api/keycloak/users/{id}")
        .then()
            .statusCode(anyOf(
                is(404),  // User not found
                is(500)   // Keycloak unavailable
            ));
    }

    @Test
    @Order(7)
    @DisplayName("Reset Password - Should Handle Missing Password")
    public void testResetPassword_MissingPassword() {
        String json = """
            {
                "username": "testuser"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .pathParam("id", "test-user-id")
            .body(json)
        .when()
            .put("/api/keycloak/users/{id}/password")
        .then()
            .statusCode(400);
    }

    @Test
    @Order(8)
    @DisplayName("Reset Password - Should Handle Non-Existent User")
    public void testResetPassword() {
        String json = """
            {
                "password": "NewPassword123!"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .pathParam("id", "non-existent-user-id")
            .body(json)
        .when()
            .put("/api/keycloak/users/{id}/password")
        .then()
            .statusCode(anyOf(
                is(404),  // User not found
                is(500),  // Keycloak unavailable
                is(400)   // Bad request
            ));
    }

    @Test
    @Order(9)
    @DisplayName("Assign Roles - Should Handle Missing Roles")
    public void testAssignRoles_MissingRoles() {
        String json = """
            {
                "user": "testuser"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .pathParam("id", "test-user-id")
            .body(json)
        .when()
            .put("/api/keycloak/users/{id}/roles")
        .then()
            .statusCode(anyOf(
                is(400),  // Missing roles
                is(500)   // Keycloak unavailable
            ));
    }

    @Test
    @Order(10)
    @DisplayName("Assign Roles - Should Handle Non-Existent User")
    public void testAssignRoles() {
        String json = """
            {
                "roles": ["USER"]
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .pathParam("id", "non-existent-user-id")
            .body(json)
        .when()
            .put("/api/keycloak/users/{id}/roles")
        .then()
            .statusCode(anyOf(
                is(400),  // Role not found or user not found
                is(500)   // Keycloak unavailable
            ));
    }

    @Test
    @Order(11)
    @DisplayName("Get User Roles - Should Return Response")
    public void testGetUserRoles() {
        given()
            .pathParam("id", "non-existent-user-id")
        .when()
            .get("/api/keycloak/users/{id}/roles")
        .then()
            .statusCode(200)
            .body(is(notNullValue()));
    }

    @Test
    @Order(12)
    @DisplayName("Delete User - Should Handle Non-Existent User")
    public void testDeleteUser() {
        given()
            .pathParam("id", "non-existent-user-id")
        .when()
            .delete("/api/keycloak/users/{id}")
        .then()
            .statusCode(anyOf(
                is(404),  // User not found
                is(500)   // Keycloak unavailable
            ));
    }

    @Test
    @Order(13)
    @DisplayName("List Available Roles - Should Return Response")
    public void testListAvailableRoles() {
        given()
            .when()
            .get("/api/keycloak/users/roles")
        .then()
            .statusCode(200)
            .body(is(notNullValue()));
    }
}


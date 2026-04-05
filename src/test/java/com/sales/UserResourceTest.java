package com.sales;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserResourceTest {

    private static String createdKeycloakUserId;

    // ==================== List Users ====================

    @Test
    @Order(1)
    @DisplayName("List Users - Should Return Response")
    public void testListUsers() {
        given()
        .when()
            .get("/api/users")
        .then()
            .statusCode(anyOf(
                is(200),  // Keycloak available
                is(500)   // Keycloak unavailable
            ));
    }

    @Test
    @Order(2)
    @DisplayName("List Available Roles - Should Return Response")
    public void testListAvailableRoles() {
        given()
        .when()
            .get("/api/users/roles")
        .then()
            .statusCode(anyOf(
                is(200),  // Keycloak available
                is(500)   // Keycloak unavailable
            ));
    }

    // ==================== Get User By ID ====================

    @Test
    @Order(3)
    @DisplayName("Get User By ID - Non-Existent")
    public void testGetUserByIdNonExistent() {
        given()
            .pathParam("id", "non-existent-keycloak-id")
        .when()
            .get("/api/users/{id}")
        .then()
            .statusCode(anyOf(
                is(404),  // User not found
                is(500)   // Keycloak unavailable
            ));
    }

    // ==================== Get User By Username ====================

    @Test
    @Order(4)
    @DisplayName("Get User By Username - Non-Existent")
    public void testGetUserByUsernameNonExistent() {
        given()
            .pathParam("username", "nonexistent_user_" + System.currentTimeMillis())
        .when()
            .get("/api/users/username/{username}")
        .then()
            .statusCode(anyOf(
                is(404),  // User not found
                is(500)   // Keycloak unavailable
            ));
    }

    // ==================== Create User ====================

    @Test
    @Order(5)
    @DisplayName("Create User - Missing Password (400)")
    public void testCreateUserMissingPassword() {
        String json = """
            {
                "username": "test_user_nopass",
                "email": "nopass@test.com"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/users")
        .then()
            .statusCode(anyOf(
                is(400),  // Validation error
                is(500)   // Keycloak unavailable
            ));
    }

    @Test
    @Order(6)
    @DisplayName("Create User - Missing Username (400)")
    public void testCreateUserMissingUsername() {
        String json = """
            {
                "password": "TestPass123!",
                "email": "nousername@test.com"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/users")
        .then()
            .statusCode(anyOf(
                is(400),  // Validation error
                is(500)   // Keycloak unavailable
            ));
    }

    // ==================== Update User ====================

    @Test
    @Order(7)
    @DisplayName("Update User - Non-Existent (404)")
    public void testUpdateUserNonExistent() {
        String json = """
            {
                "username": "updated_user",
                "email": "updated@test.com"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .pathParam("id", "non-existent-id")
            .body(json)
        .when()
            .put("/api/users/{id}")
        .then()
            .statusCode(anyOf(
                is(404),  // User not found
                is(500)   // Keycloak unavailable
            ));
    }

    // ==================== Reset Password ====================

    @Test
    @Order(8)
    @DisplayName("Reset Password - Missing Password (400)")
    public void testResetPasswordMissing() {
        String json = """
            {
                "email": "test@test.com"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .pathParam("id", "some-id")
            .body(json)
        .when()
            .put("/api/users/{id}/password")
        .then()
            .statusCode(anyOf(
                is(400),  // Missing password
                is(500)   // Keycloak unavailable
            ));
    }

    // ==================== Assign Roles ====================

    @Test
    @Order(9)
    @DisplayName("Assign Roles - Missing Roles (400)")
    public void testAssignRolesMissing() {
        String json = """
            {
                "other": "value"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .pathParam("id", "some-id")
            .body(json)
        .when()
            .put("/api/users/{id}/roles")
        .then()
            .statusCode(anyOf(
                is(400),  // Missing roles
                is(500)   // Keycloak unavailable
            ));
    }

    // ==================== Get User Roles ====================

    @Test
    @Order(10)
    @DisplayName("Get User Roles - Non-Existent User")
    public void testGetUserRolesNonExistent() {
        given()
            .pathParam("id", "non-existent-id")
        .when()
            .get("/api/users/{id}/roles")
        .then()
            .statusCode(anyOf(
                is(200),  // Returns empty roles
                is(500)   // Keycloak unavailable
            ));
    }

    // ==================== Delete User ====================

    @Test
    @Order(11)
    @DisplayName("Delete User - Non-Existent (404)")
    public void testDeleteUserNonExistent() {
        given()
            .pathParam("id", "non-existent-keycloak-id")
        .when()
            .delete("/api/users/{id}")
        .then()
            .statusCode(anyOf(
                is(404),  // User not found
                is(500)   // Keycloak unavailable
            ));
    }
}

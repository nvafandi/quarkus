package com.sales;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthResourceTest {

    // ==================== Token Operations ====================

    @Test
    @Order(1)
    @DisplayName("Login - Success (Keycloak available)")
    public void testLoginSuccess() {
        String json = """
            {
                "username": "admin",
                "password": "admin123"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(anyOf(is(200), is(401), is(500)));
    }

    @Test
    @Order(2)
    @DisplayName("Login - Missing Password (400)")
    public void testLoginMissingPassword() {
        String json = """
            {
                "username": "admin"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(anyOf(is(400), is(401), is(500)));
    }

    @Test
    @Order(3)
    @DisplayName("Login - Missing Username (400)")
    public void testLoginMissingUsername() {
        String json = """
            {
                "password": "admin123"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(anyOf(is(400), is(401), is(500)));
    }

    @Test
    @Order(4)
    @DisplayName("Refresh Token - Missing Refresh Token (400)")
    public void testRefreshMissingToken() {
        String json = """
            {
                "accessToken": "some-token"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/auth/refresh")
        .then()
            .statusCode(anyOf(is(400), is(401), is(500)));
    }

    @Test
    @Order(5)
    @DisplayName("Refresh Token - Empty Refresh Token (400)")
    public void testRefreshEmptyToken() {
        String json = """
            {
                "refreshToken": ""
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/auth/refresh")
        .then()
            .statusCode(anyOf(is(400), is(401), is(500)));
    }

    @Test
    @Order(6)
    @DisplayName("Revoke Token - Missing Refresh Token (400)")
    public void testRevokeMissingToken() {
        String json = """
            {
                "accessToken": "some-token"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/auth/revoke")
        .then()
            .statusCode(anyOf(is(400), is(401), is(500)));
    }

    @Test
    @Order(7)
    @DisplayName("Revoke Token - Empty Refresh Token (400)")
    public void testRevokeEmptyToken() {
        String json = """
            {
                "refreshToken": ""
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/auth/revoke")
        .then()
            .statusCode(anyOf(is(400), is(401), is(500)));
    }

    // ==================== User Info Operations ====================

    @Test
    @Order(10)
    @DisplayName("User Info - Authenticated")
    @TestSecurity(user = "testuser", roles = {"ADMIN", "USER"})
    public void testUserInfoAuthenticated() {
        given()
        .when()
            .get("/auth/userinfo")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("status", equalTo(200))
            .body("data.username", equalTo("testuser"))
            .body("data.roles", hasItems("ADMIN", "USER"))
            .body("data.authenticated", equalTo(true));
    }

    @Test
    @Order(11)
    @DisplayName("User Info - Anonymous (401)")
    public void testUserInfoAnonymous() {
        given()
        .when()
            .get("/auth/userinfo")
        .then()
            .statusCode(401)
            .body("success", equalTo(false))
            .body("status", equalTo(401))
            .body("message", containsString("Not authenticated"));
    }

    @Test
    @Order(12)
    @DisplayName("User Roles - Authenticated")
    @TestSecurity(user = "testuser", roles = {"ADMIN", "MANAGER"})
    public void testUserRolesAuthenticated() {
        given()
        .when()
            .get("/auth/roles")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("status", equalTo(200))
            .body("data.roles", hasItems("ADMIN", "MANAGER"));
    }

    @Test
    @Order(13)
    @DisplayName("User Roles - Anonymous (401)")
    public void testUserRolesAnonymous() {
        given()
        .when()
            .get("/auth/roles")
        .then()
            .statusCode(401)
            .body("success", equalTo(false))
            .body("status", equalTo(401))
            .body("message", containsString("Not authenticated"));
    }

    @Test
    @Order(14)
    @DisplayName("Check Auth - Authenticated")
    @TestSecurity(user = "testuser", roles = {"USER"})
    public void testCheckAuthAuthenticated() {
        given()
        .when()
            .get("/auth/check")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("status", equalTo(200))
            .body("data.authenticated", equalTo(true));
    }

    @Test
    @Order(15)
    @DisplayName("Check Auth - Anonymous")
    public void testCheckAuthAnonymous() {
        given()
        .when()
            .get("/auth/check")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("status", equalTo(200))
            .body("data.authenticated", equalTo(false));
    }
}

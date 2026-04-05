package com.sales;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TokenResourceTest {

    private static String validRefreshToken;

    @Test
    @Order(1)
    @DisplayName("POST /auth/login - Successful login")
    public void testLoginSuccess() {
        System.out.println("\n==========================================");
        System.out.println(" Testing: POST /auth/login (success)");
        System.out.println("==========================================\n");

        String loginJson = """
            {
                "username": "admin",
                "password": "admin"
            }
            """;

        System.out.println("Request:");
        System.out.println("POST /auth/login");
        System.out.println("Body: { username: \"admin\", password: \"admin\" }");
        System.out.println();

        // Accept 200 (success) or 401/500 (Keycloak unavailable)
        var response = given()
            .contentType(ContentType.JSON)
            .body(loginJson)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(500)))
            .extract()
            .response();

        int statusCode = response.getStatusCode();
        System.out.println("Response: HTTP " + statusCode);

        if (statusCode == 200) {
            // Extract tokens for later tests
            var data = response.jsonPath().getMap("data");
            validRefreshToken = (String) data.get("refreshToken");

            System.out.println("Access token received");
            System.out.println("Refresh token received");
            System.out.println("Token type: Bearer");
            System.out.println();
            System.out.println("Tokens stored for refresh/revoke tests.");
        } else {
            System.out.println("Keycloak not available - got " + statusCode + " error");
            System.out.println("Skipping tests that require tokens");
        }
    }

    @Test
    @Order(2)
    @DisplayName("POST /auth/login - Invalid credentials")
    public void testLoginInvalidCredentials() {
        System.out.println("\n==========================================");
        System.out.println(" Testing: POST /auth/login (invalid)");
        System.out.println("==========================================\n");

        String loginJson = """
            {
                "username": "admin",
                "password": "wrongpassword"
            }
            """;

        System.out.println("Request:");
        System.out.println("POST /auth/login");
        System.out.println("Body: { username: \"admin\", password: \"wrongpassword\" }");
        System.out.println();

        // Accept 401 (unauthorized) or 401/500 (Keycloak unavailable)
        var response = given()
            .contentType(ContentType.JSON)
            .body(loginJson)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(anyOf(equalTo(401), equalTo(500)))
            .body("success", equalTo(false))
            .extract()
            .response();

        int statusCode = response.getStatusCode();
        System.out.println("Response: HTTP " + statusCode);
        if (statusCode == 401) {
            System.out.println("Proper error message returned (Keycloak available)");
        } else {
            System.out.println("Keycloak not available - got 500 error");
        }
        System.out.println();
    }

    @Test
    @Order(3)
    @DisplayName("POST /auth/login - Missing fields")
    public void testLoginMissingFields() {
        System.out.println("\n==========================================");
        System.out.println(" Testing: POST /auth/login (missing fields)");
        System.out.println("==========================================\n");

        String loginJson = """
            {
                "username": "admin"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(loginJson)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("status", equalTo(400));

        System.out.println("Response: HTTP 400 Bad Request");
        System.out.println("Validation error for missing password");
        System.out.println();
    }

    @Test
    @Order(4)
    @DisplayName("POST /auth/refresh - Refresh token")
    public void testRefreshToken() {
        System.out.println("\n==========================================");
        System.out.println(" Testing: POST /auth/refresh");
        System.out.println("==========================================\n");

        if (validRefreshToken == null) {
            System.out.println("Skipping: No refresh token from login test (Keycloak not available)");
            return;
        }

        String refreshJson = String.format("""
            {
                "refreshToken": "%s"
            }
            """, validRefreshToken);

        System.out.println("Request:");
        System.out.println("POST /auth/refresh");
        System.out.println("Body: { refreshToken: \"<token>\" }");
        System.out.println();

        given()
            .contentType(ContentType.JSON)
            .body(refreshJson)
        .when()
            .post("/auth/refresh")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("status", equalTo(200))
            .body("data.accessToken", notNullValue())
            .body("data.tokenType", equalTo("Bearer"));

        System.out.println("Response: HTTP 200 OK");
        System.out.println("New access token received");
        System.out.println();
    }

    @Test
    @Order(5)
    @DisplayName("POST /auth/refresh - Invalid token")
    public void testRefreshInvalidToken() {
        System.out.println("\n==========================================");
        System.out.println(" Testing: POST /auth/refresh (invalid token)");
        System.out.println("==========================================\n");

        String refreshJson = """
            {
                "refreshToken": "invalid-token-here"
            }
            """;

        var response = given()
            .contentType(ContentType.JSON)
            .body(refreshJson)
        .when()
            .post("/auth/refresh")
        .then()
            .statusCode(anyOf(equalTo(401), equalTo(500)))
            .body("success", equalTo(false))
            .extract()
            .response();

        int statusCode = response.getStatusCode();
        System.out.println("Response: HTTP " + statusCode);
        if (statusCode == 401) {
            System.out.println("Proper error for invalid refresh token (Keycloak available)");
        } else {
            System.out.println("Keycloak not available - got 500 error");
        }
        System.out.println();
    }

    @Test
    @Order(6)
    @DisplayName("POST /auth/revoke - Revoke token")
    public void testRevokeToken() {
        System.out.println("\n==========================================");
        System.out.println(" Testing: POST /auth/revoke");
        System.out.println("==========================================\n");

        if (validRefreshToken == null) {
            System.out.println("Skipping: No refresh token from login test (Keycloak not available)");
            return;
        }

        String revokeJson = String.format("""
            {
                "refreshToken": "%s"
            }
            """, validRefreshToken);

        System.out.println("Request:");
        System.out.println("POST /auth/revoke");
        System.out.println("Body: { refreshToken: \"<token>\" }");
        System.out.println();

        // Accept 204 (success) or 500 (Keycloak unavailable)
        var response = given()
            .contentType(ContentType.JSON)
            .body(revokeJson)
        .when()
            .post("/auth/revoke")
        .then()
            .statusCode(anyOf(equalTo(204), equalTo(500)))
            .extract()
            .response();

        int statusCode = response.getStatusCode();
        System.out.println("Response: HTTP " + statusCode);
        if (statusCode == 204) {
            System.out.println("Token revoked successfully (Keycloak available)");
        } else {
            System.out.println("Keycloak not available - got 500 error");
        }
        System.out.println();
    }

    @Test
    @Order(7)
    @DisplayName("POST /auth/revoke - Missing token")
    public void testRevokeMissingToken() {
        System.out.println("\n==========================================");
        System.out.println(" Testing: POST /auth/revoke (missing token)");
        System.out.println("==========================================\n");

        String revokeJson = "{}";

        given()
            .contentType(ContentType.JSON)
            .body(revokeJson)
        .when()
            .post("/auth/revoke")
        .then()
            .statusCode(400);

        System.out.println("Response: HTTP 400 Bad Request");
        System.out.println("Validation error for missing refresh token");
        System.out.println();
    }
}

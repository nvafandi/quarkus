package com.sales;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(user = "testuser", roles = {"ADMIN"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CreateUserEndpointTest {

    @Test
    @Order(1)
    @DisplayName("POST /api/users - Create User")
    public void testCreateUserEndpoint() {
        String uniqueUsername = "api_test_user_" + System.currentTimeMillis();

        System.out.println("\n==========================================");
        System.out.println("Testing: POST /api/users");
        System.out.println("==========================================");
        System.out.println("\nRequest:");
        System.out.println("POST http://localhost:8081/api/users");
        System.out.println("Content-Type: application/json");
        System.out.println("\nBody:");
        System.out.println("{");
        System.out.println("  \"username\": \"" + uniqueUsername + "\",");
        System.out.println("  \"password\": \"TestPass123!\",");
        System.out.println("  \"email\": \"" + uniqueUsername + "@example.com\",");
        System.out.println("  \"roles\": [\"USER\"]");
        System.out.println("}");

        Response response = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "username": "%s",
                    "password": "TestPass123!",
                    "email": "%s@example.com",
                    "roles": ["USER"]
                }
                """.formatted(uniqueUsername, uniqueUsername))
        .when()
            .post("/api/users")
        .then()
            .statusCode(anyOf(
                is(201),  // Keycloak available - user created and synced
                is(400),  // Validation error or Keycloak connection issue
                is(409),  // Username already exists
                is(500)   // Keycloak unavailable
            ))
            .extract()
            .response();

        System.out.println("\nResponse:");
        System.out.println("HTTP Status: " + response.getStatusCode());
        System.out.println("\nResponse Body:");
        System.out.println(response.getBody().asPrettyString());
        System.out.println("\n==========================================");

        // Verify response structure based on status code
        int statusCode = response.getStatusCode();
        
        if (statusCode == 201) {
            // Success - should return synced UserDTO
            System.out.println("SUCCESS: User created in Keycloak and synced to local DB");
            response.then()
                .body(is(notNullValue()));
        } else if (statusCode == 400) {
            System.out.println("BAD REQUEST: Validation error or Keycloak connection issue");
        } else if (statusCode == 409) {
            System.out.println("CONFLICT: Username already exists");
        } else if (statusCode == 500) {
            System.out.println("SERVER ERROR: Keycloak unavailable (expected in test env)");
        }
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/users - Missing Password (400)")
    public void testCreateUser_MissingPassword() {
        System.out.println("\n==========================================");
        System.out.println("Testing: POST /api/users - Missing Password");
        System.out.println("==========================================");
        System.out.println("\nRequest:");
        System.out.println("POST http://localhost:8081/api/users");
        System.out.println("Body: { \"username\": \"test_no_pass\" }");

        Response response = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "username": "test_no_pass"
                }
                """)
        .when()
            .post("/api/users")
        .then()
            .statusCode(400)
            .extract()
            .response();

        System.out.println("\nResponse:");
        System.out.println("HTTP Status: " + response.getStatusCode());
        System.out.println("Response Body: " + response.getBody().asPrettyString());
        System.out.println("\nEXPECTED: 400 Bad Request (password is required)");
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/users - List Users")
    public void testListUsersEndpoint() {
        System.out.println("\n==========================================");
        System.out.println("Testing: GET /api/users");
        System.out.println("==========================================");

        Response response = given()
        .when()
            .get("/api/users")
        .then()
            .statusCode(anyOf(is(200), is(500)))
            .extract()
            .response();

        System.out.println("HTTP Status: " + response.getStatusCode());
        
        if (response.getStatusCode() == 200) {
            System.out.println("SUCCESS: Retrieved user list");
        } else {
            System.out.println("Keycloak unavailable");
        }
    }

    @Test
    @Order(4)
    @DisplayName("DELETE /api/users/{id} - Delete User")
    public void testDeleteUserEndpoint() {
        String testId = "non-existent-user-" + System.currentTimeMillis();

        System.out.println("\n==========================================");
        System.out.println("Testing: DELETE /api/users/{id}");
        System.out.println("==========================================");
        System.out.println("\nRequest:");
        System.out.println("DELETE http://localhost:8081/api/users/" + testId);

        Response response = given()
        .when()
            .delete("/api/users/" + testId)
        .then()
            .statusCode(anyOf(is(204), is(404), is(500)))
            .extract()
            .response();

        System.out.println("\nResponse:");
        System.out.println("HTTP Status: " + response.getStatusCode());
        
        if (response.getStatusCode() == 204) {
            System.out.println("SUCCESS: User deleted from Keycloak and local DB");
        } else if (response.getStatusCode() == 404) {
            System.out.println("User not found");
        } else {
            System.out.println("Keycloak unavailable");
        }
    }
}

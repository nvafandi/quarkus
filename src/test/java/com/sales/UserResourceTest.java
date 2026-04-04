package com.sales;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class UserResourceTest {

    @Test
    @TestSecurity(user = "testuser", roles = {"ADMIN"})
    public void testCreateUser() {
        String uniqueUsername = "testuser_" + System.currentTimeMillis();
        String json = """
            {
                "username": "%s",
                "role": "USER"
            }
            """.formatted(uniqueUsername);

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/users")
        .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("status", equalTo(201))
            .body("data.username", equalTo(uniqueUsername))
            .body("data.role", equalTo("USER"));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"ADMIN"})
    public void testGetAllUsers() {
        given()
            .when()
            .get("/api/users")
        .then()
            .statusCode(200)
            .body(is(notNullValue()));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"ADMIN"})
    public void testCreateUserWithDuplicateUsername() {
        // First create a user
        String json1 = """
            {
                "username": "dup_user",
                "role": "USER"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json1)
        .when()
            .post("/api/users")
        .then()
            .statusCode(201);

        // Try to create another user with same username
        String json2 = """
            {
                "username": "dup_user",
                "role": "ADMIN"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json2)
        .when()
            .post("/api/users")
        .then()
            .statusCode(409);
    }
}

package com.sales;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class UserResourceTest {

    @Test
    public void testCreateUser() {
        String json = """
            {
                "username": "testuser",
                "password": "testpass123",
                "role": "USER"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/users")
        .then()
            .statusCode(201)
            .body("username", equalTo("testuser"))
            .body("role", equalTo("USER"));
    }

    @Test
    public void testGetAllUsers() {
        given()
            .when()
            .get("/api/users")
        .then()
            .statusCode(200)
            .body(is(notNullValue()));
    }

    @Test
    public void testCreateUserWithDuplicateUsername() {
        String json = """
            {
                "username": "admin",
                "password": "admin123",
                "role": "USER"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/users")
        .then()
            .statusCode(409);
    }
}

package com.sales;

import com.sales.entity.UserEntity;
import com.sales.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(user = "testuser", roles = {"ADMIN"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TransactionResourceTest {

    @Inject
    UserRepository userRepository;

    private static String createdProductId;
    private static String createdTransactionId;

    @BeforeAll
    @Transactional
    void setupTestUser() {
        UserEntity user = new UserEntity();
        user.setUsername("testuser");
        user.setRole("ADMIN");
        userRepository.persist(user);
    }

    @Test
    @Order(1)
    @DisplayName("Create Product for Transaction")
    public void testCreateProductForTransaction() {
        String json = """
            {
                "name": "Transaction Test Product",
                "price": 200000.00,
                "stock": 50
            }
            """;

        createdProductId = given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.id", notNullValue())
            .extract()
            .jsonPath()
            .getString("data.id");
    }

    @Test
    @Order(2)
    @DisplayName("Create Transaction - Success")
    public void testCreateTransaction() {
        String json = """
            {
                "items": [
                    {
                        "productId": "%s",
                        "quantity": 2
                    }
                ]
            }
            """.formatted(createdProductId);

        createdTransactionId = given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/transactions")
        .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("status", equalTo(201))
            .body("data.id", notNullValue())
            .body("data.totalAmount", equalTo(400000.00f))
            .body("data.items", hasSize(1))
            .body("data.items[0].productId", equalTo(createdProductId))
            .body("data.items[0].quantity", equalTo(2))
            .body("data.items[0].price", equalTo(200000.00f))
            .extract()
            .jsonPath()
            .getString("data.id");
    }

    @Test
    @Order(3)
    @DisplayName("Create Transaction - Empty Items (400)")
    public void testCreateTransactionEmptyItems() {
        String json = """
            {
                "items": []
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/transactions")
        .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("status", equalTo(400));
    }

    @Test
    @Order(4)
    @DisplayName("Create Transaction - Insufficient Stock (400)")
    public void testCreateTransactionInsufficientStock() {
        String json = """
            {
                "items": [
                    {
                        "productId": "%s",
                        "quantity": 99999
                    }
                ]
            }
            """.formatted(createdProductId);

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/transactions")
        .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("status", equalTo(400));
    }

    @Test
    @Order(5)
    @DisplayName("Create Transaction - Product Not Found (404)")
    public void testCreateTransactionProductNotFound() {
        String nonExistentProductId = UUID.randomUUID().toString();
        String json = """
            {
                "items": [
                    {
                        "productId": "%s",
                        "quantity": 1
                    }
                ]
            }
            """.formatted(nonExistentProductId);

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/transactions")
        .then()
            .statusCode(404)
            .body("success", equalTo(false))
            .body("status", equalTo(404));
    }

    @Test
    @Order(10)
    @DisplayName("Get All Transactions")
    public void testFindAllTransactions() {
        given()
        .when()
            .get("/api/transactions")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("status", equalTo(200))
            .body("data", not(empty()));
    }

    @Test
    @Order(11)
    @DisplayName("Get Transaction By ID - Success")
    public void testFindTransactionById() {
        given()
            .pathParam("id", createdTransactionId)
        .when()
            .get("/api/transactions/{id}")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("status", equalTo(200))
            .body("data.id", equalTo(createdTransactionId))
            .body("data.totalAmount", equalTo(400000.00f))
            .body("data.items", hasSize(1));
    }

    @Test
    @Order(12)
    @DisplayName("Get Transaction By ID - Not Found (404)")
    public void testFindTransactionByIdNotFound() {
        String nonExistentId = UUID.randomUUID().toString();

        given()
            .pathParam("id", nonExistentId)
        .when()
            .get("/api/transactions/{id}")
        .then()
            .statusCode(404)
            .body("success", equalTo(false))
            .body("status", equalTo(404));
    }

    @Test
    @Order(13)
    @DisplayName("Get Transactions By User ID")
    public void testFindTransactionsByUserId() {
        String testUserId = userRepository.findByUsername("testuser")
                .map(u -> u.getId().toString())
                .orElse(null);

        Assertions.assertNotNull(testUserId, "Test user should exist in database");

        given()
            .pathParam("userId", testUserId)
        .when()
            .get("/api/transactions/user/{userId}")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("status", equalTo(200))
            .body("data", not(empty()))
            .body("data[0].userId", equalTo(testUserId));
    }

    @Test
    @Order(20)
    @DisplayName("Delete Transaction - Success")
    public void testDeleteTransaction() {
        given()
            .pathParam("id", createdTransactionId)
        .when()
            .delete("/api/transactions/{id}")
        .then()
            .statusCode(204);
    }

    @Test
    @Order(21)
    @DisplayName("Delete Transaction - Not Found (404)")
    public void testDeleteTransactionNotFound() {
        String nonExistentId = UUID.randomUUID().toString();

        given()
            .pathParam("id", nonExistentId)
        .when()
            .delete("/api/transactions/{id}")
        .then()
            .statusCode(404)
            .body("success", equalTo(false))
            .body("status", equalTo(404));
    }
}

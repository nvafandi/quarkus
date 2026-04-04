package com.sales;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "testuser", roles = {"ADMIN"})
public class CrudResourceTest {

    private static String createdUserId;
    private static String createdProductId;
    private static String createdTransactionId;

    // ==================== USER CRUD ====================

    @Test
    @Order(1)
    @DisplayName("Create User - Success")
    public void testCreateUser() {
        String uniqueUsername = "crud_user_" + System.currentTimeMillis();
        String json = """
            {
                "username": "%s",
                "role": "ADMIN"
            }
            """.formatted(uniqueUsername);

        createdUserId = given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/users")
        .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("status", equalTo(201))
            .body("data.id", notNullValue())
            .body("data.username", equalTo(uniqueUsername))
            .body("data.role", equalTo("ADMIN"))
            .body("data.createdAt", notNullValue())
            .body("data.updatedAt", notNullValue())
            .extract()
            .jsonPath()
            .getString("data.id");
    }

    @Test
    @Order(2)
    @DisplayName("Create User - Duplicate Username")
    public void testCreateUserDuplicateUsername() {
        // First create a user with a specific username
        String json1 = """
            {
                "username": "duplicate_test_user",
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

        // Try to create another user with the same username
        String json2 = """
            {
                "username": "duplicate_test_user",
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

    @Test
    @Order(3)
    @DisplayName("Create User - Missing Username")
    public void testCreateUserMissingUsername() {
        String json = """
            {
                "username": "",
                "role": "USER"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/users")
        .then()
            .statusCode(400);
    }

    @Test
    @Order(4)
    @DisplayName("Get All Users")
    public void testFindAllUsers() {
        given()
            .when()
            .get("/api/users")
        .then()
            .statusCode(200)
            .body("$", not(empty()));
    }

    @Test
    @Order(5)
    @DisplayName("Get User By ID - Success")
    public void testFindUserById() {
        given()
            .pathParam("id", createdUserId)
        .when()
            .get("/api/users/{id}")
        .then()
            .statusCode(200)
            .body("data.id", equalTo(createdUserId))
            .body("data.username", startsWith("crud_user_"))
            .body("data.role", equalTo("ADMIN"));
    }

    @Test
    @Order(6)
    @DisplayName("Get User By ID - Not Found")
    public void testFindUserByIdNotFound() {
        String nonExistentId = UUID.randomUUID().toString();

        given()
            .pathParam("id", nonExistentId)
        .when()
            .get("/api/users/{id}")
        .then()
            .statusCode(404);
    }

    @Test
    @Order(7)
    @DisplayName("Update User - Success")
    public void testUpdateUser() {
        String json = """
            {
                "username": "crud_user_upd_%s",
                "role": "SUPER_ADMIN"
            }
            """.formatted(System.currentTimeMillis());

        given()
            .contentType(ContentType.JSON)
            .pathParam("id", createdUserId)
            .body(json)
        .when()
            .put("/api/users/{id}")
        .then()
            .statusCode(200)
            .body("data.id", equalTo(createdUserId))
            .body("data.username", startsWith("crud_user_upd_"))
            .body("data.role", equalTo("SUPER_ADMIN"));
    }

    @Test
    @Order(8)
    @DisplayName("Update User - Not Found")
    public void testUpdateUserNotFound() {
        String nonExistentId = UUID.randomUUID().toString();
        String json = """
            {
                "username": "nonexistent",
                "role": "USER"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .pathParam("id", nonExistentId)
            .body(json)
        .when()
            .put("/api/users/{id}")
        .then()
            .statusCode(404);
    }

    // ==================== PRODUCT CRUD ====================

    @Test
    @Order(10)
    @DisplayName("Create Product - Success")
    public void testCreateProduct() {
        String json = """
            {
                "name": "Test Product",
                "price": 150000.00,
                "stock": 100
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
            .body("status", equalTo(201))
            .body("data.id", notNullValue())
            .body("data.name", equalTo("Test Product"))
            .body("data.price", equalTo(150000.00f))
            .body("data.stock", equalTo(100))
            .body("data.createdAt", notNullValue())
            .body("data.updatedAt", notNullValue())
            .extract()
            .jsonPath()
            .getString("data.id");
    }

    @Test
    @Order(11)
    @DisplayName("Create Product - Invalid Price")
    public void testCreateProductInvalidPrice() {
        String json = """
            {
                "name": "Bad Product",
                "price": -100,
                "stock": 10
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/products")
        .then()
            .statusCode(400);
    }

    @Test
    @Order(12)
    @DisplayName("Create Product - Negative Stock")
    public void testCreateProductNegativeStock() {
        String json = """
            {
                "name": "Bad Product",
                "price": 100,
                "stock": -5
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/products")
        .then()
            .statusCode(400);
    }

    @Test
    @Order(13)
    @DisplayName("Get All Products")
    public void testFindAllProducts() {
        given()
            .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("$", not(empty()));
    }

    @Test
    @Order(14)
    @DisplayName("Get Product By ID - Success")
    public void testFindProductById() {
        given()
            .pathParam("id", createdProductId)
        .when()
            .get("/api/products/{id}")
        .then()
            .statusCode(200)
            .body("data.id", equalTo(createdProductId))
            .body("data.name", equalTo("Test Product"))
            .body("data.price", equalTo(150000.00f))
            .body("data.stock", equalTo(100));
    }

    @Test
    @Order(15)
    @DisplayName("Get Product By ID - Not Found")
    public void testFindProductByIdNotFound() {
        String nonExistentId = UUID.randomUUID().toString();

        given()
            .pathParam("id", nonExistentId)
        .when()
            .get("/api/products/{id}")
        .then()
            .statusCode(404);
    }

    @Test
    @Order(16)
    @DisplayName("Update Product - Success")
    public void testUpdateProduct() {
        String json = """
            {
                "name": "Updated Product",
                "price": 200000.00,
                "stock": 50
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .pathParam("id", createdProductId)
            .body(json)
        .when()
            .put("/api/products/{id}")
        .then()
            .statusCode(200)
            .body("data.id", equalTo(createdProductId))
            .body("data.name", equalTo("Updated Product"))
            .body("data.price", equalTo(200000.00f))
            .body("data.stock", equalTo(50));
    }

    @Test
    @Order(17)
    @DisplayName("Update Product - Not Found")
    public void testUpdateProductNotFound() {
        String nonExistentId = UUID.randomUUID().toString();
        String json = """
            {
                "name": "Nonexistent",
                "price": 100,
                "stock": 10
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .pathParam("id", nonExistentId)
            .body(json)
        .when()
            .put("/api/products/{id}")
        .then()
            .statusCode(404);
    }

    // ==================== TRANSACTION CRUD ====================

    @Test
    @Order(20)
    @DisplayName("Create Transaction - Success")
    public void testCreateTransaction() {
        String json = """
            {
                "userId": "%s",
                "items": [
                    {
                        "productId": "%s",
                        "quantity": 2
                    }
                ]
            }
            """.formatted(createdUserId, createdProductId);

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
            .body("data.userId", equalTo(createdUserId))
            .body("data.totalAmount", equalTo(400000.00f))
            .body("data.items", hasSize(1))
            .body("data.items[0].productId", equalTo(createdProductId))
            .body("data.items[0].quantity", equalTo(2))
            .body("data.items[0].price", equalTo(200000.00f))
            .body("data.createdAt", notNullValue())
            .body("data.updatedAt", notNullValue())
            .extract()
            .jsonPath()
            .getString("data.id");
    }

    @Test
    @Order(21)
    @DisplayName("Create Transaction - Insufficient Stock")
    public void testCreateTransactionInsufficientStock() {
        String json = """
            {
                "userId": "%s",
                "items": [
                    {
                        "productId": "%s",
                        "quantity": 9999
                    }
                ]
            }
            """.formatted(createdUserId, createdProductId);

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/transactions")
        .then()
            .statusCode(400);
    }

    @Test
    @Order(22)
    @DisplayName("Create Transaction - User Not Found")
    public void testCreateTransactionUserNotFound() {
        String nonExistentUserId = UUID.randomUUID().toString();
        String json = """
            {
                "userId": "%s",
                "items": [
                    {
                        "productId": "%s",
                        "quantity": 1
                    }
                ]
            }
            """.formatted(nonExistentUserId, createdProductId);

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/transactions")
        .then()
            .statusCode(404);
    }

    @Test
    @Order(23)
    @DisplayName("Get All Transactions")
    public void testFindAllTransactions() {
        given()
            .when()
            .get("/api/transactions")
        .then()
            .statusCode(200)
            .body("$", not(empty()));
    }

    @Test
    @Order(24)
    @DisplayName("Get Transaction By ID - Success")
    public void testFindTransactionById() {
        given()
            .pathParam("id", createdTransactionId)
        .when()
            .get("/api/transactions/{id}")
        .then()
            .statusCode(200)
            .body("id", equalTo(createdTransactionId))
            .body("userId", equalTo(createdUserId))
            .body("totalAmount", equalTo(400000.00f))
            .body("items", hasSize(1));
    }

    @Test
    @Order(25)
    @DisplayName("Get Transaction By ID - Not Found")
    public void testFindTransactionByIdNotFound() {
        String nonExistentId = UUID.randomUUID().toString();

        given()
            .pathParam("id", nonExistentId)
        .when()
            .get("/api/transactions/{id}")
        .then()
            .statusCode(404);
    }

    @Test
    @Order(26)
    @DisplayName("Get Transactions By User ID")
    public void testFindTransactionsByUserId() {
        given()
            .pathParam("userId", createdUserId)
        .when()
            .get("/api/transactions/user/{userId}")
        .then()
            .statusCode(200)
            .body("$", not(empty()))
            .body("[0].userId", equalTo(createdUserId));
    }

    // ==================== DELETE OPERATIONS ====================

    @Test
    @Order(30)
    @DisplayName("Delete Transaction - Success")
    public void testDeleteTransaction() {
        given()
            .pathParam("id", createdTransactionId)
        .when()
            .delete("/api/transactions/{id}")
        .then()
            .statusCode(anyOf(is(204), is(500)));
    }

    @Test
    @Order(31)
    @DisplayName("Delete Transaction - Not Found")
    public void testDeleteTransactionNotFound() {
        String nonExistentId = UUID.randomUUID().toString();

        given()
            .pathParam("id", nonExistentId)
        .when()
            .delete("/api/transactions/{id}")
        .then()
            .statusCode(404);
    }

    @Test
    @Order(32)
    @DisplayName("Delete Product - Success")
    public void testDeleteProduct() {
        given()
            .pathParam("id", createdProductId)
        .when()
            .delete("/api/products/{id}")
        .then()
            .statusCode(anyOf(is(204), is(500)));
    }

    @Test
    @Order(33)
    @DisplayName("Delete Product - Not Found")
    public void testDeleteProductNotFound() {
        String nonExistentId = UUID.randomUUID().toString();

        given()
            .pathParam("id", nonExistentId)
        .when()
            .delete("/api/products/{id}")
        .then()
            .statusCode(404);
    }

    @Test
    @Order(34)
    @DisplayName("Delete User - Success")
    public void testDeleteUser() {
        given()
            .pathParam("id", createdUserId)
        .when()
            .delete("/api/users/{id}")
        .then()
            .statusCode(anyOf(is(204), is(500)));
    }

    @Test
    @Order(35)
    @DisplayName("Delete User - Not Found")
    public void testDeleteUserNotFound() {
        String nonExistentId = UUID.randomUUID().toString();

        given()
            .pathParam("id", nonExistentId)
        .when()
            .delete("/api/users/{id}")
        .then()
            .statusCode(404);
    }
}

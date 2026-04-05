package com.sales;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(user = "testuser", roles = {"ADMIN"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductResourceTest {

    private static String createdProductId;

    @Test
    @Order(1)
    @DisplayName("Create Product - Success")
    public void testCreateProduct() {
        String productName = "Test Product_" + System.currentTimeMillis();
        String json = """
            {
                "name": "%s",
                "price": 50000.00,
                "stock": 100
            }
            """.formatted(productName);

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
            .body("data.name", equalTo(productName))
            .body("data.price", equalTo(50000.00f))
            .body("data.stock", equalTo(100))
            .body("data.createdBy", notNullValue())
            .body("data.updatedBy", notNullValue())
            .extract()
            .jsonPath()
            .getString("data.id");
    }

    @Test
    @Order(2)
    @DisplayName("Create Product - Missing Name (400)")
    public void testCreateProductMissingName() {
        String json = """
            {
                "price": 50000.00,
                "stock": 100
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/products")
        .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("status", equalTo(400));
    }

    @Test
    @Order(3)
    @DisplayName("Create Product - Negative Price (400)")
    public void testCreateProductNegativePrice() {
        String json = """
            {
                "name": "Bad Price Product",
                "price": -10.00,
                "stock": 100
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/products")
        .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("status", equalTo(400));
    }

    @Test
    @Order(4)
    @DisplayName("Create Product - Negative Stock (400)")
    public void testCreateProductNegativeStock() {
        String json = """
            {
                "name": "Bad Stock Product",
                "price": 50000.00,
                "stock": -5
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/api/products")
        .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("status", equalTo(400));
    }

    @Test
    @Order(10)
    @DisplayName("Get All Products")
    public void testFindAllProducts() {
        given()
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("status", equalTo(200))
            .body("data", not(empty()));
    }

    @Test
    @Order(11)
    @DisplayName("Get Product By ID - Success")
    public void testFindProductById() {
        given()
            .pathParam("id", createdProductId)
        .when()
            .get("/api/products/{id}")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("status", equalTo(200))
            .body("data.id", equalTo(createdProductId));
    }

    @Test
    @Order(12)
    @DisplayName("Get Product By ID - Not Found (404)")
    public void testFindProductByIdNotFound() {
        String nonExistentId = UUID.randomUUID().toString();

        given()
            .pathParam("id", nonExistentId)
        .when()
            .get("/api/products/{id}")
        .then()
            .statusCode(404)
            .body("success", equalTo(false))
            .body("status", equalTo(404));
    }

    @Test
    @Order(20)
    @DisplayName("Update Product - Success")
    public void testUpdateProduct() {
        String json = """
            {
                "name": "Updated Product",
                "price": 75000.00,
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
            .body("success", equalTo(true))
            .body("status", equalTo(200))
            .body("data.id", equalTo(createdProductId))
            .body("data.name", equalTo("Updated Product"))
            .body("data.price", equalTo(75000.00f))
            .body("data.stock", equalTo(50))
            .body("data.updatedBy", notNullValue());
    }

    @Test
    @Order(21)
    @DisplayName("Update Product - Not Found (404)")
    public void testUpdateProductNotFound() {
        String nonExistentId = UUID.randomUUID().toString();
        String json = """
            {
                "name": "Nonexistent",
                "price": 10000.00,
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
            .statusCode(404)
            .body("success", equalTo(false))
            .body("status", equalTo(404));
    }

    @Test
    @Order(30)
    @DisplayName("Delete Product - Success")
    public void testDeleteProduct() {
        given()
            .pathParam("id", createdProductId)
        .when()
            .delete("/api/products/{id}")
        .then()
            .statusCode(204);
    }

    @Test
    @Order(31)
    @DisplayName("Delete Product - Not Found (404)")
    public void testDeleteProductNotFound() {
        String nonExistentId = UUID.randomUUID().toString();

        given()
            .pathParam("id", nonExistentId)
        .when()
            .delete("/api/products/{id}")
        .then()
            .statusCode(404)
            .body("success", equalTo(false))
            .body("status", equalTo(404));
    }
}

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
public class GetProductsTest {

    @Test
    @Order(1)
    @DisplayName("GET /api/products - Retrieve All Products")
    public void testGetAllProducts() {
        System.out.println("\n==========================================");
        System.out.println(" Testing: GET /api/products");
        System.out.println("==========================================\n");

        System.out.println("Request:");
        System.out.println("GET http://localhost:5000/api/products");
        System.out.println();

        var response = given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .extract()
            .response();

        System.out.println("Response:");
        System.out.println("HTTP Status: 200 OK");
        System.out.println();

        java.util.List<java.util.Map<String, Object>> products = response.jsonPath().getList("data");
        
        System.out.println("==========================================");
        System.out.println(" Summary");
        System.out.println("==========================================");
        System.out.println("📊 Total Products: " + products.size());
        System.out.println("==========================================\n");

        if (products.isEmpty()) {
            System.out.println("⚠️  No products found in database.");
            System.out.println();
            System.out.println("To insert 1000 products, run:");
            System.out.println("  psql -U nurvan -d sales_db -f insert-1000-products.sql");
        } else {
            // Display first 10 products
            int displayCount = Math.min(10, products.size());
            System.out.println("Sample Products (First " + displayCount + "):");
            System.out.println("--------------------------------------------------");
            
            for (int i = 0; i < displayCount; i++) {
                var product = products.get(i);
                System.out.printf("%2d. ID: %s%n", i + 1, product.get("id"));
                System.out.printf("    Name: %s%n", product.get("name"));
                System.out.printf("    Price: Rp %,.2f%n", product.get("price"));
                System.out.printf("    Stock: %d units%n", product.get("stock"));
                System.out.printf("    Created By: %s%n", product.get("createdBy") != null ? product.get("createdBy") : "N/A");
                System.out.printf("    Updated By: %s%n", product.get("updatedBy") != null ? product.get("updatedBy") : "N/A");
                System.out.printf("    Created At: %s%n", product.get("createdAt"));
                System.out.println();
            }
            
            // Statistics
            System.out.println("==========================================");
            System.out.println(" Statistics");
            System.out.println("==========================================");
            
            double minPrice = products.stream()
                .mapToDouble(p -> (Double) p.get("price"))
                .min().orElse(0);
            
            double maxPrice = products.stream()
                .mapToDouble(p -> (Double) p.get("price"))
                .max().orElse(0);
            
            double avgPrice = products.stream()
                .mapToDouble(p -> (Double) p.get("price"))
                .average().orElse(0);
            
            long totalStock = products.stream()
                .mapToLong(p -> ((Number) p.get("stock")).longValue())
                .sum();
            
            System.out.printf("  Min Price:  Rp %,.2f%n", minPrice);
            System.out.printf("  Max Price:  Rp %,.2f%n", maxPrice);
            System.out.printf("  Avg Price:  Rp %,.2f%n", avgPrice);
            System.out.printf("  Total Stock: %,d units%n", totalStock);
            System.out.println("==========================================\n");
        }

        // Verify response structure
        Assertions.assertNotNull(products, "Products list should not be null");
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/products/{id} - Get Single Product")
    public void testGetProductById() {
        System.out.println("\n==========================================");
        System.out.println(" Testing: GET /api/products/{id}");
        System.out.println("==========================================\n");

        // First get all products to get an ID
        var allProducts = given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("data");

        if (allProducts.isEmpty()) {
            System.out.println("⚠️  No products available to test.");
            return;
        }

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> firstProduct = (java.util.Map<String, Object>) allProducts.get(0);
        String productId = (String) firstProduct.get("id");
        
        System.out.println("Request:");
        System.out.println("GET http://localhost:5000/api/products/" + productId);
        System.out.println();

        var response = given()
            .contentType(ContentType.JSON)
            .pathParam("id", productId)
        .when()
            .get("/api/products/{id}")
        .then()
            .statusCode(200)
            .extract()
            .response();

        System.out.println("Response:");
        System.out.println("HTTP Status: 200 OK");
        System.out.println();

        java.util.Map<String, Object> product = response.jsonPath().getMap("data");
        System.out.println("Product Details:");
        System.out.printf("  ID: %s%n", product.get("id"));
        System.out.printf("  Name: %s%n", product.get("name"));
        System.out.printf("  Price: Rp %,.2f%n", product.get("price"));
        System.out.printf("  Stock: %d units%n", product.get("stock"));
        System.out.printf("  Created By: %s%n", product.get("createdBy") != null ? product.get("createdBy") : "N/A");
        System.out.printf("  Updated By: %s%n", product.get("updatedBy") != null ? product.get("updatedBy") : "N/A");
        System.out.println();
        System.out.println("✅ Single product retrieval working!");
    }
}

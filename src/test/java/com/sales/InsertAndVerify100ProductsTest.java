package com.sales;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.Random;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(user = "testuser", roles = {"ADMIN"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InsertAndVerify100ProductsTest {

    private static final Random RANDOM = new Random();
    private static final String[] PRODUCT_NAMES = {
        "Laptop", "Phone", "Tablet", "Monitor", "Keyboard", "Mouse", "Headphones",
        "Speaker", "Camera", "Printer", "Scanner", "Router", "SSD", "RAM", "CPU",
        "GPU", "Motherboard", "Power Supply", "Case", "Cooling Fan", "Webcam",
        "Microphone", "Projector", "UPS", "Cable", "Adapter", "Battery", "Charger",
        "Smartwatch", "Earbuds", "Router", "Switch", "Hub", "Dock", "Stand",
        "Backpack", "Bag", "Sleeve", "Screen Protector", "Cleaning Kit"
    };

    @Test
    @Order(1)
    @DisplayName("Insert 100 Products via API")
    public void testInsert100Products() {
        System.out.println("\n==========================================");
        System.out.println(" Inserting 100 Products via API");
        System.out.println("==========================================\n");

        int successCount = 0;

        for (int i = 1; i <= 100; i++) {
            String name = PRODUCT_NAMES[RANDOM.nextInt(PRODUCT_NAMES.length)] + " " + 
                         generateModelNumber() + " - " + i;
            BigDecimal price = BigDecimal.valueOf(RANDOM.nextDouble() * 9900000 + 100000)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            int stock = RANDOM.nextInt(500) + 10;

            String json = String.format(
                "{\"name\": \"%s\", \"price\": %s, \"stock\": %d}",
                name, price, stock
            );

            var response = given()
                .contentType(ContentType.JSON)
                .body(json)
            .when()
                .post("/api/products")
            .then()
                .statusCode(201)
                .extract()
                .response();

            successCount++;
            
            if (i % 20 == 0 || i <= 5) {
                var jsonPath = response.jsonPath();
                System.out.printf("[%3d/100] ✅ Created: %-35s | Price: %10s | createdBy: %s%n",
                        i, 
                        name.length() > 35 ? name.substring(0, 32) + "..." : name,
                        price,
                        jsonPath.getString("data.createdBy"));
            }
        }

        System.out.println("\n==========================================");
        System.out.println(" ✅ Successfully inserted 100 products!");
        System.out.println("==========================================\n");

        Assertions.assertEquals(100, successCount, "Should insert 100 products");
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/products - Verify All 100 Products")
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

        java.util.List<java.util.Map<String, Object>> products = response.jsonPath().getList("data");
        
        System.out.println("Response: HTTP 200 OK");
        System.out.println();
        System.out.println("==========================================");
        System.out.println(" Product List Verification");
        System.out.println("==========================================");
        System.out.println("📊 Total Products in Database: " + products.size());
        System.out.println("==========================================\n");

        Assertions.assertEquals(100, products.size(), "Should have exactly 100 products");

        // Display first 10 products
        System.out.println("Sample Products (First 10):");
        System.out.println("--------------------------------------------------");
        
        for (int i = 0; i < Math.min(10, products.size()); i++) {
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
            .mapToDouble(p -> ((Number) p.get("price")).doubleValue())
            .min().orElse(0);
        
        double maxPrice = products.stream()
            .mapToDouble(p -> ((Number) p.get("price")).doubleValue())
            .max().orElse(0);
        
        double avgPrice = products.stream()
            .mapToDouble(p -> ((Number) p.get("price")).doubleValue())
            .average().orElse(0);
        
        long totalStock = products.stream()
            .mapToLong(p -> ((Number) p.get("stock")).longValue())
            .sum();
        
        long withCreatedBy = products.stream()
            .filter(p -> p.get("createdBy") != null)
            .count();
        
        System.out.printf("  Min Price:     Rp %,.2f%n", minPrice);
        System.out.printf("  Max Price:     Rp %,.2f%n", maxPrice);
        System.out.printf("  Avg Price:     Rp %,.2f%n", avgPrice);
        System.out.printf("  Total Stock:   %,d units%n", totalStock);
        System.out.printf("  With Creator:  %,d / %d products%n", withCreatedBy, products.size());
        System.out.println("==========================================\n");

        // Verify all products have required fields
        for (int i = 0; i < products.size(); i++) {
            var product = products.get(i);
            Assertions.assertNotNull(product.get("id"), "Product " + (i+1) + " should have ID");
            Assertions.assertNotNull(product.get("name"), "Product " + (i+1) + " should have name");
            Assertions.assertNotNull(product.get("price"), "Product " + (i+1) + " should have price");
            Assertions.assertNotNull(product.get("stock"), "Product " + (i+1) + " should have stock");
        }
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/products/{id} - Verify Single Product")
    public void testGetSingleProduct() {
        System.out.println("\n==========================================");
        System.out.println(" Testing: GET /api/products/{id}");
        System.out.println("==========================================\n");

        // Get first product ID
        var allProducts = given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("data");

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
            .body("data.id", equalTo(productId))
            .extract()
            .response();

        java.util.Map<String, Object> product = response.jsonPath().getMap("data");
        
        System.out.println("Response: HTTP 200 OK");
        System.out.println();
        System.out.println("Product Details:");
        System.out.printf("  ID: %s%n", product.get("id"));
        System.out.printf("  Name: %s%n", product.get("name"));
        System.out.printf("  Price: Rp %,.2f%n", product.get("price"));
        System.out.printf("  Stock: %d units%n", product.get("stock"));
        System.out.printf("  Created By: %s%n", product.get("createdBy") != null ? product.get("createdBy") : "N/A");
        System.out.printf("  Updated By: %s%n", product.get("updatedBy") != null ? product.get("updatedBy") : "N/A");
        System.out.println();
        System.out.println("✅ Single product retrieval verified!");
    }

    private String generateModelNumber() {
        String[] prefixes = {"Pro", "Plus", "Max", "Ultra", "Lite", "Air", "Mini", "Elite"};
        return prefixes[RANDOM.nextInt(prefixes.length)] + " " + 
               (RANDOM.nextInt(900) + 100);
    }
}

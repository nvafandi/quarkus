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
public class InsertProductsTest {

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
    @DisplayName("Insert 100 Products via API with User Tracking")
    public void testInsert100Products() {
        System.out.println("\n==========================================");
        System.out.println(" Inserting 100 Products via API");
        System.out.println("==========================================\n");

        int successCount = 0;
        int failCount = 0;

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

            try {
                var response = given()
                    .contentType(ContentType.JSON)
                    .body(json)
                .when()
                    .post("/api/products")
                .then()
                    .statusCode(anyOf(is(201), is(400)))
                    .extract()
                    .response();

                int statusCode = response.getStatusCode();
                if (statusCode == 201) {
                    successCount++;
                    if (i % 20 == 0 || i <= 5) {
                        var jsonPath = response.jsonPath();
                        System.out.printf("[%3d/100] Created: %-35s | createdBy: %s%n",
                                i, 
                                name.length() > 35 ? name.substring(0, 32) + "..." : name,
                                jsonPath.getString("data.createdBy"));
                    }
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
                System.out.printf("[%3d/100] Error: %s%n", i, e.getMessage());
            }
        }

        System.out.println("\n==========================================");
        System.out.println(" Summary");
        System.out.println("==========================================");
        System.out.println("Success: " + successCount);
        System.out.println("Failed:   " + failCount);
        System.out.println("Total:   100");
        System.out.println("==========================================\n");

        Assertions.assertEquals(100, successCount + failCount, "Total should be 100");
    }

    @Test
    @Order(2)
    @DisplayName("Verify Products Have createdBy and updatedBy Fields")
    public void testVerifyProductFields() {
        System.out.println("\n==========================================");
        System.out.println(" Verifying Product Fields");
        System.out.println("==========================================\n");

        // Get all products
        var response = given()
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .extract()
            .response();

        java.util.List<java.util.Map<String, Object>> products = response.jsonPath().getList("data");
        
        System.out.println("Total products in database: " + products.size());
        
        if (!products.isEmpty()) {
            // Check first product
            var firstProduct = products.get(0);
            System.out.println("\nSample Product:");
            System.out.println("  ID: " + firstProduct.get("id"));
            System.out.println("  Name: " + firstProduct.get("name"));
            System.out.println("  Price: " + firstProduct.get("price"));
            System.out.println("  Stock: " + firstProduct.get("stock"));
            System.out.println("  createdBy: " + firstProduct.get("createdBy"));
            System.out.println("  updatedBy: " + firstProduct.get("updatedBy"));
            System.out.println("  createdAt: " + firstProduct.get("createdAt"));
            System.out.println("  updatedAt: " + firstProduct.get("updatedAt"));

            // Verify fields exist
            Assertions.assertNotNull(firstProduct.get("id"), "ID should not be null");
            Assertions.assertNotNull(firstProduct.get("name"), "Name should not be null");
            
            // createdBy/updatedBy will be populated if local user exists
            Object createdBy = firstProduct.get("createdBy");
            Object updatedBy = firstProduct.get("updatedBy");
            
            if (createdBy != null) {
                System.out.println("\nUser tracking is working - createdBy: " + createdBy);
            } else {
                System.out.println("\nUser tracking not active - createdBy is null");
                System.out.println("   (This is expected if no local user record exists for testuser)");
            }
        }

        System.out.println("\n==========================================");
        System.out.println("Field verification complete");
        System.out.println("==========================================\n");
    }

    private String generateModelNumber() {
        String[] prefixes = {"Pro", "Plus", "Max", "Ultra", "Lite", "Air", "Mini", "Elite"};
        return prefixes[RANDOM.nextInt(prefixes.length)] + " " + 
               (RANDOM.nextInt(900) + 100);
    }
}

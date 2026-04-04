package com.sales;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for entity ID generation using UUIDv7.
 */
@QuarkusTest
@TestSecurity(user = "testuser", roles = {"ADMIN"})
public class EntityIdGenerationTest {

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void testEntityIdGeneration_UserEntity() {
        com.sales.entity.UserEntity user = new com.sales.entity.UserEntity();
        user.setUsername("test_uuid_user_" + System.currentTimeMillis());
        user.setRole("TESTER");
        
        entityManager.persist(user);
        entityManager.flush();
        
        assertNotNull(user.getId(), "User entity ID should be generated");
        assertEquals(7, user.getId().version(), "User entity ID should be UUID v7");
        assertEquals(2, user.getId().variant(), "User entity ID should have correct variant");
    }

    @Test
    @Transactional
    public void testEntityIdGeneration_ProductEntity() {
        com.sales.entity.ProductEntity product = new com.sales.entity.ProductEntity();
        product.setName("Test UUID Product " + System.currentTimeMillis());
        product.setPrice(BigDecimal.valueOf(100000));
        product.setStock(10);
        
        entityManager.persist(product);
        entityManager.flush();
        
        assertNotNull(product.getId(), "Product entity ID should be generated");
        assertEquals(7, product.getId().version(), "Product entity ID should be UUID v7");
    }

    @Test
    @Transactional
    public void testEntityIdGeneration_TransactionEntity() {
        com.sales.entity.UserEntity user = new com.sales.entity.UserEntity();
        user.setUsername("test_txn_user_" + System.currentTimeMillis());
        user.setRole("TESTER");
        entityManager.persist(user);
        
        com.sales.entity.TransactionEntity transaction = new com.sales.entity.TransactionEntity();
        transaction.setUser(user);
        transaction.setTotalAmount(BigDecimal.valueOf(50000));
        
        entityManager.persist(transaction);
        entityManager.flush();
        
        assertNotNull(transaction.getId(), "Transaction entity ID should be generated");
        assertEquals(7, transaction.getId().version(), "Transaction entity ID should be UUID v7");
    }

    @Test
    @Transactional
    public void testEntityIdGeneration_TransactionItemEntity() {
        com.sales.entity.UserEntity user = new com.sales.entity.UserEntity();
        user.setUsername("test_item_user_" + System.currentTimeMillis());
        user.setRole("TESTER");
        entityManager.persist(user);
        
        com.sales.entity.ProductEntity product = new com.sales.entity.ProductEntity();
        product.setName("Test Item Product " + System.currentTimeMillis());
        product.setPrice(BigDecimal.valueOf(50000));
        product.setStock(100);
        entityManager.persist(product);
        
        com.sales.entity.TransactionEntity transaction = new com.sales.entity.TransactionEntity();
        transaction.setUser(user);
        transaction.setTotalAmount(BigDecimal.valueOf(100000));
        entityManager.persist(transaction);
        
        com.sales.entity.TransactionItemEntity item = new com.sales.entity.TransactionItemEntity();
        item.setTransaction(transaction);
        item.setProduct(product);
        item.setQuantity(2);
        item.setPrice(BigDecimal.valueOf(50000));
        
        entityManager.persist(item);
        entityManager.flush();
        
        assertNotNull(item.getId(), "Transaction item entity ID should be generated");
        assertEquals(7, item.getId().version(), "Transaction item entity ID should be UUID v7");
    }
}

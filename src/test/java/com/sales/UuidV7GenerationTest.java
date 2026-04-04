package com.sales;

import org.junit.jupiter.api.Test;
import xyz.block.uuidv7.UUIDv7;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UUIDv7 generation using xyz.block:uuidv7 library.
 * These tests do not require a database connection.
 */
public class UuidV7GenerationTest {

    @Test
    public void testUuidV7Generation() {
        UUID uuid = UUIDv7.generate();
        
        assertNotNull(uuid, "Generated UUID should not be null");
        assertEquals(7, uuid.version(), "UUID version should be 7");
        assertEquals(2, uuid.variant(), "UUID variant should be 2 (RFC 4122)");
    }

    @Test
    public void testUuidV7Uniqueness() {
        UUID uuid1 = UUIDv7.generate();
        UUID uuid2 = UUIDv7.generate();
        
        assertNotEquals(uuid1, uuid2, "Generated UUIDs should be unique");
    }

    @Test
    public void testUuidV7TimestampOrdering() throws InterruptedException {
        UUID uuid1 = UUIDv7.generate();
        Thread.sleep(10);
        UUID uuid2 = UUIDv7.generate();
        
        // UUIDv7 should be time-ordered, so uuid2 > uuid1
        assertTrue(uuid2.compareTo(uuid1) > 0, 
            "UUIDv7 generated later should have a greater value (time-ordered)");
    }

    @Test
    public void testUuidV7TimestampExtraction() {
        UUID uuid = UUIDv7.generate();
        long timestamp = UUIDv7.getTimestamp(uuid);
        
        long now = System.currentTimeMillis();
        long diff = Math.abs(now - timestamp);
        
        assertTrue(diff < 1000, 
            "Extracted timestamp should be close to current time (within 1 second)");
    }

    @Test
    public void testUuidV7Format() {
        UUID uuid = UUIDv7.generate();
        String uuidString = uuid.toString();
        
        // UUID v7 format: xxxxxxxx-xxxx-7xxx-xxxx-xxxxxxxxxxxx
        assertTrue(uuidString.matches("^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"),
            "UUID v7 should match the correct format with version digit 7");
    }

    @Test
    public void testUuidV7BulkGeneration() {
        // Generate 1000 UUIDs and verify all are unique and valid v7
        java.util.Set<UUID> uuids = new java.util.HashSet<>();
        for (int i = 0; i < 1000; i++) {
            UUID uuid = UUIDv7.generate();
            assertEquals(7, uuid.version(), "All UUIDs should be version 7");
            assertEquals(2, uuid.variant(), "All UUIDs should have variant 2");
            assertTrue(uuids.add(uuid), "All UUIDs should be unique");
        }
    }
}

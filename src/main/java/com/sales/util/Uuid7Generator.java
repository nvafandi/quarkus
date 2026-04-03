package com.sales.util;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerator;
import java.util.UUID;

/**
 * Custom UUID v7 generator for Hibernate.
 * UUID v7 provides time-ordered, sortable identifiers.
 */
public class Uuid7Generator extends UUIDGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        return generateUuidV7();
    }

    /**
     * Generate a UUID v7 (time-ordered).
     * Format: timestamp_ms (48 bits) | version (4 bits) | random (12 bits) | variant (2 bits) | random (62 bits)
     */
    public static UUID generateUuidV7() {
        long timestamp = System.currentTimeMillis();
        
        // Most significant bits: 48-bit timestamp + 4-bit version (0111) + 12-bit random
        long msb = (timestamp << 16) | 0x7000L | (System.nanoTime() & 0x0FFFL);
        
        // Least significant bits: 2-bit variant (10) + 62-bit random
        long lsb = 0x8000000000000000L | (System.nanoTime() & 0x0FFFFFFFFFFFFFFFL);
        
        return new UUID(msb, lsb);
    }
}

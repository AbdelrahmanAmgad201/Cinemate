package org.example.backend.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UuidV7Test {

    // -------------------------------------------------------------------------
    // TEST: Version nibble must be 0x7
    // -------------------------------------------------------------------------
    @Test
    void generate_ReturnsVersion7Uuid() {
        UUID uuid = UuidV7.generate();

        // The version nibble lives in bits 12-15 of the MSB (UUID#version()).
        assertEquals(7, uuid.version(),
                "Expected UUID version 7, got " + uuid.version());
    }

    // -------------------------------------------------------------------------
    // TEST: Variant bits must be 10xx (IETF RFC 4122 / 9562 variant)
    // -------------------------------------------------------------------------
    @Test
    void generate_HasCorrectIetfVariantBits() {
        UUID uuid = UuidV7.generate();

        // UUID#variant() returns 2 for the IETF variant (bits 10xx → decimal 2).
        assertEquals(2, uuid.variant(),
                "Expected IETF variant (2), got " + uuid.variant());
    }

    // -------------------------------------------------------------------------
    // TEST: Two UUIDs separated by a small sleep preserve MSB time-ordering
    // -------------------------------------------------------------------------
    @Test
    void generate_IsTimeOrdered() throws InterruptedException {
        UUID first = UuidV7.generate();
        // Sleep long enough for the millisecond timestamp to advance.
        Thread.sleep(2);
        UUID second = UuidV7.generate();

        // The upper 48 bits of the MSB hold the Unix-millisecond timestamp,
        // so a strictly later UUID must have an MSB >= the earlier one.
        assertTrue(Long.compareUnsigned(second.getMostSignificantBits(),
                                         first.getMostSignificantBits()) >= 0,
                "Second UUID's MSB should be >= first UUID's MSB (time-ordered)");
    }

    // -------------------------------------------------------------------------
    // TEST: 1 000 generated UUIDs are all unique
    // -------------------------------------------------------------------------
    @Test
    void generate_AllUniqueInBatch() {
        final int BATCH_SIZE = 1_000;
        Set<UUID> generated = new HashSet<>(BATCH_SIZE);

        for (int i = 0; i < BATCH_SIZE; i++) {
            generated.add(UuidV7.generate());
        }

        assertEquals(BATCH_SIZE, generated.size(),
                "Expected all " + BATCH_SIZE + " UUIDs to be unique");
    }
}

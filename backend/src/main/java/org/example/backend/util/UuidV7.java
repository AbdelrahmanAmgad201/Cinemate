package org.example.backend.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Generates UUIDv7 identifiers application-side (RFC 9562): a 48-bit Unix-millis
 * timestamp prefix + 74 random bits. Time-ordered like the MongoDB {@code ObjectId}s
 * these replace, so they index well (no B-tree hot-spotting) while staying globally
 * unique and non-enumerable.
 *
 * <p>Generated in Java rather than via Postgres 18's native {@code uuidv7()} so the
 * schema runs on the pinned {@code postgres:16} image and JPA knows the id before insert.
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    public static UUID generate() {
        long ts = Instant.now().toEpochMilli();
        byte[] value = new byte[16];

        // 48-bit big-endian timestamp
        value[0] = (byte) (ts >>> 40);
        value[1] = (byte) (ts >>> 32);
        value[2] = (byte) (ts >>> 24);
        value[3] = (byte) (ts >>> 16);
        value[4] = (byte) (ts >>> 8);
        value[5] = (byte) ts;

        byte[] rand = new byte[10];
        RANDOM.nextBytes(rand);
        System.arraycopy(rand, 0, value, 6, 10);

        value[6] = (byte) ((value[6] & 0x0F) | 0x70); // version 7
        value[8] = (byte) ((value[8] & 0x3F) | 0x80); // IETF variant

        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (value[i] & 0xFF);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (value[i] & 0xFF);
        }
        return new UUID(msb, lsb);
    }
}

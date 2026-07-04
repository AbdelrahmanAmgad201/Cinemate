package org.example.backend.util;

import org.bson.types.ObjectId;

import java.math.BigInteger;

/**
 * Converts between the MySQL {@code Long} account IDs and the MongoDB {@code ObjectId}s
 * used to reference those accounts from Mongo documents. Was previously duplicated as a
 * private method in six different services (ARC-07).
 */
public final class IdConverter {

    private IdConverter() {
    }

    public static ObjectId longToObjectId(Long value) {
        return new ObjectId(String.format("%024x", value));
    }

    public static Long objectIdToLong(ObjectId objectId) {
        String hex = objectId.toHexString();
        return new BigInteger(hex, 16).longValue();
    }
}

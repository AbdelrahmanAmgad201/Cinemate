package org.example.backend;

import org.testcontainers.containers.MongoDBContainer;

public class MongoDBTestContainer {

    private static final MongoDBContainer MONGODB_CONTAINER;

    static {
        MONGODB_CONTAINER = new MongoDBContainer("mongo:6.0")
                .withExposedPorts(27017)
                .withReuse(true);
        MONGODB_CONTAINER.start();
    }

    public static MongoDBContainer getInstance() {
        return MONGODB_CONTAINER;
    }

    public static String getReplicaSetUrl() {
        return MONGODB_CONTAINER.getReplicaSetUrl();
    }
}
package org.example.backend;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class RedisTestContainer {

    private static final int REDIS_PORT = 6379;
    private static final GenericContainer<?> REDIS_CONTAINER;

    static {
        REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7"))
                .withExposedPorts(REDIS_PORT)
                .withReuse(true);
        REDIS_CONTAINER.start();
    }

    public static String getHost() {
        return REDIS_CONTAINER.getHost();
    }

    public static Integer getPort() {
        return REDIS_CONTAINER.getMappedPort(REDIS_PORT);
    }
}

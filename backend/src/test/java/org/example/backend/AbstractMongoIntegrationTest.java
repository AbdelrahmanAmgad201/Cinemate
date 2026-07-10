package org.example.backend;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
public abstract class AbstractMongoIntegrationTest extends AbstractMySQLIntegrationTest {

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MongoDBTestContainer::getReplicaSetUrl);
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @AfterEach
    void cleanupDatabase() {
        mongoTemplate.getDb().listCollectionNames().forEach(collectionName -> {
            mongoTemplate.getDb().getCollection(collectionName).drop();
        });
    }
}
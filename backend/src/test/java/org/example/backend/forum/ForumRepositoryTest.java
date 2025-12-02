package org.example.backend.forum;

import org.bson.types.ObjectId;
import org.example.backend.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

//@DataMongoTest
class ForumRepositoryTest extends AbstractMongoIntegrationTest {

    @Autowired
    private ForumRepository forumRepository;

    @Test
    void shouldCreateAndRetrieveForum() {
        // Arrange
        Forum forum = Forum.builder()
                .id(new ObjectId())
                .name("Test Forum")
                .description("A forum for testing")
                .ownerId(new ObjectId())
                .createdAt(Instant.now())
                .build();

        forumRepository.save(forum);
        Forum saved = forumRepository.findById(forum.getId()).orElse(null);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test Forum");
        assertThat(saved.getDescription()).isEqualTo("A forum for testing");
        assertThat(saved.getOwnerId()).isEqualTo(forum.getOwnerId());
        assertThat(saved.getFollowerCount()).isEqualTo(0);
        assertThat(saved.getPostCount()).isEqualTo(0);
        assertThat(saved.getIsDeleted()).isFalse();
    }
}

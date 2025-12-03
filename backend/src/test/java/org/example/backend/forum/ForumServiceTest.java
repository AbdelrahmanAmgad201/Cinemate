package org.example.backend.forum;

import org.bson.types.ObjectId;
import org.example.backend.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class ForumServiceTest extends AbstractMongoIntegrationTest {

    @Autowired
    private ForumService forumService;

    @Autowired
    private ForumRepository forumRepository;

    // Helper that matches your service logic
    private ObjectId longToObjectId(Long value) {
        return new ObjectId(String.format("%024x", value));
    }

    @Test
    void testCreateForum() {

        ForumCreationRequest request = new ForumCreationRequest();
        request.setName("Tech Forum");
        request.setDescription("A place to talk about tech");

        Long userId = 123L;

        ObjectId expectedOwnerId = longToObjectId(userId);
        Forum forum = forumService.createForum(request, userId);


        assertThat(forum).isNotNull();
        assertThat(forum.getId()).isNotNull();
        assertThat(forum.getName()).isEqualTo("Tech Forum");
        assertThat(forum.getDescription()).isEqualTo("A place to talk about tech");

        assertThat(forum.getOwnerId()).isEqualTo(expectedOwnerId);

        assertThat(forum.getFollowerCount()).isZero();
        assertThat(forum.getPostCount()).isZero();
        assertThat(forum.getIsDeleted()).isFalse();
        assertThat(forum.getDeletedAt()).isNull();

        assertThat(forum.getCreatedAt()).isNotNull();

        Forum saved = forumRepository.findById(forum.getId()).orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getName()).isEqualTo("Tech Forum");
        assertThat(saved.getOwnerId()).isEqualTo(expectedOwnerId);
    }
}

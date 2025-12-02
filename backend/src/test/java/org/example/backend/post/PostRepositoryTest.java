package org.example.backend.post;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ActiveProfiles("test")
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    void shouldCreateAndRetrievePost() {
        // Arrange
        ObjectId forumId = new ObjectId();
        ObjectId ownerId = new ObjectId();

        Post post = Post.builder()
                .id(new ObjectId())
                .forumId(forumId)
                .ownerId(ownerId)
                .title("Test Post Title")
                .content("This is the content of the test post.")
                .createdAt(Instant.now())
                .lastActivityAt(Instant.now())
                .build();

        postRepository.save(post);
        Post saved = postRepository.findById(post.getId()).orElse(null);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getForumId()).isEqualTo(forumId);
        assertThat(saved.getOwnerId()).isEqualTo(ownerId);
        assertThat(saved.getTitle()).isEqualTo("Test Post Title");
        assertThat(saved.getContent()).isEqualTo("This is the content of the test post.");
        assertThat(saved.getUpvoteCount()).isEqualTo(0);
        assertThat(saved.getDownvoteCount()).isEqualTo(0);
        assertThat(saved.getScore()).isEqualTo(0);
        assertThat(saved.getCommentCount()).isEqualTo(0);
        assertThat(saved.getIsDeleted()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getLastActivityAt()).isNotNull();
    }

    @Test
    void shouldFindPostsByForumId() {
        // Arrange
        ObjectId forumId = new ObjectId();

        Post post1 = Post.builder()
                .id(new ObjectId())
                .forumId(forumId)
                .ownerId(new ObjectId())
                .title("Post 1")
                .createdAt(Instant.now())
                .build();

        Post post2 = Post.builder()
                .id(new ObjectId())
                .forumId(forumId)
                .ownerId(new ObjectId())
                .title("Post 2")
                .createdAt(Instant.now())
                .build();

        postRepository.save(post1);
        postRepository.save(post2);

        // Act
        List<Post> results = postRepository.findByForumId(forumId);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Post::getTitle)
                .containsExactlyInAnyOrder("Post 1", "Post 2");
    }
}

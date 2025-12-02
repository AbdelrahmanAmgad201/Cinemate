package org.example.backend.comment;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void shouldCreateAndRetrieveComment() {
        ObjectId postId = new ObjectId();
        ObjectId ownerId = new ObjectId();

        Comment comment = Comment.builder()
                .id(new ObjectId())
                .postId(postId)
                .parentId(null)
                .ownerId(ownerId)
                .content("This is a test comment")
                .createdAt(Instant.now())
                .build();

        // Act
        commentRepository.save(comment);
        Comment saved = commentRepository.findById(comment.getId()).orElse(null);

        assertThat(saved).isNotNull();
        assertThat(saved.getPostId()).isEqualTo(postId);
        assertThat(saved.getParentId()).isNull();
        assertThat(saved.getOwnerId()).isEqualTo(ownerId);
        assertThat(saved.getContent()).isEqualTo("This is a test comment");
        assertThat(saved.getUpvoteCount()).isEqualTo(0);
        assertThat(saved.getDownvoteCount()).isEqualTo(0);
        assertThat(saved.getScore()).isEqualTo(0);
        assertThat(saved.getDepth()).isEqualTo(0);
        assertThat(saved.getIsDeleted()).isFalse();

        assertThat(saved.getCreatedAt()).isNotNull();
    }
}

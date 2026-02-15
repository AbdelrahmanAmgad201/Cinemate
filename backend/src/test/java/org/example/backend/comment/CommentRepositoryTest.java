package org.example.backend.comment;

import org.bson.types.ObjectId;
import org.example.backend.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


class CommentRepositoryTest extends AbstractMongoIntegrationTest {

    @Autowired
    private CommentRepository commentRepository;

    private ObjectId postId;
    private ObjectId ownerId;
    private ObjectId parentId;

    @BeforeEach
    void setUp() {
        postId = new ObjectId();
        ownerId = new ObjectId();
        parentId = new ObjectId();
    }

    @Test
    void save_ValidComment_Success() {
        Comment comment = createComment(postId, ownerId, null);

        Comment saved = commentRepository.save(comment);

        assertNotNull(saved.getId());
        assertEquals(postId, saved.getPostId());
        assertEquals(ownerId, saved.getOwnerId());
        assertNull(saved.getParentId());
    }

    @Test
    void save_CommentWithParent_Success() {
        Comment comment = createComment(postId, ownerId, parentId);

        Comment saved = commentRepository.save(comment);

        assertNotNull(saved.getId());
        assertEquals(parentId, saved.getParentId());
    }

    @Test
    void findById_ExistingComment_ReturnsComment() {
        Comment comment = createComment(postId, ownerId, null);
        Comment saved = commentRepository.save(comment);

        Optional<Comment> found = commentRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void findById_NonExistingComment_ReturnsEmpty() {
        Optional<Comment> found = commentRepository.findById(new ObjectId());

        assertFalse(found.isPresent());
    }

    @Test
    void deleteById_ExistingComment_DeletesSuccessfully() {
        Comment comment = createComment(postId, ownerId, null);
        Comment saved = commentRepository.save(comment);

        commentRepository.deleteById(saved.getId());

        assertFalse(commentRepository.findById(saved.getId()).isPresent());
    }

    @Test
    void findAll_MultipleComments_ReturnsAll() {
        Comment comment1 = createComment(postId, ownerId, null);
        Comment comment2 = createComment(postId, ownerId, parentId);
        commentRepository.saveAll(List.of(comment1, comment2));

        List<Comment> all = commentRepository.findAll();

        assertTrue(all.size() >= 2);
    }

    @Test
    void save_CommentWithAllFields_PreservesAllData() {
        Comment comment = Comment.builder()
                .postId(postId)
                .parentId(parentId)
                .ownerId(ownerId)
                .content("Test content")
                .upvoteCount(5)
                .downvoteCount(2)
                .score(3)
                .depth(1)
                .createdAt(Instant.now())
                .isDeleted(false)
                .build();

        Comment saved = commentRepository.save(comment);

        assertEquals("Test content", saved.getContent());
        assertEquals(5, saved.getUpvoteCount());
        assertEquals(2, saved.getDownvoteCount());
        assertEquals(3, saved.getScore());
        assertEquals(1, saved.getDepth());
        assertFalse(saved.getIsDeleted());
    }

    @Test
    void save_CommentWithDefaultValues_UsesDefaults() {
        Comment comment = Comment.builder()
                .postId(postId)
                .ownerId(ownerId)
                .content("Test")
                .build();

        Comment saved = commentRepository.save(comment);

        assertEquals(0, saved.getUpvoteCount());
        assertEquals(0, saved.getDownvoteCount());
        assertEquals(0, saved.getScore());
        assertEquals(0, saved.getDepth());
        assertFalse(saved.getIsDeleted());
    }

    @Test
    void save_SoftDeletedComment_PreservesDeletedState() {
        Comment comment = Comment.builder()
                .postId(postId)
                .ownerId(ownerId)
                .content("Deleted comment")
                .isDeleted(true)
                .deletedAt(Instant.now())
                .build();

        Comment saved = commentRepository.save(comment);

        assertTrue(saved.getIsDeleted());
        assertNotNull(saved.getDeletedAt());
    }

    @Test
    void update_ExistingComment_UpdatesSuccessfully() {
        Comment comment = createComment(postId, ownerId, null);
        Comment saved = commentRepository.save(comment);

        saved.setContent("Updated content");
        saved.setUpvoteCount(10);
        Comment updated = commentRepository.save(saved);

        assertEquals("Updated content", updated.getContent());
        assertEquals(10, updated.getUpvoteCount());
    }

    @Test
    void save_MultipleCommentsForSamePost_Success() {
        Comment comment1 = createComment(postId, ownerId, null, "Comment 1");
        Comment comment2 = createComment(postId, ownerId, null, "Comment 2");
        Comment comment3 = createComment(postId, ownerId, null, "Comment 3");

        List<Comment> saved = commentRepository.saveAll(List.of(comment1, comment2, comment3));

        assertEquals(3, saved.size());
        assertTrue(saved.stream().allMatch(c -> c.getPostId().equals(postId)));
    }

    @Test
    void save_NestedComments_PreservesDepth() {
        Comment topLevel = createCommentWithDepth(postId, ownerId, null, 0);
        Comment reply = createCommentWithDepth(postId, ownerId, topLevel.getId(), 1);
        Comment nestedReply = createCommentWithDepth(postId, ownerId, reply.getId(), 2);

        Comment savedTop = commentRepository.save(topLevel);
        Comment savedReply = commentRepository.save(reply);
        Comment savedNested = commentRepository.save(nestedReply);

        assertEquals(0, savedTop.getDepth());
        assertEquals(1, savedReply.getDepth());
        assertEquals(2, savedNested.getDepth());
    }

    @Test
    void votableMethods_IncrementAndDecrement_WorkCorrectly() {
        Comment comment = createComment(postId, ownerId, null);
        Comment saved = commentRepository.save(comment);

        saved.incrementUpvote();
        saved.incrementUpvote();
        saved.incrementDownvote();
        saved.decrementUpvote();

        Comment updated = commentRepository.save(saved);

        assertEquals(1, updated.getUpvoteCount());
        assertEquals(1, updated.getDownvoteCount());
    }

    @Test
    void save_CommentWithCreatedAt_PreservesTimestamp() {
        Instant specificTime = Instant.now().minus(5, ChronoUnit.DAYS);
        Comment comment = Comment.builder()
                .postId(postId)
                .ownerId(ownerId)
                .content("Test")
                .createdAt(specificTime)
                .build();

        Comment saved = commentRepository.save(comment);

        assertEquals(specificTime, saved.getCreatedAt());
    }

    @Test
    void count_ReturnsCorrectCount() {
        Comment comment1 = createComment(postId, ownerId, null);
        Comment comment2 = createComment(postId, ownerId, null);
        commentRepository.saveAll(List.of(comment1, comment2));

        long count = commentRepository.count();

        assertTrue(count >= 2);
    }

    private Comment createComment(ObjectId postId, ObjectId ownerId, ObjectId parentId) {
        return createComment(postId, ownerId, parentId, "Test comment content");
    }

    private Comment createComment(ObjectId postId, ObjectId ownerId, ObjectId parentId, String content) {
        return Comment.builder()
                .postId(postId)
                .ownerId(ownerId)
                .parentId(parentId)
                .content(content)
                .createdAt(Instant.now())
                .build();
    }

    private Comment createCommentWithDepth(ObjectId postId, ObjectId ownerId, ObjectId parentId, int depth) {
        return Comment.builder()
                .postId(postId)
                .ownerId(ownerId)
                .parentId(parentId)
                .content("Test comment")
                .depth(depth)
                .createdAt(Instant.now())
                .build();
    }
}
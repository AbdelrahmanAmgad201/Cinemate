package org.example.backend.deletion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.comment.CommentRepository;
import org.example.backend.forum.ForumRepository;
import org.example.backend.post.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Soft-delete cascades, now a few bulk SQL statements instead of the Mongo $graphLookup +
 * async batching (~450 lines removed). The DB counter triggers keep forum.post_count,
 * post.comment_count and parent comments' number_of_replies correct as rows flip.
 *
 * Physical cleanup (the scheduled purge) is a plain DELETE — FK ON DELETE CASCADE removes
 * the children (comments, votes, follows) automatically.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CascadeDeletionService {

    private final ForumRepository forumRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final Clock clock;

    /** Forum → its posts → their comments. */
    @Transactional
    public void deleteForum(UUID forumId) {
        Instant now = Instant.now(clock);
        commentRepository.softDeleteByForum(forumId, now);
        postRepository.softDeleteByForum(forumId, now);
        forumRepository.softDelete(forumId, now);
        log.info("Soft-deleted forum {} and its posts/comments", forumId);
    }

    /** Post → its comments. */
    @Transactional
    public void deletePost(UUID postId) {
        Instant now = Instant.now(clock);
        commentRepository.softDeleteByPost(postId, now);
        postRepository.softDelete(postId, now);
        log.info("Soft-deleted post {} and its comments", postId);
    }

    /** Comment → its reply subtree (recursive CTE). */
    @Transactional
    public void deleteComment(UUID commentId) {
        Instant now = Instant.now(clock);
        int n = commentRepository.softDeleteSubtree(commentId, now);
        log.info("Soft-deleted comment {} and {} descendant(s)", commentId, Math.max(0, n - 1));
    }

    // ─── Scheduled physical purge (FK ON DELETE CASCADE clears children) ─────────
    @Transactional
    public void purgeOldForums(int daysOld)  { forumRepository.purgeDeletedBefore(cutoff(daysOld)); }

    @Transactional
    public void purgeOldPosts(int daysOld)    { postRepository.purgeDeletedBefore(cutoff(daysOld)); }

    @Transactional
    public void purgeOldComments(int daysOld) { commentRepository.purgeDeletedBefore(cutoff(daysOld)); }

    private Instant cutoff(int daysOld) {
        return Instant.now(clock).minusSeconds(daysOld * 24L * 60 * 60);
    }
}

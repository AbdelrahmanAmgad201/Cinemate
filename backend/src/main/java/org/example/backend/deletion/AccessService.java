package org.example.backend.deletion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.comment.Comment;
import org.example.backend.comment.CommentRepository;
import org.example.backend.forum.ForumRepository;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Delete-permission checks. Owner ids are real user ids (Long) now; the former
 * postOwnerId/forumId denormalization on Comment is gone, so comment checks join
 * comment -> post -> forum (cheap PK/FK lookups).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessService {

    private final ForumRepository forumRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public boolean canDeleteForum(Long userId, UUID forumId) {
        return forumRepository.findById(forumId)
                .filter(f -> !f.getIsDeleted())
                .map(f -> f.getOwnerId().equals(userId))
                .orElse(false);
    }

    public boolean canDeletePost(Long userId, UUID postId) {
        Post post = postRepository.findById(postId).filter(p -> !p.getIsDeleted()).orElse(null);
        if (post == null) {
            return false;
        }
        if (post.getOwnerId().equals(userId)) {
            return true; // post owner
        }
        return isForumOwner(post.getForumId(), userId); // parent-forum owner
    }

    public boolean canDeleteComment(Long userId, UUID commentId) {
        Comment comment = commentRepository.findById(commentId).filter(c -> !c.getIsDeleted()).orElse(null);
        if (comment == null) {
            return false;
        }
        if (comment.getOwnerId().equals(userId)) {
            return true; // comment owner
        }
        Post post = postRepository.findById(comment.getPostId()).orElse(null);
        if (post == null) {
            return false;
        }
        if (post.getOwnerId().equals(userId)) {
            return true; // post owner
        }
        return isForumOwner(post.getForumId(), userId); // forum owner
    }

    private boolean isForumOwner(UUID forumId, Long userId) {
        return forumRepository.findById(forumId)
                .map(f -> f.getOwnerId().equals(userId))
                .orElse(false);
    }
}

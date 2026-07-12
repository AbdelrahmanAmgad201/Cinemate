package org.example.backend.comment;

import lombok.RequiredArgsConstructor;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.example.backend.moderation.ContentType;
import org.example.backend.moderation.ModerationOutboxService;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CascadeDeletionService deletionService;
    private final AccessService accessService;
    private final ModerationOutboxService moderationOutboxService;

    // Optimistic-publish moderation. The comment insert + outbox entry commit in one
    // transaction; the comment-count and reply-count are maintained by DB triggers (no
    // more manual $inc), and we bump the post's lastActivityAt for the "hot" ranking.
    @Transactional
    public Comment addComment(Long ownerId, AddCommentDTO addCommentDTO) {
        Post post = canComment(addCommentDTO.getPostId());
        Comment parentComment = getParentComment(addCommentDTO.getParentId());
        UUID parentId = (parentComment != null) ? parentComment.getId() : null;

        Comment comment = Comment.builder()
                .ownerId(ownerId)
                .postId(post.getId())
                .parentId(parentId)
                .content(addCommentDTO.getContent())
                .depth(parentComment == null ? 0 : parentComment.getDepth() + 1)
                .createdAt(Instant.now())
                .build();

        Comment saved = commentRepository.save(comment);
        if (parentId != null) {
            commentRepository.incrementReplies(parentId);
        }
        postRepository.touchLastActivity(post.getId(), Instant.now());
        moderationOutboxService.enqueue(ContentType.COMMENT, saved.getId(),
                saved.getModerationVersion(), saved.getContent());
        return saved;
    }

    @Transactional
    public void deleteComment(UUID commentId, Long userId) {
        if (!accessService.canDeleteComment(userId, commentId)) {
            throw new AccessDeniedException("User cannot delete this comment");
        }
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with id: " + commentId));
        systemDeleteComment(comment);
    }

    @Transactional
    public void systemDeleteComment(Comment comment) {
        postRepository.touchLastActivity(comment.getPostId(), Instant.now());
        deletionService.deleteComment(comment.getId());
        // The surviving parent loses exactly one direct child (the subtree root); the rest
        // of the subtree is deleted too, so no other reply-counts matter (REL-07).
        if (comment.getParentId() != null) {
            commentRepository.decrementReplies(comment.getParentId());
        }
    }

    @Transactional(readOnly = true)
    public Page<CommentView> getPostComments(UUID postId, int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, getSort(sortBy));
        return commentRepository.findByPostIdAndIsDeletedAndDepth(postId, false, 0, pageable);
    }

    // Cap on direct replies returned for one comment (API-NEW-01).
    private static final int MAX_REPLIES = 200;

    @Transactional(readOnly = true)
    public List<CommentView> getReplies(UUID commentId, String sortBy) {
        Pageable pageable = PageRequest.of(0, MAX_REPLIES, getSort(sortBy));
        return commentRepository.findByParentIdAndIsDeleted(commentId, false, pageable);
    }

    private Sort getSort(String sortBy) {
        return switch (sortBy) {
            case "new" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "top", "score" -> Sort.by(Sort.Direction.DESC, "score");
            default -> Sort.by(Sort.Direction.DESC, "score");
        };
    }

    private Post canComment(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));
        if (Boolean.TRUE.equals(post.getIsDeleted())) {
            throw new IllegalStateException("this post is deleted");
        }
        return post;
    }

    private Comment getParentComment(UUID parentId) {
        if (parentId == null) {
            return null;
        }
        return commentRepository.findById(parentId).orElse(null);
    }
}

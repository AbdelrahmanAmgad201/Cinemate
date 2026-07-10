package org.example.backend.comment;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.example.backend.forum.Forum;
import org.example.backend.hateSpeech.HateSpeechException;
import org.example.backend.hateSpeech.HateSpeechService;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import static org.example.backend.util.IdConverter.longToObjectId;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final MongoTemplate mongoTemplate;
    private final PostRepository postRepository;
    private final CascadeDeletionService deletionService;
    private final AccessService accessService;
    private final HateSpeechService hateSpeechService;

    @Transactional
    public Comment addComment(Long ownerId, AddCommentDTO addCommentDTO) {
        ObjectId postId = addCommentDTO.getPostId();
        Post post = canComment(postId);
        if (!hateSpeechService.analyzeText(addCommentDTO.getContent())) {
            throw new HateSpeechException("hate speech detected");
        }
        Comment parentComment = getParentComment(addCommentDTO.getParentId());
        ObjectId parentId = (parentComment != null) ? parentComment.getId() : null;
        Comment comment = defaultCommentBuilder(ownerId, post, parentId, addCommentDTO);
        if (parentComment == null) {
            comment.setDepth(0);
        } else {
            comment.setDepth(parentComment.getDepth() + 1);
            // Atomic $inc (REL-01) instead of load-mutate-save, which loses concurrent
            // replies under real load (two replies both read numberOfReplies=3, both save 4).
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(parentComment.getId())),
                    new Update().inc("numberOfReplies", 1),
                    Comment.class);
        }
        // Same atomic treatment for the post's commentCount, and $max instead of an
        // unconditional overwrite to preserve updateLastActivityAt()'s "only move forward"
        // semantics. This also removes the second, null-unchecked fetch of `post` that
        // canComment() already validated exists (REL-03).
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(postId)),
                new Update().inc("commentCount", 1).max("lastActivityAt", Instant.now()),
                Post.class);
        return commentRepository.save(comment);
    }


    @Transactional
    public void deleteComment(ObjectId commentId, Long userId) {
        if (!accessService.canDeleteComment(longToObjectId(userId), commentId)) {
            throw new AccessDeniedException("User " + " cannot delete this post");
        }
        Comment comment = mongoTemplate.findById(commentId, Comment.class);
        if (comment == null) {
            throw new IllegalArgumentException("Comment not found with id: " + commentId);
        }
        systemDeleteComment(comment);
    }

    public void systemDeleteComment(Comment comment) {
        Post post = mongoTemplate.findById(comment.getPostId(), Post.class);
        post.updateLastActivityAt(Instant.now());
        postRepository.save(post);
        deletionService.deleteComment(comment.getId());
    }

    @Transactional(readOnly = true)
    public Page<CommentView> getPostComments(ObjectId postId, int  page,int size,String sortBy) {
        Sort sort = getSort(sortBy);
        Pageable pageable = PageRequest.of(
                page,
                size,
                sort);
        return commentRepository.findByPostIdAndIsDeletedAndDepth(postId,false,0,pageable);
    }

    // Cap on direct replies returned for one comment (API-NEW-01) — a single
    // heavily-replied comment could otherwise return an unbounded result set.
    private static final int MAX_REPLIES = 200;

    @Transactional(readOnly = true)
    public List<CommentView> getReplies(ObjectId commentId,String sortBy) {
        Pageable pageable = PageRequest.of(0, MAX_REPLIES, getSort(sortBy));
        return commentRepository.findByParentIdAndIsDeleted(commentId,false,pageable);
    }

    private Sort  getSort(String  sortBy) {
        return switch (sortBy) {
            case "new" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "top", "score" -> Sort.by(Sort.Direction.DESC, "score");
            default -> Sort.by(Sort.Direction.DESC, "score");
        };
    }
    private Post canComment(ObjectId postId) {
        Post post = mongoTemplate.findById(postId, Post.class);
        if (post == null) {
            throw new IllegalArgumentException("Post not found with id: " + postId);
        }
        if (post.getIsDeleted()) {
            throw new IllegalStateException("this post is deleted");
        }
        return post;
    }

    private Comment defaultCommentBuilder(Long ownerId, Post post, ObjectId parentId, AddCommentDTO addCommentDTO) {
        // postOwnerId/forumId are denormalized from the post we already fetched to
        // validate the comment (PERF-06) — no extra query — so AccessService can check
        // delete permissions without chasing Comment -> Post -> Forum every time.
        Comment comment = Comment.builder()
                .ownerId(longToObjectId(ownerId))
                .postId(post.getId())
                .postOwnerId(post.getOwnerId())
                .forumId(post.getForumId())
                .parentId(parentId)
                .content(addCommentDTO.getContent())
                .createdAt(Instant.now())
                .build();
        return comment;
    }

    private Comment getParentComment(ObjectId parentId) {
        if (parentId == null )
            return null;
        return mongoTemplate.findById(parentId, Comment.class);
    }

}

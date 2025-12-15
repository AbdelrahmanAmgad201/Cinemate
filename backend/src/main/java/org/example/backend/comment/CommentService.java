package org.example.backend.comment;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.example.backend.forum.Forum;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

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

    @Transactional
    public Comment addComment(Long ownerId,AddCommentDTO addCommentDTO) {
        ObjectId postId = addCommentDTO.getPostId();
        canComment(postId);
        Comment parentComment = getParentComment(addCommentDTO.getParentId());
        ObjectId parentId = (parentComment != null ) ? parentComment.getId() : null;
        Comment comment = defaultCommentBuilder(ownerId,postId,parentId,addCommentDTO);
        if (parentComment == null)
                    comment.setDepth(0);
        else {
            int depth = parentComment.getDepth() + 1;
            comment.setDepth(depth);
        }
        Post post = mongoTemplate.findById(postId, Post.class);
        post.setCommentCount(post.getCommentCount() + 1);
        post.updateLastActivityAt(Instant.now());
        parentComment.setNumberOfReplies(parentComment.getNumberOfReplies() + 1);
        postRepository.save(post);
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
        Post post = mongoTemplate.findById(comment.getPostId(), Post.class);
        post.updateLastActivityAt(Instant.now());
        postRepository.save(post);
        deletionService.deleteComment(commentId);
    }

    @Transactional
    public Page<Comment> getPostComments(ObjectId postId, int  page,int size,String sortBy) {
        Sort sort = getSort(sortBy);
        Pageable pageable = PageRequest.of(
                page,
                size,
                sort);
        return commentRepository.findByPostIdAndIsDeletedAndDepth(postId,false,0,pageable);
    }

    @Transactional
    public List<Comment> getReplies(ObjectId commentId,String sortBy) {
        Sort sort = getSort(sortBy);
        return commentRepository.findByParentIdAndIsDeleted(commentId,false,sort);
    }

    private Sort  getSort(String  sortBy) {
        return switch (sortBy) {
            case "new" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "top", "score" -> Sort.by(Sort.Direction.DESC, "score");
            default -> Sort.by(Sort.Direction.DESC, "score");
        };
    }
    private void canComment(ObjectId postId) {
        Post post = mongoTemplate.findById(postId, Post.class);
        if (post == null) {
            throw new IllegalArgumentException("Post not found with id: " + postId);
        }
        if (post.getIsDeleted()) {
            throw new IllegalStateException("this post is deleted");
        }
    }

    private Comment defaultCommentBuilder(Long ownerId,ObjectId postId, ObjectId parentId,AddCommentDTO addCommentDTO) {
        Comment comment = Comment.builder()
                .ownerId(longToObjectId(ownerId))
                .postId(postId)
                .parentId(parentId)
                .content(addCommentDTO.getContent())
                .build();
        return comment;
    }

    private Comment getParentComment(ObjectId parentId) {
        if (parentId == null )
            return null;
        return mongoTemplate.findById(parentId, Comment.class);
    }

    private ObjectId longToObjectId(Long value) {
        return new ObjectId(String.format("%024x", value));
    }
}

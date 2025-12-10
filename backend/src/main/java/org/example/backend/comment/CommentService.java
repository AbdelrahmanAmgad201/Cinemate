package org.example.backend.comment;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.example.backend.forum.Forum;
import org.example.backend.post.Post;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final MongoTemplate mongoTemplate;

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
        return commentRepository.save(comment);
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

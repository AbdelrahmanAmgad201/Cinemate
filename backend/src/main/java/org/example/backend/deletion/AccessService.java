package org.example.backend.deletion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.example.backend.comment.Comment;
import org.example.backend.forum.Forum;
import org.example.backend.post.Post;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessService {

    private final MongoTemplate mongoTemplate;


    public boolean canDeleteForum(ObjectId userId, ObjectId forumId) {
        Forum forum = mongoTemplate.findById(forumId, Forum.class);

        if (forum == null) {
            log.warn("Forum not found: {}", forumId);
            return false;
        }

        if (forum.getIsDeleted()) {
            log.warn("Forum already deleted: {}", forumId);
            return false;
        }

        boolean hasAccess = forum.getOwnerId().equals(userId);

        if (!hasAccess) {
            log.warn("User {} does not have permission to delete forum {}", userId, forumId);
        }
{
            log.warn("User {} does not have permission to delete forum {}", userId, forumId);
        }
        return hasAccess;
    }

    public boolean canDeletePost(ObjectId userId, ObjectId postId) {
        Post post = mongoTemplate.findById(postId, Post.class);

        if (post == null) {
            log.warn("Post not found: {}", postId);
            return false;
        }

        if (post.getIsDeleted()) {
            log.warn("Post already deleted: {}", postId);
            return false;
        }

        // Post owner
        if (post.getOwnerId().equals(userId)) {
            return true;
        }

        // ParentForum Owner
        Forum forum = mongoTemplate.findById(post.getForumId(), Forum.class);
        if (forum != null && forum.getOwnerId().equals(userId)) {
            return true;
        }

        log.warn("User {} does not have permission to delete post {}", userId, postId);
        return false;
    }


    public boolean canDeleteComment(ObjectId userId, ObjectId commentId) {
        Comment comment = mongoTemplate.findById(commentId, Comment.class);

        if (comment == null) {
            log.warn("Comment not found: {}", commentId);
            return false;
        }

        if (comment.getIsDeleted()) {
            log.warn("Comment already deleted: {}", commentId);
            return false;
        }

        // Check if user is the comment owner
        if (comment.getOwnerId().equals(userId)) {
            return true;
        }

        // Check if user is the post owner
        Post post = mongoTemplate.findById(comment.getPostId(), Post.class);
        if (post == null) {
            log.warn("Post not found for comment: {}", commentId);
            return false;
        }

        if (post.getOwnerId().equals(userId)) {
            return true;
        }

        // Check if user is the forum owner
        Forum forum = mongoTemplate.findById(post.getForumId(), Forum.class);
        if (forum != null && forum.getOwnerId().equals(userId)) {
            return true;
        }

        log.warn("User {} does not have permission to delete comment {}", userId, commentId);
        return false;
    }


    public boolean canDeleteVote(ObjectId userId, ObjectId voteId) {
        Query query = Query.query(Criteria.where("_id").is(voteId));
        query.fields().include("userId").include("isDeleted");

        var vote = mongoTemplate.findOne(query, org.bson.Document.class, "votes");

        if (vote == null) {
            log.warn("Vote not found: {}", voteId);
            return false;
        }

        if (vote.getBoolean("isDeleted", false)) {
            log.warn("Vote already deleted: {}", voteId);
            return false;
        }

        ObjectId voteUserId = vote.getObjectId("userId");
        boolean hasAccess = voteUserId.equals(userId);

        if (!hasAccess) {
            log.warn("User {} does not have permission to delete vote {}", userId, voteId);
        }

        return hasAccess;
    }

    /**
     * Get the owner ID of a forum
     */
    public ObjectId getForumOwnerId(ObjectId forumId) {
        Query query = Query.query(Criteria.where("_id").is(forumId));
        query.fields().include("ownerId");

        Forum forum = mongoTemplate.findOne(query, Forum.class);
        return forum != null ? forum.getOwnerId() : null;
    }

    /**
     * Get the owner ID of a post
     */
    public ObjectId getPostOwnerId(ObjectId postId) {
        Query query = Query.query(Criteria.where("_id").is(postId));
        query.fields().include("ownerId");

        Post post = mongoTemplate.findOne(query, Post.class);
        return post != null ? post.getOwnerId() : null;
    }

    /**
     * Get the owner ID of a comment
     */
    public ObjectId getCommentOwnerId(ObjectId commentId) {
        Query query = Query.query(Criteria.where("_id").is(commentId));
        query.fields().include("ownerId");

        Comment comment = mongoTemplate.findOne(query, Comment.class);
        return comment != null ? comment.getOwnerId() : null;
    }
}
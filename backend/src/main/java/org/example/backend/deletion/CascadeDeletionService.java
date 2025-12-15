package org.example.backend.deletion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.backend.comment.Comment;
import org.example.backend.comment.CommentRepository;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CascadeDeletionService {

    private final MongoTemplate mongoTemplate;
    private static final int BATCH_SIZE = 100;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    /**
     * Delete Forum - cascades to Posts, Comments, Votes
     */
    public void deleteForum(ObjectId forumId) {
        log.info("Starting forum deletion: {}", forumId);
        Instant deletedAt = Instant.now();

        // 1. Soft delete the forum itself
        softDeleteEntity("forums", forumId, deletedAt);

        // 2. Cascade to all posts in this forum
        cascadeDeleteForumPostsAsync(forumId, deletedAt);
    }

    /**
     * Delete Post - cascades to Comments, Votes
     */
    public void deletePost(ObjectId postId) {
        log.info("Starting post deletion: {}", postId);
        Instant deletedAt = Instant.now();

        // 1. Soft delete the post itself
        softDeleteEntity("posts", postId, deletedAt);

        // 2. Cascade to comments and votes
        cascadeDeletePostAsync(postId, deletedAt);
    }

    /**
     * Delete Comment - cascades to Votes
     */
    public void deleteComment(ObjectId commentId) {
        log.info("Starting comment deletion: {}", commentId);
        Instant deletedAt = Instant.now();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        ObjectId postId = comment.getPostId();
        cascadeDeleteCommentAsync(commentId, deletedAt,postId,comment.getParentId());
    }

    /**
     * Delete Vote - no cascade (leaf entity)
     */
    public void deleteVote(ObjectId voteId) {
        log.info("Starting vote deletion: {}", voteId);
        Instant deletedAt = Instant.now();

        // Soft delete the vote itself (no cascade needed)
        softDeleteEntity("votes", voteId, deletedAt);

        log.info("Vote {} deleted successfully", voteId);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Generic soft delete for a single entity
     */
    private void softDeleteEntity(String collection, ObjectId id, Instant deletedAt) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update()
                .set("isDeleted", true)
                .set("deletedAt", deletedAt);

        long modified = mongoTemplate.updateFirst(query, update, collection).getModifiedCount();

        if (modified == 0) {
            log.warn("Entity not found or already deleted: {} in {}", id, collection);
        } else {
            log.info("Soft deleted {} with id: {}", collection, id);
        }
    }

    /**
     * Batch soft delete entities by criteria
     */
    private long softDeleteBatch(String collection, Criteria criteria, Instant deletedAt) {
        Query query = new Query(criteria);
        Update update = new Update()
                .set("isDeleted", true)
                .set("deletedAt", deletedAt);

        return mongoTemplate.updateMulti(query, update, collection).getModifiedCount();
    }

    /**
     * Get IDs for a collection matching criteria
     */
    private List<ObjectId> getIds(String collection, Criteria criteria) {
        Query query = new Query(criteria);
        query.fields().include("_id");
        return mongoTemplate.find(query, ObjectId.class, collection);
    }

    // ==================== ASYNC CASCADE METHODS ====================

    /**
     * Cascade delete all posts in a forum
     */
    @Async
    public void cascadeDeleteForumPostsAsync(ObjectId forumId, Instant deletedAt) {
        try {
            // Get all post IDs for this forum
            List<ObjectId> postIds = getIds("posts", Criteria.where("forumId").is(forumId));
            log.info("Found {} posts to delete for forum {}", postIds.size(), forumId);

            if (postIds.isEmpty()) {
                log.info("No posts to delete for forum {}", forumId);
                return;
            }

            // Soft delete posts in batches
            int totalPosts = softDeletePostsBatch(postIds, deletedAt);
            log.info("Soft deleted {} posts for forum {}", totalPosts, forumId);

            // Cascade to comments and votes for all posts
            cascadeDeletePostsBatch(postIds, deletedAt);

            log.info("Completed cascade deletion for forum {}", forumId);

        } catch (Exception e) {
            log.error("Error during forum cascade deletion: {}", forumId, e);
        }
    }

    /**
     * Cascade delete for a single post
     */
    @Async
    public void cascadeDeletePostAsync(ObjectId postId, Instant deletedAt) {
        try {
            // Get all comment IDs for this post
            List<ObjectId> commentIds = getIds("comments", Criteria.where("postId").is(postId));
            log.info("Found {} comments to delete for post {}", commentIds.size(), postId);

            // Soft delete comments in batches
            if (!commentIds.isEmpty()) {
                int totalComments = softDeleteCommentsBatch(List.of(postId), deletedAt);
                log.info("Soft deleted {} comments for post {}", totalComments, postId);

                // Delete votes on comments
                cascadeDeleteCommentsVotesBatch(commentIds, deletedAt);
            }

            // Delete votes on the post itself
            long postVotes = softDeleteBatch(
                    "votes",
                    Criteria.where("targetId").is(postId).and("isPost").is(true),
                    deletedAt
            );
            log.info("Soft deleted {} votes for post {}", postVotes, postId);

            log.info("Completed cascade deletion for post {}", postId);

        } catch (Exception e) {
            log.error("Error during post cascade deletion: {}", postId, e);
        }
    }

    /**
     * Cascade delete votes for a single comment
     */
    @Async
    public void cascadeDeleteCommentVotesAsync(ObjectId commentId, Instant deletedAt) {
        try {
            long votes = softDeleteBatch(
                    "votes",
                    Criteria.where("targetId").is(commentId).and("isPost").is(false),
                    deletedAt
            );
            log.info("Soft deleted {} votes for comment {}", votes, commentId);

        } catch (Exception e) {
            log.error("Error during comment votes deletion: {}", commentId, e);
        }
    }

    /**
     * Batch soft delete posts
     */
    private int softDeletePostsBatch(List<ObjectId> postIds, Instant deletedAt) {
        int totalDeleted = 0;

        for (int i = 0; i < postIds.size(); i += BATCH_SIZE) {
            List<ObjectId> batch = postIds.subList(i, Math.min(i + BATCH_SIZE, postIds.size()));
            long deleted = softDeleteBatch("posts", Criteria.where("_id").in(batch), deletedAt);
            totalDeleted += deleted;
            log.debug("Soft deleted batch of {} posts", deleted);
        }

        return totalDeleted;
    }

    /**
     * Cascade delete for multiple posts (comments + votes)
     */
    private void cascadeDeletePostsBatch(List<ObjectId> postIds, Instant deletedAt) {
        // Delete all comments for these posts
        int totalComments = softDeleteCommentsBatch(postIds, deletedAt);
        log.info("Soft deleted {} comments for {} posts", totalComments, postIds.size());

        // Get all comment IDs to delete their votes
        List<ObjectId> commentIds = getIds("comments", Criteria.where("postId").in(postIds));

        // Delete votes on posts
        int postVotes = softDeletePostVotesBatch(postIds, deletedAt);
        log.info("Soft deleted {} post votes", postVotes);

        // Delete votes on comments
        if (!commentIds.isEmpty()) {
            int commentVotes = cascadeDeleteCommentsVotesBatch(commentIds, deletedAt);
            log.info("Soft deleted {} comment votes", commentVotes);
        }
    }

    /**
     * Batch soft delete comments for posts
     */
    private int softDeleteCommentsBatch(List<ObjectId> postIds, Instant deletedAt) {
        int totalDeleted = 0;

        for (int i = 0; i < postIds.size(); i += BATCH_SIZE) {
            List<ObjectId> batch = postIds.subList(i, Math.min(i + BATCH_SIZE, postIds.size()));
            long deleted = softDeleteBatch("comments", Criteria.where("postId").in(batch), deletedAt);
            totalDeleted += deleted;
            log.debug("Soft deleted batch of {} comments", deleted);
        }

        return totalDeleted;
    }

    /**
     * Batch soft delete comments by comment IDs (not post IDs)
     */
    private int softDeleteCommentsByIdsBatch(List<ObjectId> commentIds, Instant deletedAt) {
        int totalDeleted = 0;

        for (int i = 0; i < commentIds.size(); i += BATCH_SIZE) {
            List<ObjectId> batch = commentIds.subList(i, Math.min(i + BATCH_SIZE, commentIds.size()));
            long deleted = softDeleteBatch("comments", Criteria.where("_id").in(batch), deletedAt);
            totalDeleted += deleted;
            log.debug("Soft deleted batch of {} comments", deleted);
        }

        return totalDeleted;
    }
    
    /**
     * Batch soft delete votes on posts
     */
    private int softDeletePostVotesBatch(List<ObjectId> postIds, Instant deletedAt) {
        int totalDeleted = 0;

        for (int i = 0; i < postIds.size(); i += BATCH_SIZE) {
            List<ObjectId> batch = postIds.subList(i, Math.min(i + BATCH_SIZE, postIds.size()));
            long deleted = softDeleteBatch(
                    "votes",
                    Criteria.where("targetId").in(batch).and("isPost").is(true),
                    deletedAt
            );
            totalDeleted += deleted;
        }

        return totalDeleted;
    }

    /**
     * Batch soft delete votes on comments
     */
    private int cascadeDeleteCommentsVotesBatch(List<ObjectId> commentIds, Instant deletedAt) {
        int totalDeleted = 0;

        for (int i = 0; i < commentIds.size(); i += BATCH_SIZE) {
            List<ObjectId> batch = commentIds.subList(i, Math.min(i + BATCH_SIZE, commentIds.size()));
            long deleted = softDeleteBatch(
                    "votes",
                    Criteria.where("targetId").in(batch).and("isPost").is(false),
                    deletedAt
            );
            totalDeleted += deleted;
        }

        return totalDeleted;
    }

    @Async
    public void cascadeDeleteCommentAsync(ObjectId commentId,Instant deletedAt,ObjectId postId,ObjectId parentId) {
        Aggregation aggregation = Aggregation.newAggregation(
                // Match the parent comment
                Aggregation.match(Criteria.where("_id").is(commentId)),

                // Use $graphLookup to recursively find all descendants
                Aggregation.graphLookup("comments")
                        .startWith("$_id")
                        .connectFrom("_id")
                        .connectTo("parentId")
                        .maxDepth(100)
                        .depthField("level")
                        .as("descendants"),

                Aggregation.project()
                        .and("_id").as("parentId")
                        .and("descendants._id").as("descendantIds")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                aggregation,
                "comments",
                Document.class
        );

        Document result = results.getUniqueMappedResult();
        if (result == null) {
            return ;
        }

        List<ObjectId> allIds = new ArrayList<>();
        allIds.add(result.getObjectId("parentId"));

        List<ObjectId> descendantIds = result.getList("descendantIds", ObjectId.class);
        if (descendantIds != null) {
            allIds.addAll(descendantIds);
        }

        int totalComments = softDeleteCommentsByIdsBatch(allIds, deletedAt);

        // Delete votes on comments
        cascadeDeleteCommentsVotesBatch(allIds, deletedAt);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setCommentCount(post.getCommentCount() - totalComments);
        postRepository.save(post);
        if(parentId != null) {
            Comment parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Comment not found"));
            parent.setNumberOfReplies(parent.getNumberOfReplies() - totalComments);
            commentRepository.save(parent);
        }
    }

    /**
     * Hard delete entities that have been soft-deleted for a certain period
     * Runs Daily
     */
    @Async
    public void hardDeleteOldEntities(String collection, int daysOld) {
        Instant cutoffDate = Instant.now().minusSeconds(daysOld * 24L * 60 * 60);

        Query query = new Query(
                Criteria.where("isDeleted").is(true)
                        .and("deletedAt").lt(cutoffDate)
        );

        long deleted = mongoTemplate.remove(query, collection).getDeletedCount();
        log.info("Hard deleted {} old entities from {}", deleted, collection);
    }
}
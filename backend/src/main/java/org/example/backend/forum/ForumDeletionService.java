package org.example.backend.forum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.example.backend.comment.CommentRepository;
import org.example.backend.post.PostRepository;
import org.example.backend.vote.VoteRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForumDeletionService {
    private final ForumRepository forumRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final VoteRepository voteRepository;
    private final MongoTemplate mongoTemplate;

    private static final int BATCH_SIZE = 100;

    public void deleteForum(ObjectId forumId) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new RuntimeException("Forum not found"));

        if (forum.getIsDeleted()) {
            throw new RuntimeException("Forum already deleted");
        }

        forum.setIsDeleted(true);
        forum.setDeletedAt(Instant.now());
        forumRepository.save(forum);
        log.info("Forum {} marked as deleted. Starting cascade deletion...", forumId);
        cascadeDeleteForumAsync(forumId);
    }


    private List<ObjectId> getPostIdsForForum(ObjectId forumId) {
        Query query = new Query(Criteria.where("forumId").is(forumId));
        query.fields().include("_id");
        return mongoTemplate.find(query, ObjectId.class, "posts");
    }

    @Async
    public void cascadeDeleteForumAsync(ObjectId forumId) {
        try {
            Instant deletedAt = Instant.now();

            // Step 1: Get all post IDs for this forum (in batches)
            List<ObjectId> postIds = getPostIdsForForum(forumId);
            log.info("Found {} posts to delete for forum {}", postIds.size(), forumId);

            // Step 2: Soft delete all posts (batch update)
            softDeletePostsBatch(postIds, deletedAt);

            // Step 3: Soft delete all comments for these posts (batch update)
            softDeleteCommentsBatch(postIds, deletedAt);

            // Step 4: Delete all votes on posts and comments (batch update)
            softDeleteVotesForPostsBatch(postIds, deletedAt);

            log.info("Successfully completed cascade deletion for forum {}", forumId);

        } catch (Exception e) {
            log.error("Error during cascade deletion for forum {}", forumId, e);
            // backup
        }
    }

    private void softDeleteVotesForPostsBatch(List<ObjectId> postIds, Instant deletedAt) {
        return;
    }

    private void softDeleteCommentsBatch(List<ObjectId> postIds, Instant deletedAt) {
        return;
    }

    private void softDeletePostsBatch(List<ObjectId> postIds, Instant deletedAt) {
        return;
    }
    // TODO: Add delete flag in vote Entity
    // TODO: Implement Soft Deletion for entities
    // TODO: Implement periodical job to clear all DB entities that are deleted


}

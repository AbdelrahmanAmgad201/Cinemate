package org.example.backend.comment;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface CommentRepository extends MongoRepository<Comment, ObjectId> {
    // CommentView (not Comment) so the controller never serializes the raw
    // document directly (CQ-NEW-03).
    Page<CommentView> findByPostIdAndIsDeletedAndDepth(ObjectId postId, Boolean isDeleted, Integer depth, Pageable pageable);
    // Paginated (PERF-04) — HateSpeechScheduler processes this in fixed-size batches
    // instead of loading every comment from the last 24h into heap at once.
    Page<Comment> findAllByCreatedAtBetween(Instant startDate, Instant endDate, Pageable pageable);
    // Pageable (not Sort) caps replies at a fixed size (API-NEW-01) — a single
    // viral comment could otherwise have an unbounded number of direct replies.
    List<CommentView> findByParentIdAndIsDeleted(ObjectId parentId, Boolean isDeleted, Pageable pageable);
}

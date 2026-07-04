package org.example.backend.post;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

@Repository
public interface PostRepository extends MongoRepository<Post, ObjectId> {
    List<Post> findByForumId(ObjectId forumId);

    // PostView (not Post) for the same CQ-NEW-03 reason as the paginated queries below.
    Optional<PostView> findByIdAndIsDeletedFalse(ObjectId id);

    Page<Post> findByIsDeletedFalseAndCreatedAtGreaterThanEqual(
            Instant since,
            Pageable pageable);

    // Page<PostView> (not Page<Post>) so the controller never serializes the raw
    // document directly (CQ-NEW-03) — same projection already used by
    // findAllByOwnerIdAndIsDeletedFalse below.
    Page<PostView> findByIsDeletedFalseAndForumIdIn(List<ObjectId> forumIds, Pageable pageable);
    Page<PostView> findByIsDeletedFalseAndForumId(ObjectId forumId, Pageable pageable);
    Page<PostView> findAllByOwnerIdAndIsDeletedFalse(ObjectId userId,Pageable pageable);
}

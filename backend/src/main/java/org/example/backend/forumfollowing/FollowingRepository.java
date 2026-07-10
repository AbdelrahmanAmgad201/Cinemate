package org.example.backend.forumfollowing;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowingRepository extends MongoRepository<Following, ObjectId> {
    // DB-NEW-03: this repository soft-deletes like every other domain in the app now
    // (see FollowingService) — no hard delete method, and every existence/listing query
    // is filtered by isDeleted so a soft-deleted follow doesn't count as "still following".

    // Unfiltered lookup: follow()/unfollow() need the row even if it's soft-deleted, to
    // reactivate it in place instead of violating the {userId, forumId} unique index.
    Optional<Following> findByUserIdAndForumId(ObjectId userId, ObjectId forumId);

    boolean existsByUserIdAndForumIdAndIsDeletedFalse(ObjectId userId, ObjectId forumId);

    List<Following> findByUserIdAndIsDeletedFalse(ObjectId userId);

    Page<Following> findByUserIdAndIsDeletedFalse(ObjectId userId, Pageable pageable);

    @Query(value = "{ 'userId': ?0, 'isDeleted': false }", fields = "{ 'forumId': 1, '_id': 0 }")
    List<Document> findForumIdsByUserId(ObjectId userId);
}
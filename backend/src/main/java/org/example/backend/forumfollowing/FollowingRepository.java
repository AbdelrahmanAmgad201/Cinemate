package org.example.backend.forumfollowing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowingRepository extends MongoRepository<Following, ObjectId> {
    List<Following> findByUserId(ObjectId userId);
    boolean existsByUserIdAndForumId(ObjectId userId, ObjectId forumId);
    void deleteByUserIdAndForumId(ObjectId userId, ObjectId forumId);

    Page<Following> findByUserId(ObjectId userId, Pageable pageable);

    @Query(value = "{ 'userId': ?0 }", fields = "{ 'forumId': 1, '_id': 0 }")
    List<ObjectId> findForumIdsByUserId(ObjectId userId);
}
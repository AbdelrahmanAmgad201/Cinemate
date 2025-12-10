package org.example.backend.forumfollowing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.bson.types.ObjectId;
import org.example.backend.forum.Forum;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowingRepository extends MongoRepository<Following, ObjectId> {
    List<Following> findByUserId(ObjectId userId);
    boolean existsByUserIdAndForumId(ObjectId userId, ObjectId forumId);
    void deleteByUserIdAndForumId(ObjectId userId, ObjectId forumId);

    Page<Following> findByUserId(ObjectId userId, Pageable pageable);
}
package org.example.backend.vote;

import org.bson.types.ObjectId;
import org.example.backend.forum.Forum;
import org.example.backend.forumfollowing.Following;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRepository extends MongoRepository<Vote, ObjectId> {
    List<Vote> findByUserId(ObjectId userId);
    List<Vote> findByTargetId(ObjectId targetId);
    List<Vote> findByUserIdAndTargetId(ObjectId userId, ObjectId targetId);
    Vote findByIsDeletedIsFalseAndUserIdAndTargetId(ObjectId userId, ObjectId targetId);
}


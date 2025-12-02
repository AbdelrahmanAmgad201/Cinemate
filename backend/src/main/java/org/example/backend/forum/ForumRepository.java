package org.example.backend.forum;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumRepository extends MongoRepository<Forum, ObjectId> {
    List<Forum> findByOwnerId(ObjectId ownerId);
}

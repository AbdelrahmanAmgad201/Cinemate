package org.example.backend.post;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;

@Repository
public interface PostRepository extends MongoRepository<Post, ObjectId> {
    List<Post> findByForumId(ObjectId forumId);

    Page<Post> findByIsDeletedFalseAndCreatedAtGreaterThanEqual(
            Instant since,
            Pageable pageable);

    Page<Post> findByIsDeletedFalseAndForumIdIn(List<ObjectId> forumIds, Pageable pageable);
    Page<Post> findByIsDeletedFalseAndForumId(ObjectId forumId, Pageable pageable);
}

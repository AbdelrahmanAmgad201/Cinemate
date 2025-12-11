package org.example.backend.post;

import org.bson.types.ObjectId;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Pageable;

@Repository
public interface PostRepository extends MongoRepository<Post, ObjectId> {
    List<Post> findByForumId(ObjectId forumId);
    Page<Post> findByForumId(ObjectId forumId,Pageable pageable);
}

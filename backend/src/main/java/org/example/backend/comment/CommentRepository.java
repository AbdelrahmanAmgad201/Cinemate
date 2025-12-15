package org.example.backend.comment;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommentRepository extends MongoRepository<Comment, ObjectId> {
    Page<Comment> findByPostIdAndIsDeletedAndDepth(ObjectId postId, Boolean isDeleted, Integer depth, Pageable pageable);

    List<Comment> findByParentIdAndIsDeleted(ObjectId parentId, Boolean isDeleted, Sort sort);
}

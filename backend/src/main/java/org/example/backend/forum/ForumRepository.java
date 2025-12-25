package org.example.backend.forum;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumRepository extends MongoRepository<Forum, ObjectId> {
    List<Forum> findByOwnerId(ObjectId ownerId);
    Page<Forum> findByNameContainingIgnoreCaseAndIsDeletedFalse(String name, Pageable pageable);
    Page<Forum> findAllByIsDeletedFalse(Pageable pageable);
    Page<ForumDisplayDTO> findAllByOwnerIdAndIsDeletedFalse(ObjectId ownerId, Pageable pageable);
}

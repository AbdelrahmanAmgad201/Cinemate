package org.example.backend.userfollowing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowsRepository extends JpaRepository<Follows, FollowsID> {
    void deleteAllByIsDeleted(Boolean value);
}
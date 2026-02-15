package org.example.backend.userfollowing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowsRepository extends JpaRepository<Follows, FollowsID> {
    Page<FollowerView> findAllByFollowedUser_IdAndIsDeletedFalse(Long followedUserId, Pageable pageable);
    Page<FollowingView> findAllByFollowingUser_IdAndIsDeletedFalse(Long followingUserId, Pageable pageable);
}
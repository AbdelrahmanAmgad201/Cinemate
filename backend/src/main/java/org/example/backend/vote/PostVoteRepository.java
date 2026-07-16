package org.example.backend.vote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostVoteRepository extends JpaRepository<PostVote, PostVoteId> {
    Optional<PostVote> findByUserIdAndPostId(Long userId, UUID postId);
    List<PostVote> findByUserId(Long userId);
}

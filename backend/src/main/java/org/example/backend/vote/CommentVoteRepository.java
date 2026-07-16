package org.example.backend.vote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentVoteRepository extends JpaRepository<CommentVote, CommentVoteId> {
    Optional<CommentVote> findByUserIdAndCommentId(Long userId, UUID commentId);
    List<CommentVote> findByUserId(Long userId);
}

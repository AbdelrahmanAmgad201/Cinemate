package org.example.backend.vote;

import org.bson.types.ObjectId;
import org.example.backend.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VoteRepositoryTest extends AbstractMongoIntegrationTest {

    @Autowired
    private VoteRepository voteRepository;

    @BeforeEach
    void setUp() {
        voteRepository.deleteAll();
    }

    @Test
    void shouldCreateAndRetrieveVote() {
        // Arrange
        ObjectId userId = new ObjectId();
        ObjectId targetId = new ObjectId();

        Vote vote = Vote.builder()
                .id(new ObjectId())
                .userId(userId)
                .isPost(true)
                .targetId(targetId)
                .voteType(1)  // upvote
                .createdAt(Instant.now())
                .build();

        // Act
        voteRepository.save(vote);
        Vote saved = voteRepository.findById(vote.getId()).orElse(null);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getIsPost()).isTrue();
        assertThat(saved.getTargetId()).isEqualTo(targetId);
        assertThat(saved.getVoteType()).isEqualTo(1);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindVotesByUserId() {
        // Arrange
        ObjectId userId = new ObjectId();
        ObjectId targetId1 = new ObjectId();
        ObjectId targetId2 = new ObjectId();
        ObjectId otherUserId = new ObjectId();

        Vote vote1 = Vote.builder()
                .id(new ObjectId())
                .userId(userId)
                .isPost(true)
                .targetId(targetId1)
                .voteType(1)
                .createdAt(Instant.now())
                .build();

        Vote vote2 = Vote.builder()
                .id(new ObjectId())
                .userId(userId)
                .isPost(false)
                .targetId(targetId2)
                .voteType(-1)
                .createdAt(Instant.now())
                .build();

        Vote vote3 = Vote.builder()
                .id(new ObjectId())
                .userId(otherUserId)
                .isPost(true)
                .targetId(targetId1)
                .voteType(1)
                .createdAt(Instant.now())
                .build();

        voteRepository.save(vote1);
        voteRepository.save(vote2);
        voteRepository.save(vote3);

        // Act
        List<Vote> userVotes = voteRepository.findByUserId(userId);

        // Assert
        assertThat(userVotes).hasSize(2);
        assertThat(userVotes)
                .extracting(Vote::getUserId)
                .containsOnly(userId);
        assertThat(userVotes)
                .extracting(Vote::getTargetId)
                .containsExactlyInAnyOrder(targetId1, targetId2);
    }

    @Test
    void shouldFindVotesByTargetId() {
        // Arrange
        ObjectId targetId = new ObjectId();
        ObjectId userId1 = new ObjectId();
        ObjectId userId2 = new ObjectId();

        Vote vote1 = Vote.builder()
                .id(new ObjectId())
                .userId(userId1)
                .isPost(true)
                .targetId(targetId)
                .voteType(1)
                .createdAt(Instant.now())
                .build();

        Vote vote2 = Vote.builder()
                .id(new ObjectId())
                .userId(userId2)
                .isPost(true)
                .targetId(targetId)
                .voteType(-1)
                .createdAt(Instant.now())
                .build();

        voteRepository.save(vote1);
        voteRepository.save(vote2);

        // Act
        List<Vote> targetVotes = voteRepository.findByTargetId(targetId);

        // Assert
        assertThat(targetVotes).hasSize(2);
        assertThat(targetVotes)
                .extracting(Vote::getTargetId)
                .containsOnly(targetId);
    }

    @Test
    void shouldFindVotesByUserIdAndTargetId() {
        // Arrange
        ObjectId userId = new ObjectId();
        ObjectId targetId = new ObjectId();
        ObjectId otherTargetId = new ObjectId();

        Vote vote1 = Vote.builder()
                .id(new ObjectId())
                .userId(userId)
                .isPost(true)
                .targetId(targetId)
                .voteType(1)
                .createdAt(Instant.now())
                .build();

        Vote vote2 = Vote.builder()
                .id(new ObjectId())
                .userId(userId)
                .isPost(false)
                .targetId(otherTargetId)
                .voteType(-1)
                .createdAt(Instant.now())
                .build();

        voteRepository.save(vote1);
        voteRepository.save(vote2);

        // Act
        List<Vote> specificVotes = voteRepository.findByUserIdAndTargetId(userId, targetId);

        // Assert
        assertThat(specificVotes).hasSize(1);
        assertThat(specificVotes.get(0).getUserId()).isEqualTo(userId);
        assertThat(specificVotes.get(0).getTargetId()).isEqualTo(targetId);
    }

    @Test
    void shouldReturnEmptyListWhenNoVotesExist() {
        // Arrange
        ObjectId userId = new ObjectId();

        // Act
        List<Vote> userVotes = voteRepository.findByUserId(userId);

        // Assert
        assertThat(userVotes).isEmpty();
    }


    @Test
    void shouldAllowSameUserToVoteOnDifferentTargets() {
        // Arrange
        ObjectId userId = new ObjectId();
        ObjectId targetId1 = new ObjectId();
        ObjectId targetId2 = new ObjectId();

        Vote vote1 = Vote.builder()
                .id(new ObjectId())
                .userId(userId)
                .isPost(true)
                .targetId(targetId1)
                .voteType(1)
                .createdAt(Instant.now())
                .build();

        Vote vote2 = Vote.builder()
                .id(new ObjectId())
                .userId(userId)
                .isPost(true)
                .targetId(targetId2)
                .voteType(1)
                .createdAt(Instant.now())
                .build();

        // Act
        voteRepository.save(vote1);
        voteRepository.save(vote2);
        List<Vote> votes = voteRepository.findByUserId(userId);

        // Assert
        assertThat(votes).hasSize(2);
    }

    @Test
    void shouldAllowSameUserToVoteOnPostAndCommentWithSameId() {
        // Arrange
        ObjectId userId = new ObjectId();
        ObjectId targetId = new ObjectId();

        Vote voteOnPost = Vote.builder()
                .id(new ObjectId())
                .userId(userId)
                .isPost(true)
                .targetId(targetId)
                .voteType(1)
                .createdAt(Instant.now())
                .build();

        Vote voteOnComment = Vote.builder()
                .id(new ObjectId())
                .userId(userId)
                .isPost(false)
                .targetId(targetId)
                .voteType(1)
                .createdAt(Instant.now())
                .build();

        // Act
        voteRepository.save(voteOnPost);
        voteRepository.save(voteOnComment);
        List<Vote> votes = voteRepository.findByUserId(userId);

        // Assert
        assertThat(votes).hasSize(2);
        assertThat(votes).extracting(Vote::getIsPost).containsExactlyInAnyOrder(true, false);
    }

    @Test
    void shouldDeleteVote() {
        // Arrange
        ObjectId userId = new ObjectId();
        ObjectId targetId = new ObjectId();

        Vote vote = Vote.builder()
                .id(new ObjectId())
                .userId(userId)
                .isPost(true)
                .targetId(targetId)
                .voteType(1)
                .createdAt(Instant.now())
                .build();

        voteRepository.save(vote);

        // Act
        voteRepository.delete(vote);
        Vote deleted = voteRepository.findById(vote.getId()).orElse(null);

        // Assert
        assertThat(deleted).isNull();
    }
}
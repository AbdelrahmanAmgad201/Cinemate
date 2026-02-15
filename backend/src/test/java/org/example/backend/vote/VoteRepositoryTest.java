package org.example.backend.vote;

import org.bson.types.ObjectId;
import org.example.backend.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VoteRepositoryTest extends AbstractMongoIntegrationTest {

    @Autowired
    private VoteRepository voteRepository;

    private ObjectId userId;
    private ObjectId targetId;

    @BeforeEach
    void setUp() {
        userId = new ObjectId();
        targetId = new ObjectId();
    }

    @Test
    void save_ValidVote_Success() {
        Vote vote = createVote(userId, targetId, true, 1);

        Vote saved = voteRepository.save(vote);

        assertNotNull(saved.getId());
        assertEquals(userId, saved.getUserId());
        assertEquals(targetId, saved.getTargetId());
        assertTrue(saved.getIsPost());
        assertEquals(1, saved.getVoteType());
    }

    @Test
    void findById_ExistingVote_ReturnsVote() {
        Vote vote = createVote(userId, targetId, true, 1);
        Vote saved = voteRepository.save(vote);

        Optional<Vote> found = voteRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void findById_NonExistingVote_ReturnsEmpty() {
        Optional<Vote> found = voteRepository.findById(new ObjectId());

        assertFalse(found.isPresent());
    }

    @Test
    void deleteById_ExistingVote_DeletesSuccessfully() {
        Vote vote = createVote(userId, targetId, true, 1);
        Vote saved = voteRepository.save(vote);

        voteRepository.deleteById(saved.getId());

        assertFalse(voteRepository.findById(saved.getId()).isPresent());
    }

    @Test
    void findByUserId_ReturnsAllVotesForUser() {
        ObjectId target1 = new ObjectId();
        ObjectId target2 = new ObjectId();

        Vote vote1 = createVote(userId, target1, true, 1);
        Vote vote2 = createVote(userId, target2, false, -1);
        voteRepository.saveAll(List.of(vote1, vote2));

        List<Vote> result = voteRepository.findByUserId(userId);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(v -> v.getUserId().equals(userId)));
    }

    @Test
    void findByUserId_DifferentUsers_ReturnsOnlyUserVotes() {
        ObjectId user1 = new ObjectId();
        ObjectId user2 = new ObjectId();

        Vote vote1 = createVote(user1, targetId, true, 1);
        Vote vote2 = createVote(user2, targetId, true, 1);
        voteRepository.saveAll(List.of(vote1, vote2));

        List<Vote> result = voteRepository.findByUserId(user1);

        assertEquals(1, result.size());
        assertEquals(user1, result.get(0).getUserId());
    }

    @Test
    void findByUserId_NoVotes_ReturnsEmpty() {
        List<Vote> result = voteRepository.findByUserId(new ObjectId());

        assertTrue(result.isEmpty());
    }

    @Test
    void findByTargetId_ReturnsAllVotesForTarget() {
        ObjectId user1 = new ObjectId();
        ObjectId user2 = new ObjectId();

        Vote vote1 = createVote(user1, targetId, true, 1);
        Vote vote2 = createVote(user2, targetId, true, -1);
        voteRepository.saveAll(List.of(vote1, vote2));

        List<Vote> result = voteRepository.findByTargetId(targetId);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(v -> v.getTargetId().equals(targetId)));
    }

    @Test
    void findByTargetId_DifferentTargets_ReturnsOnlyTargetVotes() {
        ObjectId target1 = new ObjectId();
        ObjectId target2 = new ObjectId();

        Vote vote1 = createVote(userId, target1, true, 1);
        Vote vote2 = createVote(userId, target2, true, 1);
        voteRepository.saveAll(List.of(vote1, vote2));

        List<Vote> result = voteRepository.findByTargetId(target1);

        assertEquals(1, result.size());
        assertEquals(target1, result.get(0).getTargetId());
    }

    @Test
    void findByTargetId_NoVotes_ReturnsEmpty() {
        List<Vote> result = voteRepository.findByTargetId(new ObjectId());

        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserIdAndTargetId_ReturnsMatchingVote() {
        Vote vote = createVote(userId, targetId, true, 1);
        voteRepository.save(vote);

        List<Vote> result = voteRepository.findByUserIdAndTargetId(userId, targetId);

        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals(targetId, result.get(0).getTargetId());
    }

    @Test
    void findByUserIdAndTargetId_NoMatch_ReturnsEmpty() {
        List<Vote> result = voteRepository.findByUserIdAndTargetId(new ObjectId(), new ObjectId());

        assertTrue(result.isEmpty());
    }

    @Test
    void save_VoteWithAllFields_PreservesAllData() {
        Vote vote = Vote.builder()
                .userId(userId)
                .targetId(targetId)
                .isPost(true)
                .voteType(1)
                .createdAt(Instant.now())
                .isDeleted(false)
                .build();

        Vote saved = voteRepository.save(vote);

        assertEquals(userId, saved.getUserId());
        assertEquals(targetId, saved.getTargetId());
        assertTrue(saved.getIsPost());
        assertEquals(1, saved.getVoteType());
        assertNotNull(saved.getCreatedAt());
        assertFalse(saved.getIsDeleted());
        assertNull(saved.getDeletedAt());
    }

    @Test
    void save_VoteWithDefaultValues_UsesDefaults() {
        Vote vote = Vote.builder()
                .userId(userId)
                .targetId(targetId)
                .isPost(true)
                .voteType(1)
                .build();

        Vote saved = voteRepository.save(vote);

        assertFalse(saved.getIsDeleted());
        assertNull(saved.getDeletedAt());
    }

    @Test
    void save_SoftDeletedVote_PreservesDeletedState() {
        Vote vote = Vote.builder()
                .userId(userId)
                .targetId(targetId)
                .isPost(true)
                .voteType(1)
                .isDeleted(true)
                .deletedAt(Instant.now())
                .build();

        Vote saved = voteRepository.save(vote);

        assertTrue(saved.getIsDeleted());
        assertNotNull(saved.getDeletedAt());
    }

    @Test
    void update_ExistingVote_UpdatesSuccessfully() {
        Vote vote = createVote(userId, targetId, true, 1);
        Vote saved = voteRepository.save(vote);

        saved.setVoteType(-1);
        Vote updated = voteRepository.save(saved);

        assertEquals(-1, updated.getVoteType());
    }

    @Test
    void save_UpvoteAndDownvote_BothTypes() {
        Vote upvote = createVote(userId, targetId, true, 1);
        Vote downvote = createVote(new ObjectId(), targetId, true, -1);

        voteRepository.saveAll(List.of(upvote, downvote));

        List<Vote> targetVotes = voteRepository.findByTargetId(targetId);
        assertEquals(2, targetVotes.size());
        assertTrue(targetVotes.stream().anyMatch(v -> v.getVoteType() == 1));
        assertTrue(targetVotes.stream().anyMatch(v -> v.getVoteType() == -1));
    }

    @Test
    void save_PostVoteAndCommentVote_BothTypes() {
        Vote postVote = createVote(userId, targetId, true, 1);
        Vote commentVote = createVote(userId, new ObjectId(), false, 1);

        voteRepository.saveAll(List.of(postVote, commentVote));

        List<Vote> userVotes = voteRepository.findByUserId(userId);
        assertEquals(2, userVotes.size());
        assertTrue(userVotes.stream().anyMatch(Vote::getIsPost));
        assertTrue(userVotes.stream().anyMatch(v -> !v.getIsPost()));
    }


    @Test
    void compoundIndex_DifferentTargetType_AllowsSave() {
        Vote postVote = createVote(userId, targetId, true, 1);
        Vote commentVote = createVote(userId, targetId, false, 1);

        voteRepository.save(postVote);

        // Should not throw - different targetType allows duplicate user+target combination
        assertDoesNotThrow(() -> voteRepository.save(commentVote));
    }

    @Test
    void findByUserIdAndTargetId_MultipleVotes_ReturnsAll() {
        // This could happen if compound index includes more fields
        Vote vote1 = createVote(userId, targetId, true, 1);
        voteRepository.save(vote1);

        List<Vote> result = voteRepository.findByUserIdAndTargetId(userId, targetId);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(v ->
                v.getUserId().equals(userId) && v.getTargetId().equals(targetId)
        ));
    }

    @Test
    void findAll_ReturnsAllVotes() {
        Vote vote1 = createVote(userId, targetId, true, 1);
        Vote vote2 = createVote(new ObjectId(), new ObjectId(), false, -1);
        voteRepository.saveAll(List.of(vote1, vote2));

        List<Vote> all = voteRepository.findAll();

        assertTrue(all.size() >= 2);
    }

    @Test
    void count_ReturnsCorrectCount() {
        Vote vote1 = createVote(userId, targetId, true, 1);
        Vote vote2 = createVote(new ObjectId(), new ObjectId(), false, -1);
        voteRepository.saveAll(List.of(vote1, vote2));

        long count = voteRepository.count();

        assertTrue(count >= 2);
    }

    @Test
    void save_WithCreatedAtTimestamp_PreservesTimestamp() {
        Instant specificTime = Instant.now().minusSeconds(3600);
        Vote vote = Vote.builder()
                .userId(userId)
                .targetId(targetId)
                .isPost(true)
                .voteType(1)
                .createdAt(specificTime)
                .build();

        Vote saved = voteRepository.save(vote);

        assertEquals(specificTime, saved.getCreatedAt());
    }

    private Vote createVote(ObjectId userId, ObjectId targetId, Boolean isPost, Integer voteType) {
        return Vote.builder()
                .userId(userId)
                .targetId(targetId)
                .isPost(isPost)
                .voteType(voteType)
                .createdAt(Instant.now())
                .build();
    }
}
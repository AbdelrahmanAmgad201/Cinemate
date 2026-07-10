package org.example.backend.vote;

import org.bson.types.ObjectId;
import org.example.backend.AbstractMongoIntegrationTest;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REL-01: vote counts are updated via MongoDB's atomic $inc rather than a
 * load-mutate-save round trip, specifically so concurrent votes on the same post
 * don't lose updates (two threads both reading count=5 and both writing count=6).
 * A mocked-MongoTemplate unit test can't prove that; this drives real concurrent
 * writes against a real Mongo instance.
 */
class VoteConcurrencyTest extends AbstractMongoIntegrationTest {

    private static final int CONCURRENT_VOTERS = 25;

    @Autowired
    private VoteService voteService;

    @Autowired
    private PostRepository postRepository;

    private ObjectId postId;

    @BeforeEach
    void setUp() {
        Post post = Post.builder()
                .id(new ObjectId())
                .forumId(new ObjectId())
                .ownerId(new ObjectId())
                .title("Concurrency target post")
                .content("content")
                .createdAt(Instant.now())
                .lastActivityAt(Instant.now())
                .isDeleted(false)
                .upvoteCount(0)
                .downvoteCount(0)
                .score(0)
                .build();
        postId = postRepository.save(post).getId();
    }

    @Test
    void concurrentUpvotes_FromDifferentUsers_AllCountsAreApplied() throws InterruptedException {
        List<Long> voterIds = IntStream.range(0, CONCURRENT_VOTERS)
                .mapToObj(i -> (long) (i + 1))
                .collect(Collectors.toList());

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_VOTERS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_VOTERS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENT_VOTERS);
        AtomicInteger failures = new AtomicInteger();

        for (Long voterId : voterIds) {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    VoteDTO dto = VoteDTO.builder().targetId(postId).value(1).build();
                    voteService.vote(dto, VoteTargetType.POST, voterId);
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();
        boolean completed = done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Concurrent votes did not finish in time");
        assertEquals(0, failures.get(), "Some concurrent votes threw unexpectedly");

        Post updated = postRepository.findById(postId).orElseThrow();
        assertEquals(CONCURRENT_VOTERS, updated.getUpvoteCount(),
                "Lost updates: atomic $inc should apply every concurrent vote exactly once");
        assertEquals(CONCURRENT_VOTERS, updated.getScore());
        assertEquals(0, updated.getDownvoteCount());
    }
}

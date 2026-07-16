package org.example.backend.feed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.forum.Forum;
import org.example.backend.forum.ForumPageResponse;
import org.example.backend.forum.ForumRepository;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Explore feed. The response cache is gone (consolidation removed the cache layer) — the
 * partial indexes on posts/forums (WHERE NOT is_deleted, ordered by score/created_at/…)
 * make these queries cheap directly. A materialized view is the documented next step if
 * profiling ever shows it's needed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

    private final PostRepository postRepository;
    private final ForumRepository forumRepository;

    private static final int EXPLORE_DAYS_LIMIT = 7;
    private static final int DEFAULT_PAGE_SIZE = 20;

    @Transactional(readOnly = true)
    public PostPageResponse getExploreFeed(int page, int size, String sortBy) {
        Instant cutoffDate = Instant.now().minus(EXPLORE_DAYS_LIMIT, ChronoUnit.DAYS);
        Pageable pageable = PageRequest.of(page, size, buildSort(sortBy));

        Page<Post> postsPage = postRepository.findByIsDeletedFalseAndCreatedAtGreaterThanEqual(cutoffDate, pageable);

        return PostPageResponse.builder()
                .posts(postsPage.getContent())
                .currentPage(postsPage.getNumber())
                .totalPages(postsPage.getTotalPages())
                .totalElements(postsPage.getTotalElements())
                .pageSize(postsPage.getSize())
                .hasNext(postsPage.hasNext())
                .hasPrevious(postsPage.hasPrevious())
                .build();
    }

    public PostPageResponse getExploreFeed(int page) {
        return getExploreFeed(page, DEFAULT_PAGE_SIZE, "score");
    }

    @Transactional(readOnly = true)
    public ForumPageResponse getExploreForums(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, buildSortForum(sortBy));
        Page<Forum> forumsPage = forumRepository.findAllByIsDeletedFalse(pageable);
        return ForumPageResponse.from(forumsPage);
    }

    public ForumPageResponse getExploreForums(int page) {
        return getExploreForums(page, DEFAULT_PAGE_SIZE, "followers");
    }

    private Sort buildSort(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "new" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "hot" -> Sort.by(Sort.Direction.DESC, "lastActivityAt");
            case "top", "score" -> Sort.by(Sort.Direction.DESC, "score");
            default -> Sort.by(Sort.Direction.DESC, "score");
        };
    }

    private Sort buildSortForum(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "new" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "followers" -> Sort.by(Sort.Direction.DESC, "followerCount");
            case "posts" -> Sort.by(Sort.Direction.DESC, "postCount");
            default -> Sort.by(Sort.Direction.DESC, "followerCount");
        };
    }
}

package org.example.backend.feed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

    private final PostRepository postRepository;

    // configuration
    private static final int EXPLORE_DAYS_LIMIT = 7;
    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Get explore feed - popular posts from last 7 days
     * Results are cached for 15 minutes (can be changed in CacheConfig)
     *
     * Cache Key: exploreFeed::{page}::{size}::{sortBy}
     * This means each page/size/sort combination is cached separately
     */
    @Cacheable(
            value = "exploreFeed",
            key = "#page + '::' + #size + '::' + #sortBy",
            unless = "#result == null || #result.posts.isEmpty()"
    )
    public Page<Post> getExploreFeed(int page, int size, String sortBy) {
        log.info("Cache MISS - Fetching explore feed from DB: page={}, size={}, sortBy={}",
                page, size, sortBy);


        Instant cutoffDate = Instant.now().minus(EXPLORE_DAYS_LIMIT, ChronoUnit.DAYS);
        Sort sort = buildSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return postRepository.findByIsDeletedFalseAndCreatedAtGreaterThanEqual(
                cutoffDate,
                pageable
        );
        /*
        return PostPageResponse.builder()
                .posts(postsPage.getContent())
                .currentPage(postsPage.getNumber())
                .totalPages(postsPage.getTotalPages())
                .totalElements(postsPage.getTotalElements())
                .pageSize(postsPage.getSize())
                .hasNext(postsPage.hasNext())
                .hasPrevious(postsPage.hasPrevious())
                .build();*/
    }

    /**
     * Get explore feed with defaults
     */
    public Page<Post> getExploreFeed(int page) {
        return getExploreFeed(page, DEFAULT_PAGE_SIZE, "score");
    }

    /**
     * Build sort based on sortBy parameter
     */
    private Sort buildSort(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "new" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "hot" -> Sort.by(Sort.Direction.DESC, "lastActivityAt");
            case "top", "score" -> Sort.by(Sort.Direction.DESC, "score");
            default -> Sort.by(Sort.Direction.DESC, "score");  // Default to score
        };
    }
}
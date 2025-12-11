package org.example.backend.feed;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /**
     * Get explore feed - popular posts for all users
     *
     * Query parameters:
     * - page: page number (default: 0)
     * - size: page size (default: 20)
     * - sort: sorting method - "top"/"score", "new", "hot" (default: "score")
     *
     * Examples:
     * GET /api/feed/explore
     * GET /api/feed/explore?page=0&size=10
     * GET /api/feed/explore?page=1&size=20&sort=new
     * GET /api/feed/explore?sort=hot
     */
    @GetMapping("/explore")
    public ResponseEntity<PostPageResponse> explore(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "score") String sort) {

        // Validate page and size
        if (page < 0) {
            return ResponseEntity.badRequest().build();
        }
        if (size < 1 || size > 100) {
            size = 20; // Default to 20 if invalid
        }

        PostPageResponse response = feedService.getExploreFeed(page, size, sort);
        return ResponseEntity.ok(response);
    }
}

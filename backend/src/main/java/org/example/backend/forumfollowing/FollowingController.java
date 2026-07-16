package org.example.backend.forumfollowing;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import org.example.backend.forum.ForumPageResponse;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/forum-follow")
@RequiredArgsConstructor
public class FollowingController {
    public final FollowingService followingService;

    @PutMapping("/v1/{forumId}")
    public ResponseEntity<?> followForum(
            HttpServletRequest request,
            @PathVariable UUID forumId) {

        Long userId = (Long) request.getAttribute("userId");
        followingService.follow(forumId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/v1/{forumId}")
    public ResponseEntity<?> unfollowForum(
            HttpServletRequest request,
            @PathVariable UUID forumId) {

        Long userId = (Long) request.getAttribute("userId");
        followingService.unfollow(forumId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/v1/{forumId}")
    public ResponseEntity<Boolean> isFollowed(
            HttpServletRequest request,
            @PathVariable UUID forumId) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(followingService.isFollowed(forumId, userId));
    }

    /**
     * Get followed forums with pagination
     * Query params:
     * - page: page number (0-indexed), default: 0
     * - size: items per page, default: 20
     * - sort: sort field and direction, default: createdAt,desc
     * Example: GET /api/forum-follow/v1/followed?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping("/v1/followed")
    public ResponseEntity<ForumPageResponse> getFollowedForums(
            HttpServletRequest request,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        Long userId = (Long) request.getAttribute("userId");
        ForumPageResponse response = followingService.getFollowedForums(userId, pageable);
        return ResponseEntity.ok(response);
    }
}

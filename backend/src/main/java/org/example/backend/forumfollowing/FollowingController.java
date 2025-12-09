package org.example.backend.forumfollowing;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.example.backend.forum.ForumCreationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum-follow")
@RequiredArgsConstructor
public class FollowingController {
    public final FollowingService followingService;

    @PutMapping("/v1/follow/{forumId}")
    public ResponseEntity<?> followForum(
            HttpServletRequest request,
            @PathVariable ObjectId forumId) {

        Long userId = (Long) request.getAttribute("userId");
        followingService.follow(forumId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/v1/follow/{forumId}")
    public ResponseEntity<?> unfollowForum(
            HttpServletRequest request,
            @PathVariable ObjectId forumId) {

        Long userId = (Long) request.getAttribute("userId");
        followingService.unfollow(forumId, userId);
        return ResponseEntity.noContent().build();
    }
}

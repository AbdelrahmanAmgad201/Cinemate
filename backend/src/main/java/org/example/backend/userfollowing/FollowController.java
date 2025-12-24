package org.example.backend.userfollowing;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follow")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/v1/follow/{followedUserId}")
    public ResponseEntity<?> follow(
            HttpServletRequest request,
            @PathVariable Long followedUserId
    ){
        Long followingUserId = (Long) request.getAttribute("userId");
        followService.follow(followingUserId,followedUserId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/v1/unfollow/{followedUserId}")
    public ResponseEntity<?> unFollow(
            HttpServletRequest request,
            @PathVariable Long followedUserId
    ){
        Long followingUserId = (Long) request.getAttribute("userId");
        followService.unfollow(followingUserId,followedUserId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/v1/is-followed/{followedUserId}")
    public ResponseEntity<?> isFollowed(
            HttpServletRequest request,
            @PathVariable Long followedUserId
    ){
        Long followingUserId = (Long) request.getAttribute("userId");
        return  ResponseEntity.ok(followService.isFollowed(followingUserId,followedUserId));
    }
}

package org.example.backend.userfollowing;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping("/v1/followers")
    public ResponseEntity<Page<FollowerView>> getFollowers(
            HttpServletRequest request,
            @PageableDefault(size = 20) Pageable pageable
    ){
        Long followedUserId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(followService.getUserFollowers(followedUserId,pageable));
    }

    @GetMapping("/v1/followings")
    public ResponseEntity<Page<FollowingView>> getFollowings(
            HttpServletRequest request,
            @PageableDefault(size = 20) Pageable pageable
    ){
        Long followingUserId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(followService.getUserFollowings(followingUserId,pageable));
    }
}

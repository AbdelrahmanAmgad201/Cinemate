package org.example.backend.post;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/post")
public class PostController {
    @Autowired
    private PostService postService;

    @PostMapping("/v1")
    public ResponseEntity<String> addPost(HttpServletRequest request, @Valid @RequestBody AddPostDTO addPostDto) {
        Long userId = (Long) request.getAttribute("userId");
        Post post = postService.addPost(addPostDto, userId);
        UUID postId = post.getId();
        return ResponseEntity.ok(postId.toString());
    }

    @PutMapping("/v1/{postId}")
    public ResponseEntity<String> updatePost(HttpServletRequest request, @PathVariable UUID postId,
            @Valid @RequestBody AddPostDTO addPostDto) {
        Long userId = (Long) request.getAttribute("userId");
        postService.updatePost(postId, addPostDto, userId);
        return ResponseEntity.ok(postId.toString());
    }

    @DeleteMapping("/v1/{postId}")
    public ResponseEntity<String> deletePost(HttpServletRequest request, @PathVariable UUID postId) {
        Long userId = (Long) request.getAttribute("userId");
        postService.deletePost(postId, userId);
        return ResponseEntity.ok("deleted");
    }

    @PostMapping("/v1/main-feed")
    public ResponseEntity<Page<PostView>> getUserFeed(
            HttpServletRequest request,
            @Valid @RequestBody MainFeedRequestDTO mainFeedRequestDTO) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(postService.getUserPosts(userId, mainFeedRequestDTO));
    }

    @PostMapping("/v1/forum-posts")
    public ResponseEntity<Page<PostView>> getForumPosts(HttpServletRequest request,
            @Valid @RequestBody ForumPostsRequestDTO forumPostsRequestDTO) {
        return ResponseEntity.ok(postService.getForumPosts(forumPostsRequestDTO));
    }

    @GetMapping("/v1/{postId}")
    public ResponseEntity<PostView> getPost(HttpServletRequest request, @PathVariable UUID postId) {
        return ResponseEntity.ok(postService.getPostById(postId));
    }

    @GetMapping("/v1/my-posts")
    public  ResponseEntity<Page<PostView>> getMyPosts(HttpServletRequest request,
                                                      @PageableDefault(size = 20) Pageable pageable){
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(postService.getMyPosts(userId, pageable));
    }

    @GetMapping("/v1/user/{userId}")
    public  ResponseEntity<Page<PostView>> getOtherUserPosts(@PathVariable Long userId,
                                                      @PageableDefault(size = 20) Pageable pageable){
        return ResponseEntity.ok(postService.getOtherUserPosts(userId, pageable));
    }
}

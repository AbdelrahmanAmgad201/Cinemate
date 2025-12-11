package org.example.backend.post;

import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/post")
public class PostController {
    @Autowired
    private PostService postService;

    @PostMapping("/v1/post")
    public ResponseEntity<String> addPost(HttpServletRequest request, @RequestBody AddPostDto addPostDto) {
        Long userId = (Long) request.getAttribute("userId");
        Post post = postService.addPost(addPostDto, userId);
        ObjectId postId = post.getId();
        return ResponseEntity.ok(postId.toHexString());
    }
    @PutMapping("/v1/post/{postId}")
    public ResponseEntity<String> updatePost(HttpServletRequest request, @PathVariable ObjectId postId, @RequestBody AddPostDto addPostDto) {
        Long userId = (Long) request.getAttribute("userId");
        postService.updatePost(postId, addPostDto, userId);
        return ResponseEntity.ok(postId.toHexString());
    }
    @DeleteMapping("/v1/post/{postId}")
    public ResponseEntity<String> deletePost(HttpServletRequest request, @PathVariable ObjectId postId) {
        Long userId = (Long) request.getAttribute("userId");
        postService.deletePost(postId, userId);
        return ResponseEntity.ok("deleted");
    }
    @GetMapping("/v1/user-main-feed")
    public ResponseEntity<Page<Post>> getUserFeed(
            HttpServletRequest request,
            @RequestBody MainFeedRequestDTO mainFeedRequestDTO) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(postService.getUserPosts(userId, mainFeedRequestDTO));
    }
}

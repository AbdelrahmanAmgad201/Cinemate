package org.example.backend.post;

import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/post")
public class PostController {
    @Autowired
    private PostService postService;

    @PostMapping("/v1/post")
    public ResponseEntity<String> addPost(HttpServletRequest request, @RequestBody AddPostDto addPostDto) {
        Long userId = (Long) request.getAttribute("userId");
        Post post = postService.addPost(addPostDto, userId);
        String postId = post.getId().toHexString();
        return ResponseEntity.ok(postId);
    }
    @PutMapping("/v1/post")
    public ResponseEntity<String> updatePost(HttpServletRequest request, @PathVariable String postId, @RequestBody AddPostDto addPostDto) {
        Long userId = (Long) request.getAttribute("userId");
        postService.updatePost(postId, addPostDto, userId);
        return ResponseEntity.ok(postId);
    }
    @DeleteMapping("/v1/post")
    public ResponseEntity<?> deletePost(HttpServletRequest request, @PathVariable String postId) {
        Long userId = (Long) request.getAttribute("userId");
        postService.deletePost(postId, userId);
        return ResponseEntity.ok().build();
    }
}

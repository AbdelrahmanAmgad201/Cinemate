package org.example.backend.post;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/post")
public class PostController {
    @Autowired
    private PostService postService;

    @PostMapping("/v1/post")
    public ResponseEntity<?> addPost(HttpServletRequest request, @RequestBody AddPostDto addPostDto) {
        Long userId = (Long) request.getAttribute("userId");
        postService.addPost(addPostDto, userId);
        return ResponseEntity.ok().build();
    }

}

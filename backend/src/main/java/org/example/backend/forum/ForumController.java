package org.example.backend.forum;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum")
public class ForumController {
    @Autowired
    private ForumService forumService;

    @PostMapping("/v1/create")
    public ResponseEntity<Forum> searchMoviesPost(
            HttpServletRequest request,
            @Valid @RequestBody ForumCreationRequest requestDTO) {

        Long userId = (Long) request.getAttribute("userId");
        Forum forum = forumService.createForum(requestDTO, userId);
        return ResponseEntity.ok(forum);
    }

}

package org.example.backend.forum;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum")
public class ForumController {
    @Autowired
    private ForumService forumService;

    @PostMapping("/v1/create")
    public ResponseEntity<Forum> createForum(
            HttpServletRequest request,
            @Valid @RequestBody ForumCreationRequest requestDTO) {

        Long userId = (Long) request.getAttribute("userId");
        Forum forum = forumService.createForum(requestDTO, userId);
        return ResponseEntity.ok(forum);
    }

    @DeleteMapping("v1/delete/{forumId}")
    public ResponseEntity<?> deleteForum(
            HttpServletRequest request,
            @PathVariable ObjectId forumId) {

        Long userId = (Long) request.getAttribute("userId");
        forumService.deleteForum(forumId,userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("v1/update/{forumId}")
    public ResponseEntity<?> deleteForum(
            HttpServletRequest request,
            @PathVariable ObjectId forumId,
            @Valid @RequestBody ForumCreationRequest requestDTO) {

        Long userId = (Long) request.getAttribute("userId");
        forumService.updateForum(forumId, requestDTO, userId);
        return ResponseEntity.ok().build();
    }

}

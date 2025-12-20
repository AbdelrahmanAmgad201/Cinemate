package org.example.backend.forum;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

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

    @GetMapping("/v1/get-forum-by-id/{forumId}")
    public ResponseEntity<Forum> getForumById(
            HttpServletRequest request,
            @PathVariable ObjectId forumId){

        return ResponseEntity.ok(forumService.getForumById(forumId));
    }


    @DeleteMapping("/v1/delete/{forumId}")
    public ResponseEntity<?> deleteForum(
            HttpServletRequest request,
            @PathVariable ObjectId forumId) {

        Long userId = (Long) request.getAttribute("userId");
        forumService.deleteForum(forumId,userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/v1/update/{forumId}")
    public ResponseEntity<?> updateForum(
            HttpServletRequest request,
            @PathVariable ObjectId forumId,
            @Valid @RequestBody ForumCreationRequest requestDTO) {

        Long userId = (Long) request.getAttribute("userId");
        forumService.updateForum(forumId, requestDTO, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Search forums by name
     *
     * Query parameters:
     * - q: search query (required)
     * - page: page number (default: 0)
     * - size: page size (default: 20)
     *
     * Example: GET /api/forum/v1/search?q=javascript&page=0&size=10
     */

    @GetMapping("/v1/search")
    public ResponseEntity<SearchResultDto> searchForums(
            @RequestParam("q") String searchQuery,
            @PageableDefault(size = 20) Pageable pageable) {

        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        SearchResultDto results = forumService.searchForums(searchQuery.trim(), pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/v1/forum-name/{forumId}")
    public ResponseEntity<String> getForumByName(
            HttpServletRequest request,
            @PathVariable ObjectId forumId
    ) {
        return ResponseEntity.ok(forumService.getForumName(forumId));
        }

    @GetMapping("/v1/user-forums")
    public ResponseEntity<Page<ForumDisplayDTO>> getForumsByUser(
            HttpServletRequest request,
            @PageableDefault(size = 20) Pageable pageable){
            Long userId = (Long) request.getAttribute("userId");
            return ResponseEntity.ok(forumService.findUserForums(userId,pageable));
    }
}

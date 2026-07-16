package org.example.backend.comment;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comment")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @PostMapping("/v1")
    public ResponseEntity<String> createComment(HttpServletRequest request, @Valid @RequestBody AddCommentDTO addCommentDTO) {
        Long userId = (Long) request.getAttribute("userId");
        Comment comment=commentService.addComment(userId,addCommentDTO);
        return ResponseEntity.ok(comment.getId().toString());
    }
    @DeleteMapping("/v1/{commentId}")
    public ResponseEntity<String> deleteComment(HttpServletRequest request, @PathVariable UUID commentId) {
        Long userId = (Long) request.getAttribute("userId");
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok("Comment deleted successfully");
    }
    @GetMapping("/v1/post/{postId}")
    public ResponseEntity<Page<CommentView>> getAllComments(HttpServletRequest request,
                                                        @PathVariable UUID postId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        @RequestParam(defaultValue = "score") String sortBy) {
        return ResponseEntity.ok(commentService.getPostComments(postId, page, size, sortBy));
    }
    @GetMapping("/v1/{parentId}/replies")
    public ResponseEntity<List<CommentView>> getAllReplies(HttpServletRequest request,
                                                       @PathVariable UUID parentId,
                                                       @RequestParam(defaultValue = "score") String sortBy) {
        return ResponseEntity.ok(commentService.getReplies(parentId,sortBy));
    }
}

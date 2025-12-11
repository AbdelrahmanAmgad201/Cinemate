package org.example.backend.comment;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.coyote.Response;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comment")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @PostMapping("/v1/create-comment")
    public ResponseEntity<String> createComment(HttpServletRequest request, @RequestBody AddCommentDTO addCommentDTO) {
        Long userId = (Long) request.getAttribute("userId");
        Comment comment=commentService.addComment(userId,addCommentDTO);
        return ResponseEntity.ok(comment.getId().toHexString());
    }
    @DeleteMapping("/v1/delete-comment/{commentId}")
    public ResponseEntity<String> deleteComment(HttpServletRequest request, @PathVariable ObjectId commentId) {
        Long userId = (Long) request.getAttribute("userId");
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok("Comment deleted successfully");
    }
}

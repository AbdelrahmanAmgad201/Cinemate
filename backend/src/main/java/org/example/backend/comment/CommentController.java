package org.example.backend.comment;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.coyote.Response;
import org.bson.types.ObjectId;
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
    @GetMapping("/v1/posts/{postId}/comments")
    public ResponseEntity<Page<Comment>> getAllComments(HttpServletRequest request,
                                                        @PathVariable ObjectId postId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        @RequestParam(defaultValue = "score") String sortBy) {
        return ResponseEntity.ok(commentService.getPostComments(postId, page, size, sortBy));
    }
    @GetMapping("/v1/replies/{parentId}")
    public ResponseEntity<List<Comment>> getAllReplies(HttpServletRequest request,
                                                       @PathVariable ObjectId parentId,
                                                       @RequestParam(defaultValue = "score") String sortBy) {
        return ResponseEntity.ok(commentService.getReplies(parentId,sortBy));
    }
}

package org.example.backend.comment;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

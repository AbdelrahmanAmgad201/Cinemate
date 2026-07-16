package org.example.backend.vote;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vote")
public class VoteController {
    @Autowired
    private VoteService voteService;

    @PostMapping("/v1/post")
    public ResponseEntity<?> postVote(
            HttpServletRequest request,
            @Valid @RequestBody VoteDTO voteDTO) {

        Long userId = (Long) request.getAttribute("userId");
        voteService.vote(voteDTO,VoteTargetType.POST,userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/v1/comment")
    public ResponseEntity<?> commentVote(
            HttpServletRequest request,
            @Valid @RequestBody VoteDTO voteDTO) {

        Long userId = (Long) request.getAttribute("userId");
        voteService.vote(voteDTO,VoteTargetType.COMMENT,userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/v1")
    public ResponseEntity<?> updateVote(
            HttpServletRequest request,
            @Valid @RequestBody UpdateVoteDTO updateVoteDTO) {

        Long userId = (Long) request.getAttribute("userId");
        voteService.updateVote(updateVoteDTO,userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/v1/{targetId}")
    public ResponseEntity<?> deleteVote(
            HttpServletRequest request,
            @PathVariable UUID targetId
    ){
        Long userId = (Long) request.getAttribute("userId");
        voteService.deleteVote(targetId,userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/v1/{targetId}")
    public ResponseEntity<Integer> getIsVoted(
            HttpServletRequest request,
            @PathVariable UUID targetId
    ){
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(voteService.isVote(targetId,userId));
    }

}

package org.example.backend.vote;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vote")
public class VoteController {
    @Autowired
    private VoteService voteService;

    @PostMapping("/v1/post-vote")
    public ResponseEntity<?> postVote(
            HttpServletRequest request,
            @Valid @RequestBody VoteDTO voteDTO) {

        Long userId = (Long) request.getAttribute("userId");
        voteService.vote(voteDTO,true,userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/v1/comment-vote")
    public ResponseEntity<?> commentVote(
            HttpServletRequest request,
            @Valid @RequestBody VoteDTO voteDTO) {

        Long userId = (Long) request.getAttribute("userId");
        voteService.vote(voteDTO,false,userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/v1/update-vote")
    public ResponseEntity<?> updateVote(
            HttpServletRequest request,
            @Valid @RequestBody UpdateVoteDTO updateVoteDTO) {

        Long userId = (Long) request.getAttribute("userId");
        voteService.updateVote(updateVoteDTO,userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/v1/delete-vote/{targetId}")
    public ResponseEntity<?> deleteVote(
            HttpServletRequest request,
            @PathVariable ObjectId targetId
    ){
        Long userId = (Long) request.getAttribute("userId");
        voteService.deleteVote(targetId,userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/v1/is-voted/{targetId}")
    public ResponseEntity<Integer> getIsVoted(
            HttpServletRequest request,
            @PathVariable ObjectId targetId
    ){
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(voteService.isVote(targetId,userId));
    }

}

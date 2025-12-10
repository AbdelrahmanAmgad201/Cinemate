package org.example.backend.vote;

import jakarta.servlet.http.HttpServletRequest;
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
            @RequestBody VoteDTO voteDTO) {

        Long userId = (Long) request.getAttribute("userId");
        voteService.vote(voteDTO,true,userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/v1/comment-vote")
    public ResponseEntity<?> commentVote(
            HttpServletRequest request,
            @RequestBody VoteDTO voteDTO) {

        Long userId = (Long) request.getAttribute("userId");
        voteService.vote(voteDTO,false,userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/v1/update-vote")
    public ResponseEntity<?> updateVote(
            HttpServletRequest request,
            @RequestBody UpdateVoteDTO updateVoteDTO) {

        Long userId = (Long) request.getAttribute("userId");
        voteService.updateVote(updateVoteDTO,userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/v1/delete-vote/{voteId}")
    public ResponseEntity<?> deleteVote(
            HttpServletRequest request,
            @PathVariable ObjectId voteId
    ){
        Long userId = (Long) request.getAttribute("userId");
        voteService.deleteVote(voteId,userId);
        return ResponseEntity.ok().build();
    }

}

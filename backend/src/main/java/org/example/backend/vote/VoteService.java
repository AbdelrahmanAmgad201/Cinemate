package org.example.backend.vote;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.example.backend.comment.Comment;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.example.backend.post.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VoteService {
    @Autowired
    private VoteRepository voteRepository;

    private final MongoTemplate mongoTemplate;
    private final CascadeDeletionService deletionService;
    private final AccessService accessService;

    @Transactional
    public void vote(VoteDTO voteDTO,Boolean isPost,Long userId) {
        ObjectId targetId = new ObjectId(voteDTO.getTargetId());
        ObjectId ownerId = longToObjectId(userId);
        canVote(targetId,isPost);
        Vote vote = Vote.builder()
                .targetId(targetId)
                .userId(ownerId)
                .voteType(voteDTO.getValue())
                .isPost(isPost)
                .build();
        voteRepository.save(vote);
    }

    @Transactional
    public void updateVote(UpdateVoteDTO updateVoteDTO,Long userId) {
        ObjectId voteId = new ObjectId(updateVoteDTO.getId());
        Vote vote = mongoTemplate.findById(voteId, Vote.class);
        if(vote == null){
            throw new IllegalArgumentException("Vote not found with id: " + voteId);
        }
        if (!vote.getUserId().equals(longToObjectId(userId))) {
            throw new AccessDeniedException("User does not have permission to update this forum");
        }
        vote.setVoteType(updateVoteDTO.getValue());
        voteRepository.save(vote);
    }

    @Transactional
    public void deleteVote(String id,Long userId){
        ObjectId voteId = new ObjectId(id);
        if (!accessService.canDeleteVote(longToObjectId(userId), voteId)) {
            throw new AccessDeniedException("User " + " cannot delete this forum");
        }

        deletionService.deleteVote(voteId);
    }

    private void canVote(ObjectId targetId,Boolean isPost){
        if(isPost)  canVotePost(targetId);
        else canVoteComment(targetId);
    }

    private void canVotePost(ObjectId postId){

        Post post = mongoTemplate.findById(postId, Post.class);

        if (post == null) {
            throw new IllegalArgumentException("Post not found with id: " + postId);
        }

        if (post.getIsDeleted()) {
            throw new IllegalStateException("Cannot vote a deleted post");
        }
    }

    private void canVoteComment(ObjectId commentId){
        Comment comment = mongoTemplate.findById(commentId, Comment.class);

        if (comment == null) {
            throw new IllegalArgumentException("Post not found with id: " + commentId);
        }

        if (comment.getIsDeleted()) {
            throw new IllegalStateException("Cannot vote a deleted comment");
        }
    }
    private ObjectId longToObjectId(Long value) {
        return new ObjectId(String.format("%024x", value));
    }
}
